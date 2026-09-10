# File purpose: Generates local Jenkins secrets once, then builds and starts the complete CI/CD stack.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$jenkinsDirectory = Join-Path $projectRoot 'jenkins'
$composeFile = Join-Path $jenkinsDirectory 'compose.yml'
$exampleEnvironment = Join-Path $jenkinsDirectory '.env.example'
$runtimeEnvironment = Join-Path $jenkinsDirectory '.env'
$qualityEnvironment = Join-Path $projectRoot 'quality\.env'

function New-RandomHexSecret {
    param([int]$ByteCount = 32)

    $bytes = New-Object byte[] $ByteCount
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }

    return ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
}

function Set-EnvironmentValue {
    param(
        [string]$Content,
        [string]$Name,
        [string]$Value
    )

    return [regex]::Replace(
        $Content,
        "(?m)^$([regex]::Escape($Name))=.*$",
        "$Name=$Value"
    )
}

function Get-EnvironmentValue {
    param(
        [string]$Content,
        [string]$Name
    )

    $match = [regex]::Match(
        $Content,
        "(?m)^$([regex]::Escape($Name))=(.*)$"
    )
    if ($match.Success) {
        return $match.Groups[1].Value
    }

    return ''
}

function Add-EnvironmentValueIfMissing {
    param(
        [string]$Content,
        [string]$Name,
        [string]$Value
    )

    if ($Content -match "(?m)^$([regex]::Escape($Name))=") {
        return $Content
    }

    return "$($Content.TrimEnd())`r`n$Name=$Value`r`n"
}

$createdEnvironment = $false
if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    $environmentContent = Get-Content -LiteralPath $exampleEnvironment -Raw
    $environmentContent = Set-EnvironmentValue $environmentContent 'JENKINS_ADMIN_PASSWORD' (New-RandomHexSecret 24)
    $environmentContent = Set-EnvironmentValue $environmentContent 'JENKINS_AGENT_PASSWORD' (New-RandomHexSecret 24)
    $environmentContent = Set-EnvironmentValue $environmentContent 'DEPLOY_JWT_SECRET' (New-RandomHexSecret 48)
    $environmentContent = Set-EnvironmentValue $environmentContent 'DEPLOY_INTERNAL_SERVICE_TOKEN' (New-RandomHexSecret 48)
    $environmentContent = Set-EnvironmentValue $environmentContent 'DEPLOY_MONGO_PASSWORD' (New-RandomHexSecret 24)
    $environmentContent = Set-EnvironmentValue $environmentContent 'DEPLOY_MINIO_PASSWORD' (New-RandomHexSecret 24)

    [System.IO.File]::WriteAllText(
        $runtimeEnvironment,
        $environmentContent,
        [System.Text.UTF8Encoding]::new($false)
    )
    $createdEnvironment = $true
}

# Existing installations may predate the dedicated agent and Mailpit. Add the
# new settings once without replacing any value the operator already chose.
$runtimeContent = Get-Content -LiteralPath $runtimeEnvironment -Raw
$updatedRuntimeContent = Add-EnvironmentValueIfMissing $runtimeContent 'JENKINS_AGENT_ID' 'agent-bootstrap'
$updatedRuntimeContent = Add-EnvironmentValueIfMissing $updatedRuntimeContent 'JENKINS_AGENT_PASSWORD' (New-RandomHexSecret 24)
$updatedRuntimeContent = Add-EnvironmentValueIfMissing $updatedRuntimeContent 'MAILPIT_HTTP_PORT' '8025'
$updatedRuntimeContent = Add-EnvironmentValueIfMissing $updatedRuntimeContent 'NOTIFICATION_EMAIL' 'builds@example.com'
if ($updatedRuntimeContent -ne $runtimeContent) {
    [System.IO.File]::WriteAllText(
        $runtimeEnvironment,
        $updatedRuntimeContent,
        [System.Text.UTF8Encoding]::new($false)
    )
}

# SonarQube generates the analysis token; Jenkins receives it through process
# environment substitution without copying or printing it in another file.
if (-not (Test-Path -LiteralPath $qualityEnvironment)) {
    throw 'Run scripts\sonarqube-start.ps1 before Jenkins so the quality token exists.'
}
$qualityContent = Get-Content -LiteralPath $qualityEnvironment -Raw
$sonarToken = Get-EnvironmentValue $qualityContent 'SONAR_TOKEN'
$sonarDockerUrl = Get-EnvironmentValue $qualityContent 'SONAR_DOCKER_HOST_URL'
if ([string]::IsNullOrWhiteSpace($sonarToken)) {
    throw 'SONAR_TOKEN is missing from ignored quality/.env.'
}
$env:SONAR_TOKEN = $sonarToken
$env:SONAR_HOST_URL = $sonarDockerUrl

# Nexus publication is part of the complete pipeline. Import only its limited
# publisher account, never its administrator password.
$nexusEnvironment = Join-Path $projectRoot 'nexus/.env'
if (-not (Test-Path -LiteralPath $nexusEnvironment)) {
    throw 'Run scripts/start-nexus.ps1 first so artifact publication is configured.'
}
$nexusContent = Get-Content -LiteralPath $nexusEnvironment -Raw
$env:NEXUS_PUBLISHER_USER = (Get-EnvironmentValue $nexusContent 'NEXUS_PUBLISHER_USER').Trim()
$env:NEXUS_PUBLISHER_PASSWORD = (Get-EnvironmentValue $nexusContent 'NEXUS_PUBLISHER_PASSWORD').Trim()

# The public GitHub repository can be cloned without credentials.

& (Join-Path $PSScriptRoot 'prepare-ci-trust.ps1')
docker version | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not available. Start Docker Desktop and try again.'
}

docker compose `
    --project-directory $jenkinsDirectory `
    --env-file $runtimeEnvironment `
    -f $composeFile `
    up --detach --build --wait --wait-timeout 600

if ($LASTEXITCODE -ne 0) {
    throw 'Jenkins did not start successfully. Run docker compose logs for details.'
}

$settings = @{}
Get-Content -LiteralPath $runtimeEnvironment | ForEach-Object {
    if ($_ -match '^(?<name>[^#=]+)=(?<value>.*)$') {
        $settings[$Matches.name] = $Matches.value
    }
}
$mailpitPort = if ($settings.MAILPIT_HTTP_PORT) {
    $settings.MAILPIT_HTTP_PORT
} else {
    '8025'
}

Write-Host ''
Write-Host 'Nexora Commerce is ready.' -ForegroundColor Green
Write-Host "Jenkins: $($settings.JENKINS_URL)"
Write-Host "User:    $($settings.JENKINS_ADMIN_ID)"
if ($createdEnvironment) {
    Write-Host 'Password: read JENKINS_ADMIN_PASSWORD from ignored jenkins/.env'
    Write-Host 'The generated password is stored only in ignored jenkins/.env.' -ForegroundColor Yellow
} else {
    Write-Host 'Password: read JENKINS_ADMIN_PASSWORD from ignored jenkins/.env'
}
Write-Host "Staging app after deployment:   http://localhost:$($settings.STAGING_FRONTEND_PORT)"
Write-Host "Production app after deployment: http://localhost:$($settings.PRODUCTION_FRONTEND_PORT)"
Write-Host "Notification inbox:              http://localhost:$mailpitPort"
