# Starts the bounded laptop stack; reuse images unless -Build is requested.
[CmdletBinding()]
param([switch]$Build, [switch]$Full)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

# Check the host before asking Docker to allocate memory or build images.
if ($env:OS -eq 'Windows_NT') {
    $freeMemoryGB = (Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1MB
    $driveName = [IO.Path]::GetPathRoot($projectRoot).TrimEnd('\').TrimEnd(':')
    $freeDiskGB = (Get-PSDrive -Name $driveName).Free / 1GB
    if ($freeMemoryGB -lt 3) {
        throw ('Only {0:N1} GB RAM is free. Close memory-heavy apps before starting Nexora (at least 3 GB free).' -f $freeMemoryGB)
    }
    if ($freeDiskGB -lt 5) {
        throw ('Only {0:N1} GB disk space is free. Free at least 5 GB before starting Docker workloads.' -f $freeDiskGB)
    }
}
docker info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Start the Docker Desktop Linux engine, then run this script again.' }
$composeArguments = @('compose', '--project-directory', $projectRoot, '-f', "$projectRoot/compose.yml")
if (-not $Full) { $composeArguments += @('-f', "$projectRoot/compose.laptop.yml") }

if (-not (Test-Path -LiteralPath (Join-Path $projectRoot '.env'))) {
    docker run --rm --memory 128m --cpus 0.5 --mount "type=bind,source=$projectRoot,target=/app" -w /app node:22.12-alpine node scripts/setup.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Could not generate local configuration.' }
}
# Avoid piling the app onto running local CI/quality/artifact servers.
foreach ($project in @('nexora-commerce-ci', 'nexora-commerce-quality', 'nexora-artifacts')) {
    $runningTools = @(docker ps --filter "label=com.docker.compose.project=$project" --format '{{.Names}}')
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect running Docker workloads.' }
    if ($runningTools.Count -gt 0 -and -not $Full) {
        throw 'Optional DevOps tools are running. Use scripts/stop-tools.ps1 before starting the laptop stack.'
    }
}
$appServices = @('discovery-service', 'user-service', 'product-service', 'media-service', 'order-service', 'gateway-service', 'frontend')
$images = @(docker @composeArguments config --images @appServices)
if ($LASTEXITCODE -ne 0) { throw 'Compose configuration is invalid.' }
$needsBuild = $Build.IsPresent
foreach ($image in $images) {
    # Listing by reference avoids expected "image not found" native stderr on PS 5.
    $existingImage = @(docker image ls --quiet --filter "reference=$image")
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect local images.' }
    if ($existingImage.Count -eq 0) { $needsBuild = $true }
}
if ($needsBuild) {
    if ($env:OS -eq 'Windows_NT' -and ($freeDiskGB -lt 10 -or $freeMemoryGB -lt 4)) {
        throw 'Building needs at least 10 GB free disk and 4 GB free host RAM. Free space/memory, then retry.'
    }
    docker @composeArguments --parallel 1 build
    if ($LASTEXITCODE -ne 0) { throw 'Image build failed.' }
}
docker @composeArguments up --no-build --detach --wait --wait-timeout 600
if ($LASTEXITCODE -ne 0) { throw 'Startup failed. Review docker compose logs; scripts/stop.ps1 releases resources.' }
docker @composeArguments ps
$uiAddress = (docker @composeArguments port frontend 80).Replace('0.0.0.0:', 'localhost:')
Write-Host "Nexora Commerce: http://$uiAddress"
docker @composeArguments run --rm --no-deps demo-seed
if ($LASTEXITCODE -ne 0) { throw 'The app started, but demo catalog setup failed.' }
