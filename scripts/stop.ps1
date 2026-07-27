# File purpose: Stops the Compose platform while preserving development volumes.
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
docker compose --project-directory $projectRoot down
