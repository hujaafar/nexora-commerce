# File purpose: Exports a secret-free snapshot of the live SonarQube dashboard.
#
# Concepts to learn:
# - REST APIs let automation read the same metrics shown in a web dashboard.
# - Basic authentication is created in memory and is never written to reports.
# - A stable `latest.md` file makes audit evidence easy to find.
[CmdletBinding()]
param(
    [string]$ProjectKey = 'nexora-commerce'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$runtimeEnvironment = Join-Path $root 'quality\.env'
$reportDirectory = Join-Path $root 'quality\reports'

if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    throw 'Run scripts\sonarqube-start.ps1 first so ignored quality/.env exists.'
}

$settings = @{}
Get-Content -LiteralPath $runtimeEnvironment | ForEach-Object {
    if ($_ -match '^(?<name>[^#=]+)=(?<value>.*)$') {
        $settings[$Matches.name] = $Matches.value
    }
}

$hostUrl = $settings.SONAR_HOST_URL.TrimEnd('/')
$credentials = "admin:$($settings.SONAR_ADMIN_PASSWORD)"
$authorization = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes($credentials)
)
$headers = @{ Authorization = "Basic $authorization" }

function Invoke-SonarGet([string]$Path) {
    Invoke-RestMethod -Method Get -Headers $headers -Uri "$hostUrl$Path"
}

$metrics = @(
    'coverage', 'duplicated_lines_density', 'bugs', 'vulnerabilities',
    'code_smells', 'security_hotspots', 'security_rating',
    'reliability_rating', 'sqale_rating'
) -join ','
$gate = Invoke-SonarGet "/api/qualitygates/project_status?projectKey=$ProjectKey"
$measures = Invoke-SonarGet "/api/measures/component?component=$ProjectKey&metricKeys=$metrics"
$issues = Invoke-SonarGet "/api/issues/search?componentKeys=$ProjectKey&resolved=false&ps=1"
$hotspots = Invoke-SonarGet "/api/hotspots/search?projectKey=$ProjectKey&status=TO_REVIEW&ps=1"
$analyses = Invoke-SonarGet "/api/project_analyses/search?project=$ProjectKey&ps=1"

$values = @{}
foreach ($measure in $measures.component.measures) {
    $values[$measure.metric] = $measure.value
}

$latestAnalysis = $analyses.analyses | Select-Object -First 1
$snapshot = [ordered]@{
    generatedAtUtc = [DateTime]::UtcNow.ToString('o')
    projectKey = $ProjectKey
    dashboard = "$hostUrl/dashboard?id=$ProjectKey"
    qualityGate = $gate.projectStatus.status
    analysisRevision = $latestAnalysis.revision
    analysisDate = $latestAnalysis.date
    coveragePercent = $values.coverage
    duplicationPercent = $values.duplicated_lines_density
    bugs = $values.bugs
    vulnerabilities = $values.vulnerabilities
    codeSmells = $values.code_smells
    securityHotspots = $values.security_hotspots
    securityRating = $values.security_rating
    reliabilityRating = $values.reliability_rating
    maintainabilityRating = $values.sqale_rating
    openIssues = $issues.total
    hotspotsToReview = $hotspots.paging.total
}

New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$jsonPath = Join-Path $reportDirectory "quality-report-$timestamp.json"
$markdownPath = Join-Path $reportDirectory 'latest.md'
$snapshot | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 -LiteralPath $jsonPath

$markdown = @"
# Nexora Commerce quality snapshot

- Generated (UTC): $($snapshot.generatedAtUtc)
- Quality Gate: **$($snapshot.qualityGate)**
- Analysis revision: $($snapshot.analysisRevision)
- Coverage: $($snapshot.coveragePercent)%
- Duplicated lines: $($snapshot.duplicationPercent)%
- Open issues: $($snapshot.openIssues)
- Bugs / vulnerabilities / code smells: $($snapshot.bugs) / $($snapshot.vulnerabilities) / $($snapshot.codeSmells)
- Security Hotspots awaiting review: $($snapshot.hotspotsToReview)
- Dashboard: $($snapshot.dashboard)

This generated report deliberately contains no password or token.
"@
$markdown | Set-Content -Encoding UTF8 -LiteralPath $markdownPath

Write-Host "Quality Gate: $($snapshot.qualityGate)" -ForegroundColor Green
Write-Host "Open issues: $($snapshot.openIssues); hotspots to review: $($snapshot.hotspotsToReview)"
Write-Host "Report: $markdownPath"

# Remove credential-bearing values from the current scope as soon as possible.
$credentials = $null
$authorization = $null
$settings = @{}
