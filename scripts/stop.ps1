# BUY-01 learning header
# File purpose: Stops the Compose platform while preserving development volumes.
# Learning focus: Safe lifecycle automation and persistent data.
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
docker compose --project-directory $projectRoot down
