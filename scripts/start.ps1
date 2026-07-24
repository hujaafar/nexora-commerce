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

Write-Host ''
Write-Host 'BUY-01 is starting:'
Write-Host '  UI:        http://localhost:4200'
Write-Host '  Gateway:   http://localhost:8080'
Write-Host '  Discovery: http://localhost:8761'
Write-Host '  MinIO:     http://localhost:9001'
