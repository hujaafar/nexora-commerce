# File purpose: Builds and starts the complete Compose platform from PowerShell.
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$envExample = Join-Path $projectRoot '.env.example'

if (-not (Test-Path -LiteralPath $envFile)) {
    Copy-Item -LiteralPath $envExample -Destination $envFile
    Write-Host 'Created .env from .env.example. Change its secrets before public deployment.'
}

docker compose --project-directory $projectRoot up --build --detach
docker compose --project-directory $projectRoot ps

$uiAddress = (docker compose --project-directory $projectRoot port frontend 80).Replace('0.0.0.0:', 'localhost:')
$gatewayAddress = (docker compose --project-directory $projectRoot port gateway-service 8080).Replace('0.0.0.0:', 'localhost:')
$discoveryAddress = (docker compose --project-directory $projectRoot port discovery-service 8761).Replace('0.0.0.0:', 'localhost:')
$minioAddress = (docker compose --project-directory $projectRoot port minio 9001).Replace('0.0.0.0:', 'localhost:')

Write-Host ''
Write-Host 'BUY-01 is starting:'
Write-Host "  UI:        http://$uiAddress"
Write-Host "  Gateway:   http://$gatewayAddress"
Write-Host "  Discovery: http://$discoveryAddress"
Write-Host "  MinIO:     http://$minioAddress"
