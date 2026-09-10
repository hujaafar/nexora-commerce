# Configures Nexus through its supported REST API. Re-running is safe.
[CmdletBinding()]
param(
    # Legal terms require an explicit operator choice; automation never assumes it.
    [switch]$AcceptEula
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nexusDirectory = Join-Path $projectRoot 'nexus'
$runtimeEnvironment = Join-Path $nexusDirectory '.env'
$composeFile = Join-Path $nexusDirectory 'compose.yml'

if (-not (Test-Path -LiteralPath $runtimeEnvironment)) {
    throw 'Run scripts/start-nexus.ps1 first so local credentials are generated.'
}

$settings = @{}
Get-Content -LiteralPath $runtimeEnvironment | ForEach-Object {
    if ($_ -match '^(?<name>[^#=]+)=(?<value>.*)$') {
        $settings[$Matches.name.Trim()] = $Matches.value.Trim()
    }
}

$baseUrl = "http://localhost:$($settings.NEXUS_HTTP_PORT)"

function New-BasicHeaders {
    param([string]$Username, [string]$Password)

    $token = [Convert]::ToBase64String(
        [Text.Encoding]::UTF8.GetBytes("$Username`:$Password")
    )
    return @{ Authorization = "Basic $token" }
}

function Invoke-Nexus {
    param(
        [ValidateSet('GET', 'POST', 'PUT', 'DELETE')][string]$Method,
        [string]$Path,
        [hashtable]$Headers,
        [object]$Body,
        [string]$ContentType = 'application/json',
        [switch]$AllowNotFound
    )

    $request = @{
        Method = $Method
        Uri = "$baseUrl$Path"
        Headers = $Headers
        UseBasicParsing = $true
        TimeoutSec = 60
    }
    if ($null -ne $Body) {
        $request.ContentType = $ContentType
        $request.Body = if ($ContentType -eq 'application/json') {
            $Body | ConvertTo-Json -Depth 20
        } else {
            [string]$Body
        }
    }

    try {
        return Invoke-WebRequest @request
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        if ($AllowNotFound -and $status -eq 404) {
            return $null
        }
        throw
    }
}

function Test-NexusLogin {
    param([hashtable]$Headers)

    try {
        Invoke-Nexus GET '/service/rest/v1/security/users?userId=admin' $Headers $null | Out-Null
        return $true
    } catch {
        return $false
    }
}

$adminHeaders = New-BasicHeaders $settings.NEXUS_ADMIN_USER $settings.NEXUS_ADMIN_PASSWORD
if (-not (Test-NexusLogin $adminHeaders)) {
    # First boot generates a one-time password inside the protected data volume.
    $initialPassword = docker compose `
        --project-directory $nexusDirectory `
        --env-file $runtimeEnvironment `
        -f $composeFile `
        exec -T nexus sh -c 'cat /nexus-data/admin.password'
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($initialPassword)) {
        throw 'Could not read the initial Nexus admin password.'
    }

    $initialHeaders = New-BasicHeaders $settings.NEXUS_ADMIN_USER $initialPassword.Trim()
    Invoke-Nexus `
        PUT `
        '/service/rest/v1/security/users/admin/change-password' `
        $initialHeaders `
        $settings.NEXUS_ADMIN_PASSWORD `
        'text/plain' | Out-Null
    if (-not (Test-NexusLogin $adminHeaders)) {
        throw 'Nexus admin password initialization failed.'
    }
}

# Current Nexus Community Edition blocks artifact traffic until the operator
# accepts its EULA. The switch makes that legal choice visible and auditable.
$eulaResponse = Invoke-Nexus GET '/service/rest/v1/system/eula' $adminHeaders $null
$eulaStatus = $eulaResponse.Content | ConvertFrom-Json
if (-not $eulaStatus.accepted) {
    if (-not $AcceptEula) {
        throw 'Nexus Community Edition EULA is not accepted. Review it in the Nexus UI, then rerun with -AcceptEula only if you agree.'
    }
    Invoke-Nexus `
        POST `
        '/service/rest/v1/system/eula' `
        $adminHeaders `
        @{
            accepted = $true
            # Recent Nexus CE releases validate that the operator accepted the
            # exact disclaimer currently served by this installation.
            disclaimer = $eulaStatus.disclaimer
        } | Out-Null
    Write-Host 'Recorded explicit Nexus Community Edition EULA acceptance.'
}

function Set-Repository {
    param(
        [string]$Format,
        [string]$Type,
        [string]$Name,
        [hashtable]$Definition
    )

    $path = "/service/rest/v1/repositories/$Format/$Type"
    $existing = Invoke-Nexus GET "$path/$Name" $adminHeaders $null -AllowNotFound
    if ($null -eq $existing) {
        Invoke-Nexus POST $path $adminHeaders $Definition | Out-Null
        Write-Host "Created repository $Name"
    } else {
        Invoke-Nexus PUT "$path/$Name" $adminHeaders $Definition | Out-Null
        Write-Host "Updated repository $Name"
    }
}

$mutableHostedStorage = @{
    blobStoreName = 'default'
    strictContentTypeValidation = $true
    writePolicy = 'ALLOW'
}

# A release path can be uploaded once but never silently replaced. Snapshots
# remain writable because their publication protocol updates metadata.
$releaseStorage = @{
    blobStoreName = 'default'
    strictContentTypeValidation = $true
    writePolicy = 'ALLOW_ONCE'
}

$releaseRepository = @{
    name = $settings.NEXUS_MAVEN_RELEASES
    online = $true
    storage = $releaseStorage
    maven = @{ versionPolicy = 'RELEASE'; layoutPolicy = 'STRICT' }
}
Set-Repository maven hosted $settings.NEXUS_MAVEN_RELEASES $releaseRepository

$snapshotRepository = @{
    name = $settings.NEXUS_MAVEN_SNAPSHOTS
    online = $true
    storage = $mutableHostedStorage
    maven = @{ versionPolicy = 'SNAPSHOT'; layoutPolicy = 'STRICT' }
}
Set-Repository maven hosted $settings.NEXUS_MAVEN_SNAPSHOTS $snapshotRepository

$proxyRepository = @{
    name = $settings.NEXUS_MAVEN_PROXY
    online = $true
    storage = @{
        blobStoreName = 'default'
        strictContentTypeValidation = $false
    }
    proxy = @{
        remoteUrl = 'https://repo1.maven.org/maven2/'
        contentMaxAge = 1440
        metadataMaxAge = 1440
    }
    negativeCache = @{ enabled = $true; timeToLive = 1440 }
    httpClient = @{
        blocked = $false
        autoBlock = $true
        connection = @{
            retries = 2
            timeout = 60
            enableCircularRedirects = $false
            enableCookies = $false
            useTrustStore = $false
        }
    }
    maven = @{ versionPolicy = 'MIXED'; layoutPolicy = 'STRICT' }
}
Set-Repository maven proxy $settings.NEXUS_MAVEN_PROXY $proxyRepository

$groupRepository = @{
    name = $settings.NEXUS_MAVEN_GROUP
    online = $true
    storage = @{
        blobStoreName = 'default'
        strictContentTypeValidation = $true
    }
    group = @{
        memberNames = @(
            $settings.NEXUS_MAVEN_RELEASES,
            $settings.NEXUS_MAVEN_SNAPSHOTS,
            $settings.NEXUS_MAVEN_PROXY
        )
    }
    maven = @{ versionPolicy = 'MIXED'; layoutPolicy = 'STRICT' }
}
Set-Repository maven group $settings.NEXUS_MAVEN_GROUP $groupRepository

$dockerRepository = @{
    name = $settings.NEXUS_DOCKER_REPOSITORY
    online = $true
    storage = $releaseStorage
    docker = @{
        v1Enabled = $false
        forceBasicAuth = $true
        httpPort = 8082
    }
}
Set-Repository docker hosted $settings.NEXUS_DOCKER_REPOSITORY $dockerRepository

# Docker clients need the token realm in addition to ordinary username/password auth.
$realmResponse = Invoke-Nexus GET '/service/rest/v1/security/realms/active' $adminHeaders $null
$realms = @($realmResponse.Content | ConvertFrom-Json)
# Authorization is built into current Nexus releases and is not an activatable
# realm. Only authentication plus the Docker bearer-token realm belong here.
foreach ($requiredRealm in @('NexusAuthenticatingRealm', 'DockerToken')) {
    if ($realms -notcontains $requiredRealm) {
        $realms += $requiredRealm
    }
}
Invoke-Nexus PUT '/service/rest/v1/security/realms/active' $adminHeaders $realms | Out-Null

function Set-Role {
    param([string]$Id, [string]$Name, [string]$Description, [string[]]$Privileges)

    $body = @{
        id = $Id
        name = $Name
        description = $Description
        privileges = $Privileges
        roles = @()
    }
    $existing = Invoke-Nexus GET "/service/rest/v1/security/roles/$Id" $adminHeaders $null -AllowNotFound
    if ($null -eq $existing) {
        Invoke-Nexus POST '/service/rest/v1/security/roles' $adminHeaders $body | Out-Null
    } else {
        Invoke-Nexus PUT "/service/rest/v1/security/roles/$Id" $adminHeaders $body | Out-Null
    }
}

$readPrivileges = @(
    'nx-search-read',
    "nx-repository-view-docker-$($settings.NEXUS_DOCKER_REPOSITORY)-browse",
    "nx-repository-view-docker-$($settings.NEXUS_DOCKER_REPOSITORY)-read"
)
# Nexus checks permissions on both a Maven group and the member repository that
# serves a component. Read/browse on named members is therefore required while
# add/edit remains limited to the two hosted publication targets.
foreach ($repository in @(
    $settings.NEXUS_MAVEN_GROUP,
    $settings.NEXUS_MAVEN_RELEASES,
    $settings.NEXUS_MAVEN_SNAPSHOTS,
    $settings.NEXUS_MAVEN_PROXY
)) {
    foreach ($action in @('browse', 'read')) {
        $readPrivileges += "nx-repository-view-maven2-$repository-$action"
    }
}
Set-Role 'artifact-reader' 'Artifact reader' 'Read-only access to approved Maven and Docker artifacts.' $readPrivileges

$publishPrivileges = @($readPrivileges)
foreach ($repository in @($settings.NEXUS_MAVEN_RELEASES, $settings.NEXUS_MAVEN_SNAPSHOTS)) {
    foreach ($action in @('browse', 'read', 'add', 'edit')) {
        $publishPrivileges += "nx-repository-view-maven2-$repository-$action"
    }
}
foreach ($action in @('browse', 'read', 'add', 'edit')) {
    $publishPrivileges += "nx-repository-view-docker-$($settings.NEXUS_DOCKER_REPOSITORY)-$action"
}
Set-Role 'artifact-publisher' 'Artifact publisher' 'CI may publish, but cannot administer Nexus or delete artifacts.' $publishPrivileges

function Set-NexusUser {
    param(
        [string]$UserId,
        [string]$Password,
        [string]$FirstName,
        [string]$LastName,
        [string]$Role
    )

    $response = Invoke-Nexus GET "/service/rest/v1/security/users?userId=$UserId" $adminHeaders $null
    $users = @($response.Content | ConvertFrom-Json)
    $body = @{
        userId = $UserId
        firstName = $FirstName
        lastName = $LastName
        emailAddress = 'artifacts@example.com'
        # ApiUser updates require the backing user source in current Nexus.
        source = 'default'
        status = 'active'
        roles = @($Role)
    }
    if ($users.Count -eq 0) {
        $body.password = $Password
        Invoke-Nexus POST '/service/rest/v1/security/users' $adminHeaders $body | Out-Null
    } else {
        Invoke-Nexus PUT "/service/rest/v1/security/users/$UserId" $adminHeaders $body | Out-Null
        Invoke-Nexus `
            PUT `
            "/service/rest/v1/security/users/$UserId/change-password" `
            $adminHeaders `
            $Password `
            'text/plain' | Out-Null
    }
}

Set-NexusUser `
    $settings.NEXUS_PUBLISHER_USER `
    $settings.NEXUS_PUBLISHER_PASSWORD `
    'CI' `
    'Publisher' `
    'artifact-publisher'
Set-NexusUser `
    $settings.NEXUS_READER_USER `
    $settings.NEXUS_READER_PASSWORD `
    'Artifact' `
    'Reader' `
    'artifact-reader'

# Anonymous access is disabled: every download and dashboard view is attributable.
$anonymous = @{
    enabled = $false
    userId = 'anonymous'
    realmName = 'NexusAuthorizingRealm'
}
Invoke-Nexus PUT '/service/rest/v1/security/anonymous' $adminHeaders $anonymous | Out-Null

[IO.File]::WriteAllText(
    (Join-Path $nexusDirectory '.bootstrap-complete'),
    "Provisioned $(Get-Date -Format o)`n",
    [Text.UTF8Encoding]::new($false)
)

Write-Host ''
Write-Host 'Nexus provisioning is complete.' -ForegroundColor Green
Write-Host "Dashboard:       $baseUrl"
Write-Host "Docker registry: localhost:$($settings.NEXUS_DOCKER_PORT)"
Write-Host 'Credentials remain only in ignored nexus/.env.'
