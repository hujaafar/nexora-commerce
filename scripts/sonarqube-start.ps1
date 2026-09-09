# File purpose: Creates local secrets, starts SonarQube, changes the default
# password, creates the Nexora Commerce project, and generates a CI analysis token.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$qualityDirectory = Join-Path $projectRoot 'quality'
$composeFile = Join-Path $qualityDirectory 'compose.yml'
$exampleEnvironment = Join-Path $qualityDirectory '.env.example'
$runtimeEnvironment = Join-Path $qualityDirectory '.env'

function New-RandomHexSecret {
    param([int]$ByteCount = 32)

    # RandomNumberGenerator is suitable for passwords and tokens; Random is not.
    $bytes = New-Object byte[] $ByteCount
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
}

function Read-EnvironmentFile {
    param([string]$Path)

    $values = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        if ($_ -match '^(?<name>[^#=]+)=(?<value>.*)$') {
            $values[$Matches.name.Trim()] = $Matches.value.Trim()
        }
    }
    return $values
}

function Set-EnvironmentValue {
    param([string]$Path, [string]$Name, [string]$Value)

    $content = [System.IO.File]::ReadAllText($Path)
    $updated = [regex]::Replace(
        $content,
        "(?m)^$([regex]::Escape($Name))=.*$",
        "$Name=$Value"
    )
    [System.IO.File]::WriteAllText(
        $Path,
        $updated,
        [System.Text.UTF8Encoding]::new($false)
    )
}

function New-BasicAuthorization {
    param([string]$Username, [string]$Password)

    $bytes = [System.Text.Encoding]::UTF8.GetBytes("${Username}:${Password}")
    return "Basic $([Convert]::ToBase64String($bytes))"
}

function Invoke-SonarApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Authorization,
        [hashtable]$Body = @{}
    )

    $arguments = @{
        Method = $Method
        Uri = "$script:sonarHost$Path"
        Headers = @{ Authorization = $Authorization }
        ErrorAction = 'Stop'
    }
    if ($Body.Count -gt 0) {
        $arguments.Body = $Body
        $arguments.ContentType = 'application/x-www-form-urlencoded'
    }
    return Invoke-RestMethod @arguments
}

docker version | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not available. Start Docker Desktop and try again.'
}

$createdEnvironment = $false
if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    Copy-Item -LiteralPath $exampleEnvironment -Destination $runtimeEnvironment
    Set-EnvironmentValue $runtimeEnvironment 'SONAR_DB_PASSWORD' (New-RandomHexSecret 32)
    Set-EnvironmentValue $runtimeEnvironment 'SONAR_SYSTEM_PASSCODE' (New-RandomHexSecret 32)
    # The fixed prefix guarantees every character class required by SonarQube;
    # the random suffix supplies the entropy.
    Set-EnvironmentValue $runtimeEnvironment 'SONAR_ADMIN_PASSWORD' ("Sz9!$(New-RandomHexSecret 24)")
    # Empty means the API still needs to create the first analysis token.
    Set-EnvironmentValue $runtimeEnvironment 'SONAR_TOKEN' ''
    $createdEnvironment = $true
}

$settings = Read-EnvironmentFile $runtimeEnvironment
# Upgrade an environment created by an older script whose password did not
# satisfy SonarQube's mixed-character policy. This does not expose the value.
if (
    $settings.SONAR_ADMIN_PASSWORD -notmatch '[A-Z]' -or
    $settings.SONAR_ADMIN_PASSWORD -notmatch '[a-z]' -or
    $settings.SONAR_ADMIN_PASSWORD -notmatch '[0-9]' -or
    $settings.SONAR_ADMIN_PASSWORD -notmatch '[^A-Za-z0-9]'
) {
    Set-EnvironmentValue $runtimeEnvironment 'SONAR_ADMIN_PASSWORD' ("Sz9!$(New-RandomHexSecret 24)")
    $settings = Read-EnvironmentFile $runtimeEnvironment
}
$script:sonarHost = $settings.SONAR_HOST_URL.TrimEnd('/')

docker compose `
    --project-directory $qualityDirectory `
    --env-file $runtimeEnvironment `
    -f $composeFile `
    up --detach --wait --wait-timeout 900
if ($LASTEXITCODE -ne 0) {
    throw 'SonarQube did not become healthy. Inspect: docker compose -f quality/compose.yml logs'
}

# A fresh server accepts admin/admin exactly once. Existing volumes use the
# random password already stored in the ignored environment file.
$adminAuthorization = New-BasicAuthorization 'admin' $settings.SONAR_ADMIN_PASSWORD
try {
    $adminValidation = Invoke-SonarApi 'GET' '/api/authentication/validate' $adminAuthorization
    if ($adminValidation.valid -ne $true) {
        throw 'The generated administrator password is not active yet.'
    }
} catch {
    $defaultAuthorization = New-BasicAuthorization 'admin' 'admin'
    Invoke-SonarApi 'POST' '/api/users/change_password' $defaultAuthorization @{
        login = 'admin'
        previousPassword = 'admin'
        password = $settings.SONAR_ADMIN_PASSWORD
    } | Out-Null
    $adminAuthorization = New-BasicAuthorization 'admin' $settings.SONAR_ADMIN_PASSWORD
}

# Project creation is idempotent: an existing project is the desired state.
try {
    Invoke-SonarApi 'POST' '/api/projects/create' $adminAuthorization @{
        project = 'nexora-commerce'
        name = 'Nexora Commerce E-commerce Platform'
    } | Out-Null
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw }
}

# Force sign-in globally and make this project's results/source private. The
# generated CI token can still submit analysis, but anonymous visitors cannot
# browse findings or source code. Reapplying both settings is idempotent.
Invoke-SonarApi 'POST' '/api/settings/set' $adminAuthorization @{
    key = 'sonar.forceAuthentication'
    value = 'true'
} | Out-Null
Invoke-SonarApi 'POST' '/api/projects/update_visibility' $adminAuthorization @{
    project = 'nexora-commerce'
    visibility = 'private'
} | Out-Null

$tokenIsValid = $false
if (-not [string]::IsNullOrWhiteSpace($settings.SONAR_TOKEN)) {
    try {
        $tokenAuthorization = New-BasicAuthorization $settings.SONAR_TOKEN ''
        $validation = Invoke-SonarApi 'GET' '/api/authentication/validate' $tokenAuthorization
        $tokenIsValid = $validation.valid -eq $true
    } catch {
        $tokenIsValid = $false
    }
}

if (-not $tokenIsValid) {
    # Revoke only the named automation token, then create a fresh replacement.
    try {
        Invoke-SonarApi 'POST' '/api/user_tokens/revoke' $adminAuthorization @{
            name = 'nexora-commerce-ci'
        } | Out-Null
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 400) { throw }
    }
    $generated = Invoke-SonarApi 'POST' '/api/user_tokens/generate' $adminAuthorization @{
        name = 'nexora-commerce-ci'
        type = 'GLOBAL_ANALYSIS_TOKEN'
    }
    Set-EnvironmentValue $runtimeEnvironment 'SONAR_TOKEN' $generated.token
}

Write-Host ''
Write-Host 'Nexora Commerce SonarQube is ready.' -ForegroundColor Green
Write-Host "Dashboard: $script:sonarHost"
Write-Host 'User:      admin'
if ($createdEnvironment) {
    Write-Host 'A random password and CI token were generated in ignored quality/.env.' -ForegroundColor Yellow
} else {
    Write-Host 'Password and token remain in ignored quality/.env.'
}
Write-Host 'No secret was committed or printed.' -ForegroundColor Green
