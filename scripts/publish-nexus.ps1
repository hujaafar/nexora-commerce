# Publish the actual marketplace artifacts from this checkout.
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?$')]
    [string]$Version,
    [switch]$SkipDocker
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $projectRoot 'nexus/.env'
if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Run scripts/start-nexus.ps1 first.' }
$settings = @{}
Get-Content -LiteralPath $environmentFile | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $settings[$Matches[1].Trim()] = $Matches[2].Trim() }
}
function Invoke-DockerStep {
    param([string[]]$Arguments)
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'Docker build, test, or publication failed; stopping.' }
}
$environmentNames = @('NEXUS_USERNAME', 'NEXUS_PASSWORD', 'NEXUS_BASE_URL', 'ARTIFACT_VERSION')
$previousEnvironment = @{}
foreach ($name in $environmentNames) { $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name) }
$temporaryAuth = Join-Path ([IO.Path]::GetTempPath()) ('nexora-docker-auth-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporaryAuth | Out-Null
Push-Location $projectRoot
try {
    $env:NEXUS_USERNAME = $settings.NEXUS_PUBLISHER_USER
    $env:NEXUS_PASSWORD = $settings.NEXUS_PUBLISHER_PASSWORD
    # Compose DNS keeps Maven independent of host port mappings on every OS.
    $env:NEXUS_BASE_URL = 'http://nexus:8081'
    $env:ARTIFACT_VERSION = $Version
    Invoke-DockerStep @('run', '--rm', '--network', 'nexora-artifacts_default',
        '--mount', "type=bind,source=$projectRoot,target=/workspace",
        '-v', 'nexora-artifacts-maven-cache:/root/.m2', '-w', '/workspace',
        '-e', 'NEXUS_USERNAME', '-e', 'NEXUS_PASSWORD', '-e', 'NEXUS_BASE_URL', '-e', 'ARTIFACT_VERSION',
        'maven:3.9.11-eclipse-temurin-17', 'bash', 'scripts/ci/nexus-maven.sh', 'publish')
    if (-not $SkipDocker) {
        Invoke-DockerStep @('run', '--rm', '--mount', "type=bind,source=$projectRoot,target=/workspace",
            '-v', 'nexora-artifacts-node-modules:/workspace/frontend/node_modules',
            '-v', 'nexora-artifacts-npm-cache:/root/.npm', '-w', '/workspace/frontend',
            'node:24-bookworm-slim', 'sh', '-ec',
            'npm ci && npm audit --omit=dev --audit-level=high && npm run test:ci && node ../scripts/motion-test.mjs && npm run build')
        $registry = "localhost:$($settings.NEXUS_DOCKER_PORT)"
        $settings.NEXUS_PUBLISHER_PASSWORD | docker --config $temporaryAuth login $registry `
            --username $settings.NEXUS_PUBLISHER_USER --password-stdin
        if ($LASTEXITCODE -ne 0) { throw 'Nexus registry login failed.' }
        $sourceCommit = git rev-parse HEAD
        if ($LASTEXITCODE -ne 0) { throw 'Could not identify source commit.' }
        $evidenceDirectory = Join-Path $projectRoot 'test-results/images'
        New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
        $manifest = @()
        foreach ($service in @('discovery-service', 'gateway-service', 'user-service',
            'product-service', 'media-service', 'order-service', 'frontend')) {
            $dockerfile = if ($service -eq 'frontend') { 'docker/Dockerfile.frontend-artifact' } else { 'docker/Dockerfile.artifact' }
            $image = "$registry/nexora-commerce/${service}:$Version"
            Invoke-DockerStep @('build', '-f', $dockerfile, '--build-arg', "SERVICE=$service",
                '--build-arg', "ARTIFACT_VERSION=$Version", '--build-arg', "SOURCE_COMMIT=$sourceCommit", '-t', $image, '.')
            Invoke-DockerStep @('--config', $temporaryAuth, 'push', $image)
            $manifest += docker image inspect --format '{{join .RepoDigests "\n"}}' $image
            if ($LASTEXITCODE -ne 0) { throw 'Could not record image digest.' }
        }
        $manifest | Set-Content -LiteralPath (Join-Path $evidenceDirectory 'nexus-local-push.txt')
    }
    Write-Host "Published Nexora Commerce $Version. Evidence is in test-results/."
} finally {
    Pop-Location
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name]) }
    # Delete only the exact temporary auth directory created by this invocation.
    $resolvedAuth = [IO.Path]::GetFullPath($temporaryAuth)
    $temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if (-not $resolvedAuth.StartsWith($temporaryRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected auth directory.' }
    Remove-Item -LiteralPath $resolvedAuth -Recurse -Force
}
