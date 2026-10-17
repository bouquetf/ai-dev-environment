param(
    [ValidateSet("all", "html", "pdf")]
    [string]$Target = "all"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Deck = Join-Path $Root "slide-deck.md"
$GeneratedDeck = Join-Path $Root ".slide-deck.generated.md"
$Theme = Join-Path $Root "theme.css"
$PrepareMermaid = Join-Path $Root "prepare-mermaid.mjs"
$Dist = Join-Path $Root "dist"

if (-not (Test-Path $Deck)) {
    throw "slide-deck.md is missing in $Root"
}
if (-not (Test-Path $Theme)) {
    throw "theme.css is missing in $Root"
}
if (-not (Test-Path $PrepareMermaid)) {
    throw "prepare-mermaid.mjs is missing in $Root"
}

New-Item -ItemType Directory -Force -Path $Dist | Out-Null
& node $PrepareMermaid
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $GeneratedDeck)) {
    throw "Mermaid preparation failed."
}

$AssetSource = Join-Path $Root "assets"
$AssetTarget = Join-Path $Dist "assets"
if (Test-Path $AssetSource) {
    if (Test-Path $AssetTarget) {
        Remove-Item -Recurse -Force $AssetTarget
    }
    Copy-Item -Recurse -Force $AssetSource $AssetTarget
}

function Invoke-Marp {
    param([string]$Output)
    & npx --yes "@marp-team/marp-cli@latest" $GeneratedDeck --theme-set $Theme --theme talk --allow-local-files -o $Output
    if ($LASTEXITCODE -ne 0) {
        throw "Marp failed while writing $Output. PDF export needs Chrome, Edge, or Chromium. Set CHROME_PATH to the browser executable if needed."
    }
}

if ($Target -eq "all" -or $Target -eq "html") {
    Invoke-Marp (Join-Path $Dist "slide-deck.html")
}
if ($Target -eq "all" -or $Target -eq "pdf") {
    Invoke-Marp (Join-Path $Dist "slide-deck.pdf")
}
