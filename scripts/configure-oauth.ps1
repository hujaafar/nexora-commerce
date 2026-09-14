[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('google', 'github')]
    [string]$Provider,
    [string]$PublicOrigin
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Run scripts/setup.mjs first to create your private .env.' }
$content = [IO.File]::ReadAllText($environmentFile)
if (-not $PublicOrigin) {
    $originMatch = [regex]::Match($content, '(?m)^PUBLIC_ORIGIN=([^\r\n]+)')
    $PublicOrigin = if ($originMatch.Success) { $originMatch.Groups[1].Value } else { 'http://127.0.0.1:4200' }
}
$PublicOrigin = $PublicOrigin.TrimEnd('/')
$originUri = [uri]$PublicOrigin
$localHttp = $originUri.Scheme -eq 'http' -and $originUri.Host -in @('localhost','127.0.0.1','[::1]')
if ((-not $localHttp -and $originUri.Scheme -ne 'https') -or $originUri.UserInfo -or $originUri.Query -or $originUri.Fragment -or $originUri.AbsolutePath -ne '/') {
    throw 'Use an HTTPS origin, or HTTP on localhost/127.0.0.1, without a path.'
}
Write-Host "Register this exact callback URL with ${Provider}: $PublicOrigin/api/auth/oauth2/callback/$Provider"

function Read-PrivateValue([string]$Prompt) {
    $secureValue = Read-Host $Prompt -AsSecureString
    $valuePointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($valuePointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($valuePointer) }
}

$clientId = Read-PrivateValue 'Client ID (input hidden)'
$clientSecret = Read-PrivateValue 'Client secret (input hidden)'
if ($Provider -eq 'google' -and $clientId -notmatch '^[a-zA-Z0-9._-]+\.apps\.googleusercontent\.com$') {
    throw 'The Google Client ID must end in .apps.googleusercontent.com. Nothing was saved.'
}
if ($Provider -eq 'github' -and $clientId -notmatch '^[a-zA-Z0-9]{16,24}$') {
    throw 'Use the GitHub OAuth App Client ID, not its client secret. Nothing was saved.'
}
if ($clientSecret -notmatch '^[a-zA-Z0-9._-]{16,256}$') { throw 'The client secret format is invalid. Nothing was saved.' }
$values = @{
    PUBLIC_ORIGIN = $PublicOrigin
    OAUTH_COOKIE_SECURE = ($originUri.Scheme -eq 'https').ToString().ToLowerInvariant()
}
$values[$Provider.ToUpperInvariant() + '_CLIENT_ID'] = $clientId
$values[$Provider.ToUpperInvariant() + '_CLIENT_SECRET'] = $clientSecret
# The API gateway also needs the canonical origin when the app is reached through an alias.
$corsMatch = [regex]::Match($content, '(?m)^ALLOWED_ORIGINS=([^\r\n]*)')
$origins = if ($corsMatch.Success) { @($corsMatch.Groups[1].Value.Split(',')) } else { @() }
$values['ALLOWED_ORIGINS'] = (@($origins + $PublicOrigin | Select-Object -Unique) -join ',')
foreach ($key in $values.Keys) {
    $pattern = '(?m)^' + [regex]::Escape($key) + '=[^\r\n]*'
    $line = $key + '=' + $values[$key]
    if ([regex]::IsMatch($content, $pattern)) { $content = [regex]::Replace($content, $pattern, $line) }
    else { $content = $content.TrimEnd() + "`n" + $line + "`n" }
}
[IO.File]::WriteAllText($environmentFile, $content, [Text.UTF8Encoding]::new($false))
$clientSecret = $null
$clientId = $null
Write-Host 'Saved to the ignored local .env. Apply with:'
Write-Host 'docker compose -f compose.yml -f compose.laptop.yml up -d --no-deps --wait user-service gateway-service'
