# Export only the already-trusted public AVG root for the isolated CI containers.
# This does not read a private key or change the Windows trust store.
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
$certificate = Get-ChildItem Cert:\CurrentUser\Root,Cert:\LocalMachine\Root |
    Where-Object { $_.Subject -like '*CN=AVG Web/Mail Shield Root*' } |
    Sort-Object NotAfter -Descending | Select-Object -First 1
if ($null -ne $certificate) {
    $directory = Join-Path $repository 'jenkins/certs'
    [void](New-Item -ItemType Directory -Force -Path $directory)
    $encoded = [Convert]::ToBase64String($certificate.RawData, [Base64FormattingOptions]::InsertLineBreaks)
    [IO.File]::WriteAllText((Join-Path $directory 'avg-web-mail-shield-root.crt'),
        "-----BEGIN CERTIFICATE-----`n$encoded`n-----END CERTIFICATE-----`n", [Text.UTF8Encoding]::new($false))
    Write-Host 'Prepared the Windows-trusted public AVG root for isolated CI containers.'
}
