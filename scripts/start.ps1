# File purpose: Builds and starts the complete Compose platform from PowerShell.
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$envExample = Join-Path $projectRoot '.env.example'

if (-not (Test-Path -LiteralPath $envFile)) {
    docker run --rm --mount "type=bind,source=$projectRoot,target=/app" -w /app node:22.12-alpine node scripts/setup.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Could not generate local configuration.' }
}

docker compose --project-directory $projectRoot up --build --detach --wait --wait-timeout 360
if ($LASTEXITCODE -ne 0) { throw 'Startup failed. Review docker compose logs.' }
docker compose --project-directory $projectRoot ps

$uiAddress = (docker compose --project-directory $projectRoot port frontend 80).Replace('0.0.0.0:', 'localhost:')
$gatewayAddress = (docker compose --project-directory $projectRoot port gateway-service 8080).Replace('0.0.0.0:', 'localhost:')
$discoveryAddress = (docker compose --project-directory $projectRoot port discovery-service 8761).Replace('0.0.0.0:', 'localhost:')
$minioAddress = (docker compose --project-directory $projectRoot port minio 9001).Replace('0.0.0.0:', 'localhost:')

Write-Host ''
Write-Host 'Nexora Commerce is starting:'
Write-Host "  UI:        http://$uiAddress"
Write-Host "  Gateway:   http://$gatewayAddress"
Write-Host "  Discovery: http://$discoveryAddress"
Write-Host "  MinIO:     http://$minioAddress"

docker compose --project-directory $projectRoot run --rm --no-deps demo-seed
if ($LASTEXITCODE -ne 0) { throw 'The app started, but demo catalog setup failed.' }
