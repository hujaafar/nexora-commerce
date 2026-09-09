# Starts Nexus and idempotently provisions repositories, realms, roles, and users.
[CmdletBinding()]
param(
    # This switch records the operator's explicit acceptance of the Nexus CE EULA.
    [switch]$AcceptEula
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nexusDirectory = Join-Path $projectRoot 'nexus'
$exampleEnvironment = Join-Path $nexusDirectory '.env.example'
$runtimeEnvironment = Join-Path $nexusDirectory '.env'
$composeFile = Join-Path $nexusDirectory 'compose.yml'

function New-HexSecret {
    param([int]$Bytes = 24)

    $buffer = New-Object byte[] $Bytes
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($buffer)
    } finally {
        $generator.Dispose()
    }
    return ($buffer | ForEach-Object { $_.ToString('x2') }) -join ''
}

function Set-EnvironmentValue {
    param([string]$Content, [string]$Name, [string]$Value)

    return [regex]::Replace(
        $Content,
        "(?m)^$([regex]::Escape($Name))=.*$",
        "$Name=$Value"
    )
}

if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    $content = Get-Content -LiteralPath $exampleEnvironment -Raw
    foreach ($name in @(
        'NEXUS_ADMIN_PASSWORD',
        'NEXUS_PUBLISHER_PASSWORD',
        'NEXUS_READER_PASSWORD'
    )) {
        $content = Set-EnvironmentValue $content $name (New-HexSecret)
    }
    [IO.File]::WriteAllText(
        $runtimeEnvironment,
        $content,
        [Text.UTF8Encoding]::new($false)
    )
    Write-Host 'Generated local credentials in ignored nexus/.env.' -ForegroundColor Green
}

docker version | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not available.'
}

docker compose `
    --project-directory $nexusDirectory `
    --env-file $runtimeEnvironment `
    -f $composeFile `
    up --detach --wait --wait-timeout 600
if ($LASTEXITCODE -ne 0) {
    throw 'Nexus did not become healthy. Inspect: docker compose -f nexus/compose.yml logs nexus'
}

& (Join-Path $PSScriptRoot 'provision-nexus.ps1') -AcceptEula:$AcceptEula
