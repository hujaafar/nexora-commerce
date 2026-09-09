# Verify the installed repositories and a published release using both roles.
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?$')]
    [string]$Version,
    [string]$ImageTag = $Version,
    [switch]$SkipDocker
)
$ErrorActionPreference = 'Stop'
if ($Version.EndsWith('-SNAPSHOT')) { throw 'Verify an immutable release version, not a mutable snapshot.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$settings = @{}
Get-Content -LiteralPath (Join-Path $projectRoot 'nexus/.env') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $settings[$Matches[1].Trim()] = $Matches[2].Trim() }
}
$baseUrl = "http://127.0.0.1:$($settings.NEXUS_HTTP_PORT)"
function New-Headers([string]$Username, [string]$Password) {
    return @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Username}:$Password")) }
}
$adminHeaders = New-Headers $settings.NEXUS_ADMIN_USER $settings.NEXUS_ADMIN_PASSWORD
$publisherHeaders = New-Headers $settings.NEXUS_PUBLISHER_USER $settings.NEXUS_PUBLISHER_PASSWORD
$readerHeaders = New-Headers $settings.NEXUS_READER_USER $settings.NEXUS_READER_PASSWORD
$checks = [Collections.Generic.List[string]]::new()
function Assert-Check([bool]$Condition, [string]$Description) {
    if (-not $Condition) { throw "FAILED: $Description" }
    $checks.Add($Description)
    Write-Host "PASS: $Description"
}
function Request-Status([string]$Method, [string]$Url, [hashtable]$Headers, [AllowNull()][object]$Body) {
    try {
        $arguments = @{ Method = $Method; Uri = $Url; Headers = $Headers; UseBasicParsing = $true; TimeoutSec = 30 }
        if ($null -ne $Body) { $arguments.Body = $Body; $arguments.ContentType = 'application/xml' }
        return [int](Invoke-WebRequest @arguments).StatusCode
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        return [int]$_.Exception.Response.StatusCode
    }
}
$status = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/service/rest/v1/status"
Assert-Check ($status.StatusCode -eq 200) 'Nexus responds successfully'
$repositoryUrl = "$baseUrl/service/rest/v1/repositories"
$repositories = Invoke-RestMethod -Uri $repositoryUrl -Headers $adminHeaders
foreach ($name in @('maven-releases', 'maven-snapshots', 'maven-central', 'maven-public', 'docker-hosted')) {
    Assert-Check ($name -in $repositories.name) "Repository exists: $name"
}
foreach ($format in @('maven', 'docker')) {
    $name = if ($format -eq 'maven') { 'maven-releases' } else { 'docker-hosted' }
    $definition = Invoke-RestMethod -Uri "$repositoryUrl/$format/hosted/$name" -Headers $adminHeaders
    Assert-Check ($definition.storage.writePolicy -eq 'ALLOW_ONCE') "$name rejects redeployment"
}
foreach ($role in @('publisher', 'reader')) {
    $headers = if ($role -eq 'publisher') { $publisherHeaders } else { $readerHeaders }
    Assert-Check ((Request-Status GET "$baseUrl/service/rest/v1/security/users" $headers $null) -eq 403) "$role cannot administer users"
}
$path = "/repository/maven-releases/com/nexora/nexora-commerce/$Version/nexora-commerce-$Version.pom"
Assert-Check ((Request-Status GET "$baseUrl$path" @{} $null) -eq 401) 'Anonymous artifact download is blocked'
$parentPom = (Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl$path" -Headers $readerHeaders).Content
Assert-Check (-not $parentPom.Contains('${revision}')) 'Published parent POM has a resolved version'
Assert-Check ((Request-Status PUT "$baseUrl$path" $readerHeaders $parentPom) -eq 403) 'Reader cannot publish'
Assert-Check ((Request-Status PUT "$baseUrl$path" $publisherHeaders $parentPom) -in @(400, 409)) 'Publisher cannot replace a release POM'
foreach ($service in @('discovery-service', 'gateway-service', 'user-service', 'product-service', 'media-service', 'order-service')) {
    $artifactBase = "$baseUrl/repository/maven-releases/com/nexora/$service/$Version/$service-$Version"
    $pom = (Invoke-WebRequest -UseBasicParsing -Uri "$artifactBase.pom" -Headers $readerHeaders).Content
    Assert-Check (-not $pom.Contains('${revision}')) "$service POM resolves its parent version"
    Assert-Check ((Request-Status HEAD "$artifactBase.jar" $readerHeaders $null) -eq 200) "$service JAR is downloadable"
}
$cached = Invoke-RestMethod -Uri "$baseUrl/service/rest/v1/assets?repository=maven-central" -Headers $adminHeaders
Assert-Check (@($cached.items).Count -gt 0) 'Maven Central proxy has cached artifacts'

if (-not $SkipDocker) {
    $registry = "localhost:$($settings.NEXUS_DOCKER_PORT)"
    $temporaryAuth = Join-Path ([IO.Path]::GetTempPath()) ('nexora-reader-auth-' + [guid]::NewGuid().ToString('N'))
    $container = 'nexora-artifact-check-' + [guid]::NewGuid().ToString('N')
    New-Item -ItemType Directory -Path $temporaryAuth | Out-Null
    try {
        $settings.NEXUS_READER_PASSWORD | docker --config $temporaryAuth login $registry `
            --username $settings.NEXUS_READER_USER --password-stdin
        Assert-Check ($LASTEXITCODE -eq 0) 'Read-only registry login succeeds'
        foreach ($service in @('discovery-service', 'gateway-service', 'user-service', 'product-service', 'media-service', 'order-service', 'frontend')) {
            $image = "$registry/nexora-commerce/${service}:$ImageTag"
            docker --config $temporaryAuth pull $image
            Assert-Check ($LASTEXITCODE -eq 0) "Reader can pull $service"
            $imageVersion = docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' $image
            Assert-Check ($LASTEXITCODE -eq 0 -and $imageVersion -eq $Version) "$service image has the release version"
        }
        docker run --detach --name $container -p '127.0.0.1::8761' "$registry/nexora-commerce/discovery-service:$ImageTag" | Out-Null
        Assert-Check ($LASTEXITCODE -eq 0) 'Pulled discovery image starts'
        $inspection = docker inspect $container | ConvertFrom-Json
        $port = $inspection[0].NetworkSettings.Ports.'8761/tcp'[0].HostPort
        $deadline = (Get-Date).AddMinutes(3)
        $healthy = $false
        do {
            try {
                $health = Invoke-RestMethod -Uri "http://127.0.0.1:$port/actuator/health" -TimeoutSec 5
                $healthy = $health.status -eq 'UP'
            } catch { $healthy = $false }
            if (-not $healthy) { Start-Sleep -Seconds 3 }
        } until ($healthy -or (Get-Date) -ge $deadline)
        Assert-Check $healthy 'Pulled discovery image becomes healthy'
    } finally {
        docker rm --force $container 2>$null | Out-Null
        $resolvedAuth = [IO.Path]::GetFullPath($temporaryAuth)
        $temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if (-not $resolvedAuth.StartsWith($temporaryRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected auth directory.' }
        Remove-Item -LiteralPath $resolvedAuth -Recurse -Force
    }
}
$reportDirectory = Join-Path $projectRoot 'test-results/nexus'
New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
@{ version = $Version; imageTag = $ImageTag; verifiedAt = (Get-Date).ToUniversalTime().ToString('o'); checks = $checks.ToArray() } |
    ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $reportDirectory 'verification.json')
Write-Host "All $($checks.Count) Nexus checks passed."
