[CmdletBinding()]
param([switch]$NoBuild)
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
Push-Location $repository
try {
    docker run --rm --mount "type=bind,source=$repository,target=/workspace" -w /workspace `
        maven:3.9.11-eclipse-temurin-17 bash scripts/tls-init.sh
    if ($LASTEXITCODE -ne 0) { throw 'TLS certificate generation failed' }
    $arguments = @('compose', '--env-file', '.env', '--env-file', 'certs/tls/.env', '-f', 'compose.yml', '-f', 'compose.tls.yml', 'up', '-d', '--wait', '--wait-timeout', '600')
    if ($NoBuild) { $arguments += '--no-build' } else { $arguments += '--build' }
    & docker @arguments
    if ($LASTEXITCODE -ne 0) { throw 'TLS deployment did not become healthy' }
    Write-Host 'Storefront: https://localhost:8443'
    Write-Host 'Local CA: certs/tls/trust/ca.pem (no system trust settings were changed).'
} finally { Pop-Location }
