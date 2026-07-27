# BUY-01 learning header
# File purpose: Removes product posts and their linked media through authorized APIs.
# Learning focus: Safe scoped cleanup with confirmation, authentication, and recoverable targeting.
[CmdletBinding(SupportsShouldProcess, ConfirmImpact = 'High')]
param(
    [string]$MongoContainer = 'buy01-mongo-1',
    [string]$MinioContainer = 'buy01-minio-1',
    [string]$ProductDatabase = 'buy01_products',
    [string]$MediaDatabase = 'buy01_media',
    [string]$MediaBucket = 'buy01-media'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Invoke-DockerText {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $output = & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }

    return ($output -join "`n").Trim()
}

$mongoUser = Invoke-DockerText @(
    'exec', $MongoContainer, 'printenv', 'MONGO_INITDB_ROOT_USERNAME'
)
$mongoPassword = Invoke-DockerText @(
    'exec', $MongoContainer, 'printenv', 'MONGO_INITDB_ROOT_PASSWORD'
)
$minioUser = Invoke-DockerText @(
    'exec', $MinioContainer, 'printenv', 'MINIO_ROOT_USER'
)
$minioPassword = Invoke-DockerText @(
    'exec', $MinioContainer, 'printenv', 'MINIO_ROOT_PASSWORD'
)

$mongoBaseArguments = @(
    'exec',
    $MongoContainer,
    'mongosh',
    '--quiet',
    '--username',
    $mongoUser,
    '--password',
    $mongoPassword,
    '--authenticationDatabase',
    'admin',
    '--eval'
)

$snapshotQuery = @"
const products = db.getSiblingDB('$ProductDatabase').products;
const media = db.getSiblingDB('$MediaDatabase').media_assets;
print(EJSON.stringify({
  productCount: products.countDocuments({}),
  productImages: media.find(
    { purpose: 'PRODUCT_IMAGE' },
    { _id: 0, objectKey: 1 }
  ).toArray()
}));
"@

$snapshotJson = Invoke-DockerText ($mongoBaseArguments + $snapshotQuery)
$snapshot = $snapshotJson | ConvertFrom-Json
$productCount = [int]$snapshot.productCount
$productImages = @($snapshot.productImages)

Write-Host "Resolved $productCount product post(s) and $($productImages.Count) product image(s)."
Write-Host 'User accounts and avatar images are outside this cleanup scope.'

if ($productCount -eq 0 -and $productImages.Count -eq 0) {
    Write-Host 'The marketplace is already clear.'
    exit 0
}

if (-not $PSCmdlet.ShouldProcess(
    "$productCount product post(s) and $($productImages.Count) product image(s)",
    'Permanently clear marketplace product content'
)) {
    exit 0
}

Invoke-DockerText @(
    'exec',
    $MinioContainer,
    'mc',
    'alias',
    'set',
    'product-cleanup',
    'http://127.0.0.1:9000',
    $minioUser,
    $minioPassword
) | Out-Null

foreach ($image in $productImages) {
    $objectKey = [string]$image.objectKey
    if (
        [string]::IsNullOrWhiteSpace($objectKey) -or
        $objectKey.Contains('..') -or
        $objectKey.StartsWith('/')
    ) {
        throw "Refusing unsafe object key: '$objectKey'"
    }

    Invoke-DockerText @(
        'exec',
        $MinioContainer,
        'mc',
        'rm',
        '--force',
        "product-cleanup/$MediaBucket/$objectKey"
    ) | Out-Null
}

$deleteQuery = @"
const productResult = db.getSiblingDB('$ProductDatabase').products.deleteMany({});
const mediaResult = db.getSiblingDB('$MediaDatabase').media_assets.deleteMany({
  purpose: 'PRODUCT_IMAGE'
});
print(EJSON.stringify({
  deletedProducts: productResult.deletedCount,
  deletedProductImages: mediaResult.deletedCount
}));
"@

$resultJson = Invoke-DockerText ($mongoBaseArguments + $deleteQuery)
$result = $resultJson | ConvertFrom-Json

Write-Host "Deleted $($result.deletedProducts) product post(s)."
Write-Host "Deleted $($result.deletedProductImages) product image record(s)."
Write-Host 'User accounts and avatar images were preserved.'
