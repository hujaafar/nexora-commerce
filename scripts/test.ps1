# File purpose: Runs reproducible backend and frontend verification.
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot

docker run --rm `
    --volume "${projectRoot}:/workspace" `
    --volume "nexora-maven-cache:/root/.m2" `
    --workdir /workspace `
    maven:3.9.11-eclipse-temurin-17 `
    mvn verify
if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }

Push-Location (Join-Path $projectRoot 'frontend')
try {
    npm.cmd ci
    if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed.' }
    npm.cmd run test:ci
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
    npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
}
finally {
    Pop-Location
}
