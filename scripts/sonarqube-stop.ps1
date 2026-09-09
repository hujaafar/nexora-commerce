# File purpose: Stops local SonarQube containers without deleting analysis
# history. Add -RemoveData only when intentionally resetting the whole server.
[CmdletBinding()]
param([switch]$RemoveData)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$qualityDirectory = Join-Path $projectRoot 'quality'
$runtimeEnvironment = Join-Path $qualityDirectory '.env'
$arguments = @(
    'compose',
    '--project-directory', $qualityDirectory,
    '--env-file', $runtimeEnvironment,
    '-f', (Join-Path $qualityDirectory 'compose.yml'),
    'down', '--remove-orphans'
)

if ($RemoveData) {
    # Named volumes contain the database and dashboard history, so this switch
    # is deliberately explicit and visible at the command line.
    $arguments += '--volumes'
}

& docker @arguments
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose could not stop SonarQube.' }
