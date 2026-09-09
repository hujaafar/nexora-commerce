# File purpose: Runs the same tests, coverage, static analysis, and blocking
# Quality Gate locally that CI uses before deployment.
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [string]$ProjectVersion = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$qualityEnvironment = Join-Path $projectRoot 'quality\.env'

if (-not (Test-Path -LiteralPath $qualityEnvironment)) {
    throw 'Run scripts\sonarqube-start.ps1 first so a local token exists.'
}

$settings = @{}
Get-Content -LiteralPath $qualityEnvironment | ForEach-Object {
    if ($_ -match '^(?<name>[^#=]+)=(?<value>.*)$') {
        $settings[$Matches.name.Trim()] = $Matches.value.Trim()
    }
}
if ([string]::IsNullOrWhiteSpace($settings.SONAR_TOKEN)) {
    throw 'SONAR_TOKEN is missing from ignored quality/.env.'
}

Push-Location $projectRoot
try {
    if (-not $SkipBuild) {
        # Containers avoid machine-specific Java/npm trust stores and match CI.
        docker volume create nexora-commerce-local-maven-cache | Out-Null
        docker run --rm `
            --volume "${projectRoot}:/workspace" `
            --volume 'nexora-commerce-local-maven-cache:/root/.m2' `
            --workdir /workspace `
            maven:3.9.11-eclipse-temurin-17 `
            ./mvnw -B -ntp clean verify
        if ($LASTEXITCODE -ne 0) { throw 'Maven tests failed; analysis stopped.' }

        docker volume create nexora-commerce-local-node-modules | Out-Null
        docker volume create nexora-commerce-local-npm-cache | Out-Null
        docker run --rm `
            --volume "${projectRoot}\frontend:/workspace" `
            --volume 'nexora-commerce-local-node-modules:/workspace/node_modules' `
            --volume 'nexora-commerce-local-npm-cache:/cache' `
            --env NPM_CONFIG_CACHE=/cache `
            --workdir /workspace `
            node:22-bookworm-slim `
            sh -c 'npm ci && npm audit --omit=dev --audit-level=high && npm run test:ci && npm run build'
        if ($LASTEXITCODE -ne 0) { throw 'Angular test, audit, or build failed.' }
    }

    # Angular's LCOV paths are relative to the frontend directory. SonarQube
    # scans from the repository root, so normalize only the generated report.
    $lcovReport = Join-Path $projectRoot 'frontend\coverage\frontend\lcov.info'
    if (Test-Path -LiteralPath $lcovReport) {
        $lcov = [IO.File]::ReadAllText($lcovReport)
        $lcov = $lcov.Replace('SF:src/', 'SF:frontend/src/')
        [IO.File]::WriteAllText($lcovReport, $lcov)
    }

    $env:SONAR_HOST_URL = $settings.SONAR_DOCKER_HOST_URL
    $env:SONAR_TOKEN = $settings.SONAR_TOKEN
    $revision = (& git rev-parse HEAD).Trim()
    $analysisVersion = if ([string]::IsNullOrWhiteSpace($ProjectVersion)) {
        $revision
    } else {
        $ProjectVersion
    }
    $scannerImage = 'sonarsource/sonar-scanner-cli:12.1.0.3233_8.0.1@sha256:23ca0f137965d9dff2198074043fd48d386280bc5d0ccac8c8349cea4cf096a9'

    docker volume create nexora-commerce-sonar-scanner-cache | Out-Null
    docker run --rm `
        --volume 'nexora-commerce-sonar-scanner-cache:/cache' `
        alpine:3.22 `
        chown -R 1000:1000 /cache
    docker run --rm `
        --add-host host.docker.internal:host-gateway `
        --env SONAR_HOST_URL `
        --env SONAR_TOKEN `
        --volume "${projectRoot}:/usr/src" `
        --volume 'nexora-commerce-sonar-scanner-cache:/opt/sonar-scanner/.sonar/cache' `
        --workdir /usr/src `
        $scannerImage `
        '-Dsonar.working.directory=/usr/src/.scannerwork' `
        "-Dsonar.projectVersion=$analysisVersion" `
        "-Dsonar.scm.revision=$revision"
    if ($LASTEXITCODE -ne 0) {
        throw 'SonarQube analysis or its Quality Gate failed.'
    }
} finally {
    Remove-Item Env:SONAR_TOKEN -ErrorAction SilentlyContinue
    Pop-Location
}
