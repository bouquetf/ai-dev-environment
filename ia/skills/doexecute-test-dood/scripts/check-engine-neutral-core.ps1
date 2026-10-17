# Fails when core modules contain disallowed Camunda/Zeebe references.
# Allowed: historical Flyway scripts under db/migration; forbidden vendor URI fragments in BpmnVendorExtensionValidator.
param(
    [Parameter(Mandatory = $true)]
    [string]$CrbpmRoot
)

$ErrorActionPreference = 'Stop'

$scanRoots = @(
    (Join-Path $CrbpmRoot 'backend\src\main')
    (Join-Path $CrbpmRoot 'backend-shared\src\main')
    (Join-Path $CrbpmRoot 'rbac-shared\src\main')
    (Join-Path $CrbpmRoot 'engine-spi\src\main')
)
$apiRoot = Join-Path $CrbpmRoot 'api\src\main'
$pattern = 'camunda|zeebe'

function Get-NeutralCoreHits {
    param([string[]]$Roots)

    $hits = @()
    foreach ($root in $Roots) {
        if (-not (Test-Path $root)) {
            continue
        }
        $files = Get-ChildItem -Path $root -Recurse -File |
            Where-Object { $_.FullName -notmatch '[\\/]db[\\/]migration[\\/]' }
        foreach ($file in $files) {
            $lineNumber = 0
            foreach ($line in [System.IO.File]::ReadLines($file.FullName)) {
                $lineNumber++
                if ($line -match $pattern) {
                    $relativePath = $file.FullName.Substring($CrbpmRoot.Length + 1)
                    $hits += "${relativePath}:${lineNumber}:${line}"
                }
            }
        }
    }
    return $hits
}

function Test-AllowlistedHit {
    param([string]$HitLine)

    return $HitLine -match 'BpmnVendorExtensionValidator\.java:\d+:.*"(camunda\.org|zeebe\.io)"'
}

$scanHits = Get-NeutralCoreHits -Roots $scanRoots
$violations = @($scanHits | Where-Object { -not (Test-AllowlistedHit $_) })

if (@($violations).Count -gt 0) {
    Write-Host 'Engine-neutral core check failed. Disallowed camunda/zeebe references:' -ForegroundColor Red
    $violations | ForEach-Object { Write-Host $_ }
    exit 1
}

$apiHits = @(Get-NeutralCoreHits -Roots @($apiRoot))
if ($apiHits.Count -gt 0) {
    Write-Host 'API module must stay engine-neutral. Disallowed references:' -ForegroundColor Red
    $apiHits | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host 'Engine-neutral core check passed.'
