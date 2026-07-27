# File purpose: Exercises the complete deployed marketplace and cleans temporary data.
param(
    [Parameter(Mandatory = $true)]
    [string]$ImagePath,

    [string]$ApiBase = 'http://localhost:4200/api'
)

$ErrorActionPreference = 'Stop'

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "Smoke test assertion failed: $Message"
    }
}

function Invoke-ExpectedFailure {
    param(
        [scriptblock]$Request,
        [int]$ExpectedStatus,
        [string]$Description
    )

    $actualStatus = 200
    try {
        & $Request | Out-Null
    } catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }
        $actualStatus = [int]$_.Exception.Response.StatusCode
    }

    Assert-True ($actualStatus -eq $ExpectedStatus) `
        "$Description returned HTTP $actualStatus instead of $ExpectedStatus"
}

function Invoke-WithRetry {
    param(
        [scriptblock]$Request,
        [int]$Attempts = 12,
        [int]$DelaySeconds = 5
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        try {
            return & $Request
        } catch {
            if ($attempt -eq $Attempts) {
                throw
            }
            Start-Sleep -Seconds $DelaySeconds
        }
    }
}

$resolvedImage = (Resolve-Path -LiteralPath $ImagePath).Path
$projectRoot = Split-Path -Parent $PSScriptRoot
$invalidImage = Join-Path $projectRoot 'pom.xml'
$headers = @{}
$productId = $null
$mediaId = $null
$avatarMediaIds = @()
$originalProfile = $null

try {
    $ui = Invoke-WebRequest -Uri ($ApiBase -replace '/api$', '/') -UseBasicParsing
    Assert-True ($ui.StatusCode -eq 200) 'the production UI is not reachable'

    $sellerLogin = Invoke-WithRetry -Request {
        Invoke-RestMethod `
            -Uri "$ApiBase/auth/login" `
            -Method Post `
            -ContentType 'application/json' `
            -Body (@{
                email = 'seller@buy01.local'
                password = 'Seller123!'
            } | ConvertTo-Json)
    }
    $sellerToken = $sellerLogin.accessToken
    Assert-True (-not [string]::IsNullOrWhiteSpace($sellerToken)) `
        'seller login did not return a token'
    $headers = @{ Authorization = "Bearer $sellerToken" }

    $profile = Invoke-RestMethod -Uri "$ApiBase/me" -Headers $headers
    $originalProfile = $profile
    Assert-True ($profile.role -eq 'SELLER') 'seller profile has the wrong role'

    $firstAvatarJson = & curl.exe `
        --silent `
        --show-error `
        --fail-with-body `
        --request POST `
        "$ApiBase/media/images" `
        --header "Authorization: Bearer $sellerToken" `
        --form "file=@$resolvedImage;type=image/png" `
        --form 'purpose=AVATAR'
    Assert-True ($LASTEXITCODE -eq 0) 'seller avatar creation failed'
    $firstAvatar = $firstAvatarJson | ConvertFrom-Json
    $avatarMediaIds += $firstAvatar.id

    $profileWithAvatar = Invoke-RestMethod `
        -Uri "$ApiBase/me" `
        -Method Put `
        -Headers $headers `
        -ContentType 'application/json' `
        -Body (@{
            name = $profile.name
            avatarUrl = $firstAvatar.url
        } | ConvertTo-Json)
    Assert-True ($profileWithAvatar.avatarUrl -eq $firstAvatar.url) `
        'seller profile did not retain the created avatar'

    $replacementAvatarJson = & curl.exe `
        --silent `
        --show-error `
        --fail-with-body `
        --request POST `
        "$ApiBase/media/images" `
        --header "Authorization: Bearer $sellerToken" `
        --form "file=@$resolvedImage;type=image/png" `
        --form 'purpose=AVATAR'
    Assert-True ($LASTEXITCODE -eq 0) 'seller avatar replacement upload failed'
    $replacementAvatar = $replacementAvatarJson | ConvertFrom-Json
    $avatarMediaIds += $replacementAvatar.id

    $profileWithReplacement = Invoke-RestMethod `
        -Uri "$ApiBase/me" `
        -Method Put `
        -Headers $headers `
        -ContentType 'application/json' `
        -Body (@{
            name = $profile.name
            avatarUrl = $replacementAvatar.url
        } | ConvertTo-Json)
    Assert-True ($profileWithReplacement.avatarUrl -eq $replacementAvatar.url) `
        'seller profile did not retain the replacement avatar'
    Assert-True ($profileWithReplacement.avatarUrl -ne $firstAvatar.url) `
        'seller avatar URL did not change after replacement'

    $clientLogin = Invoke-RestMethod `
        -Uri "$ApiBase/auth/login" `
        -Method Post `
        -ContentType 'application/json' `
        -Body (@{
            email = 'client@buy01.local'
            password = 'Client123!'
        } | ConvertTo-Json)
    $clientHeaders = @{ Authorization = "Bearer $($clientLogin.accessToken)" }

    $adminLogin = Invoke-RestMethod `
        -Uri "$ApiBase/auth/login" `
        -Method Post `
        -ContentType 'application/json' `
        -Body (@{
            email = 'admin@buy01.local'
            password = 'Admin123!'
        } | ConvertTo-Json)
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.accessToken)" }
    $accounts = @(Invoke-RestMethod -Uri "$ApiBase/admin/users" -Headers $adminHeaders)
    Assert-True (@($accounts.role) -contains 'ADMIN') `
        'admin account list did not include an ADMIN identity'
    @(Invoke-RestMethod `
        -Uri "$ApiBase/products/moderation" `
        -Headers $adminHeaders) | Out-Null
    @(Invoke-RestMethod `
        -Uri "$ApiBase/media/images/moderation" `
        -Headers $adminHeaders) | Out-Null

    Invoke-ExpectedFailure `
        -ExpectedStatus 403 `
        -Description 'seller access to admin accounts' `
        -Request {
            Invoke-RestMethod -Uri "$ApiBase/admin/users" -Headers $headers
        }

    $productRequest = @{
        name = 'BUY-01 Smoke-Test Camera'
        description = 'Temporary product created by the automated end-to-end smoke test.'
        price = 149.90
        quantity = 3
        imageUrls = @()
    }

    Invoke-ExpectedFailure `
        -ExpectedStatus 401 `
        -Description 'anonymous product creation' `
        -Request {
            Invoke-RestMethod `
                -Uri "$ApiBase/products" `
                -Method Post `
                -ContentType 'application/json' `
                -Body ($productRequest | ConvertTo-Json)
        }

    Invoke-ExpectedFailure `
        -ExpectedStatus 403 `
        -Description 'client product creation' `
        -Request {
            Invoke-RestMethod `
                -Uri "$ApiBase/products" `
                -Method Post `
                -Headers $clientHeaders `
                -ContentType 'application/json' `
                -Body ($productRequest | ConvertTo-Json)
        }

    $product = Invoke-RestMethod `
        -Uri "$ApiBase/products" `
        -Method Post `
        -Headers $headers `
        -ContentType 'application/json' `
        -Body ($productRequest | ConvertTo-Json)
    $productId = $product.id
    Assert-True (-not [string]::IsNullOrWhiteSpace($productId)) `
        'product creation did not return an ID'

    $invalidStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out '%{http_code}' `
        --request POST `
        "$ApiBase/media/images" `
        --header "Authorization: Bearer $sellerToken" `
        --form "file=@$invalidImage;type=image/png" `
        --form 'purpose=PRODUCT_IMAGE' `
        --form "productId=$productId"
    Assert-True ($invalidStatus -eq '400') `
        "a fake PNG was not rejected (HTTP $invalidStatus)"

    $uploadJson = & curl.exe `
        --silent `
        --show-error `
        --fail-with-body `
        --request POST `
        "$ApiBase/media/images" `
        --header "Authorization: Bearer $sellerToken" `
        --form "file=@$resolvedImage;type=image/png" `
        --form 'purpose=PRODUCT_IMAGE' `
        --form "productId=$productId"
    Assert-True ($LASTEXITCODE -eq 0) 'valid image upload failed'
    $media = $uploadJson | ConvertFrom-Json
    $mediaId = $media.id
    Assert-True ($media.contentType -eq 'image/png') 'uploaded media has the wrong MIME type'

    $productRequest.imageUrls = @($media.url)
    $updated = Invoke-RestMethod `
        -Uri "$ApiBase/products/$productId" `
        -Method Put `
        -Headers $headers `
        -ContentType 'application/json' `
        -Body ($productRequest | ConvertTo-Json)
    Assert-True ($updated.imageUrls[0] -eq $media.url) `
        'product did not retain the uploaded image URL'

    $catalog = @(Invoke-RestMethod -Uri "$ApiBase/products")
    Assert-True (@($catalog.id) -contains $productId) `
        "product $productId is missing from the public catalog; found $($catalog.id -join ', ')"

    $mine = @(Invoke-RestMethod -Uri "$ApiBase/products/mine" -Headers $headers)
    Assert-True (@($mine.id) -contains $productId) `
        "product $productId is missing from seller inventory; found $($mine.id -join ', ')"

    $download = Invoke-WebRequest -Uri $media.url -UseBasicParsing
    Assert-True ($download.StatusCode -eq 200) 'public image download failed'
    Assert-True ($download.Headers['Content-Type'] -like 'image/png*') `
        'public image download has the wrong Content-Type'
    Assert-True ($download.Headers['Cache-Control'] -like '*immutable*') `
        'public image download is missing immutable caching'

    Write-Host 'PASS: UI, authentication, roles, admin moderation boundaries,'
    Write-Host '      seller avatar create/replace,'
    Write-Host '      product CRUD, media validation, object storage, public catalog,'
    Write-Host '      and image caching all work.'
} finally {
    if ($null -ne $originalProfile -and $headers.Count -gt 0) {
        try {
            Invoke-RestMethod `
                -Uri "$ApiBase/me" `
                -Method Put `
                -Headers $headers `
                -ContentType 'application/json' `
                -Body (@{
                    name = $originalProfile.name
                    avatarUrl = $originalProfile.avatarUrl
                } | ConvertTo-Json) | Out-Null
        } catch {
            Write-Warning 'Could not restore the original seller profile'
        }
    }

    foreach ($avatarMediaId in $avatarMediaIds) {
        try {
            Invoke-RestMethod `
                -Uri "$ApiBase/media/images/$avatarMediaId" `
                -Method Delete `
                -Headers $headers | Out-Null
        } catch {
            Write-Warning "Could not remove smoke-test avatar $avatarMediaId"
        }
    }

    if ($null -ne $mediaId) {
        try {
            Invoke-RestMethod `
                -Uri "$ApiBase/media/images/$mediaId" `
                -Method Delete `
                -Headers $headers | Out-Null
        } catch {
            Write-Warning "Could not remove smoke-test media $mediaId"
        }
    }

    if ($null -ne $productId) {
        try {
            Invoke-RestMethod `
                -Uri "$ApiBase/products/$productId" `
                -Method Delete `
                -Headers $headers | Out-Null
        } catch {
            Write-Warning "Could not remove smoke-test product $productId"
        }
    }
}
