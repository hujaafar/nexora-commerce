# Displays locally generated credentials only when the operator explicitly asks.
[CmdletBinding()]
param()

$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeEnvironment = Join-Path $projectRoot 'nexus/.env'
if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    throw 'Run scripts/start-nexus.ps1 first.'
}

$settings = @{}
Get-Content -LiteralPath $runtimeEnvironment | ForEach-Object {
    if ($_ -match '^(?<name>[^#=]+)=(?<value>.*)$') {
        $settings[$Matches.name.Trim()] = $Matches.value.Trim()
    }
}

Write-Host "Dashboard: http://localhost:$($settings.NEXUS_HTTP_PORT)"
Write-Host "Admin user: $($settings.NEXUS_ADMIN_USER)"
Write-Host "Admin password: $($settings.NEXUS_ADMIN_PASSWORD)"
Write-Host "Publisher user: $($settings.NEXUS_PUBLISHER_USER)"
Write-Host "Publisher password: $($settings.NEXUS_PUBLISHER_PASSWORD)"
Write-Host "Read-only user: $($settings.NEXUS_READER_USER)"
Write-Host "Read-only password: $($settings.NEXUS_READER_PASSWORD)"
