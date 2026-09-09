# File purpose: Stops Jenkins while preserving controller, image, and deployment state by default.
[CmdletBinding()]
param(
    [switch]$RemoveData
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$jenkinsDirectory = Join-Path $projectRoot 'jenkins'
$composeFile = Join-Path $jenkinsDirectory 'compose.yml'
$runtimeEnvironment = Join-Path $jenkinsDirectory '.env'

if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    throw 'jenkins/.env does not exist. The Jenkins stack has not been initialized.'
}

$arguments = @(
    'compose',
    '--project-directory', $jenkinsDirectory,
    '--env-file', $runtimeEnvironment,
    '-f', $composeFile,
    'down', '--remove-orphans'
)

if ($RemoveData) {
    Write-Warning 'Removing volumes permanently deletes Jenkins jobs, credentials, build history, images, and rollback state.'
    $arguments += '--volumes'
}

docker @arguments
if ($LASTEXITCODE -ne 0) {
    throw 'Jenkins shutdown failed.'
}

Write-Host 'Jenkins stopped.' -ForegroundColor Green
if (-not $RemoveData) {
    Write-Host 'Persistent Jenkins and deployment data was preserved.'
}
