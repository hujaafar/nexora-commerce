# File purpose: Saves a Gmail App Password locally without echoing or committing it.
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[^@\s]+@gmail\.com$')]
    [string]$Email
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $projectRoot 'jenkins\.env'

if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw 'Run scripts\jenkins-start.ps1 once before configuring Gmail.'
}

function Set-DotEnvValue {
    param(
        [string]$Content,
        [string]$Name,
        [string]$Value
    )

    $line = "$Name=$Value"
    if ($Content -match "(?m)^$([regex]::Escape($Name))=") {
        return [regex]::Replace(
            $Content,
            "(?m)^$([regex]::Escape($Name))=.*$",
            $line
        )
    }

    return "$($Content.TrimEnd())`r`n$line`r`n"
}

Write-Host ''
Write-Host 'Paste the 16-character Google App Password.' -ForegroundColor Cyan
Write-Host 'The value is hidden, saved only in ignored jenkins/.env, and never printed.'
$securePassword = Read-Host 'Google App Password' -AsSecureString

$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $appPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}

# Google displays the password in groups. Remove spaces before validating and
# storing it because SMTP expects the continuous 16-character value.
$appPassword = $appPassword -replace '\s', ''
if ($appPassword -notmatch '^[A-Za-z0-9]{16}$') {
    throw 'The App Password must contain exactly 16 letters or digits.'
}

$content = Get-Content -LiteralPath $environmentFile -Raw
$content = Set-DotEnvValue $content 'JENKINS_ADMIN_EMAIL' $Email
$content = Set-DotEnvValue $content 'NOTIFICATION_EMAIL' $Email
$content = Set-DotEnvValue $content 'SMTP_HOST' 'smtp.gmail.com'
$content = Set-DotEnvValue $content 'SMTP_PORT' '587'
$content = Set-DotEnvValue $content 'SMTP_USERNAME' $Email
$content = Set-DotEnvValue $content 'SMTP_APP_PASSWORD' $appPassword
$content = Set-DotEnvValue $content 'SMTP_CREDENTIALS_ID' 'notification-smtp'
$content = Set-DotEnvValue $content 'SMTP_USE_TLS' 'true'

[IO.File]::WriteAllText(
    $environmentFile,
    $content,
    [Text.UTF8Encoding]::new($false)
)

# AVG Mail Shield proxies SMTP TLS with a public root trusted by Windows. Docker
# uses its own Linux trust store, so copy only that public certificate into an
# ignored mount when AVG is present. No private key or account secret is read.
$certificateStores = @(
    'Cert:\CurrentUser\Root',
    'Cert:\LocalMachine\Root'
)
$avgMailShieldCertificate = $certificateStores |
    ForEach-Object {
        Get-ChildItem -Path $_ -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Subject -like '*CN=AVG Web/Mail Shield Root*' -and
                -not $_.HasPrivateKey
            }
    } |
    Sort-Object -Property NotAfter -Descending |
    Select-Object -First 1

if ($null -ne $avgMailShieldCertificate) {
    $localCaDirectory = Join-Path $projectRoot 'jenkins\certs'
    $localCaFile = Join-Path $localCaDirectory 'avg-web-mail-shield-root.crt'
    [void](New-Item -ItemType Directory -Path $localCaDirectory -Force)
    $encodedCertificate = [Convert]::ToBase64String(
        $avgMailShieldCertificate.RawData,
        [Base64FormattingOptions]::InsertLineBreaks
    ) -replace "`r`n", "`n"
    $pemCertificate = "-----BEGIN CERTIFICATE-----`n$encodedCertificate`n-----END CERTIFICATE-----`n"
    [IO.File]::WriteAllText(
        $localCaFile,
        $pemCertificate,
        [Text.UTF8Encoding]::new($false)
    )
    Write-Host 'AVG Mail Shield public root prepared for the Jenkins agent.' -ForegroundColor Green
}

# Clear the remaining managed-string reference as soon as the file is written.
$appPassword = $null
[GC]::Collect()

Write-Host ''
Write-Host 'Gmail SMTP configuration saved securely.' -ForegroundColor Green
Write-Host 'Return to Codex; Jenkins will be restarted and tested for you.'
