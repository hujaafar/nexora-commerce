# BUY-01 learning header
# File purpose: Runs reproducible backend and frontend verification.
# Learning focus: Containerized build toolchains and full regression suites.
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot

docker run --rm `
    --volume "${projectRoot}:/workspace" `
    --volume "buy01-maven-cache:/root/.m2" `
    --workdir /workspace `
    maven:3.9.11-eclipse-temurin-17 `
    mvn test

Push-Location (Join-Path $projectRoot 'frontend')
try {
    npm ci
    npm test
    npm run build
}
finally {
    Pop-Location
}
