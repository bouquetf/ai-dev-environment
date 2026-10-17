<#
.SYNOPSIS
    Map git changes to CRBPM modules, related tests, and suggested DooD targets.

.DESCRIPTION
    Helper for the `run-tests-dood` coverage/relevance gate. Prints a structured
    assessment the agent uses before handing off to `doexecute-test-dood`.
    Does not run tests.

.PARAMETER RepoRoot
    Repository root (defaults to git rev-parse --show-toplevel).

.PARAMETER BaseRef
    Optional git ref to diff against (e.g. origin/main, HEAD~1). When omitted,
    uses working tree changes: staged + unstaged + untracked (not ignored).
#>

[CmdletBinding()]
param(
    [string]$RepoRoot = '',
    [string]$BaseRef = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-RepoRoot([string]$explicit) {
    if ($explicit) { return (Resolve-Path $explicit).Path }
    $top = (& git rev-parse --show-toplevel 2>$null)
    if (-not $top) { throw 'Not inside a git repository; pass -RepoRoot.' }
    return $top.Trim()
}

function Get-ChangedPaths([string]$repoRoot, [string]$baseRef) {
    Push-Location $repoRoot
    try {
        $paths = New-Object System.Collections.Generic.HashSet[string]
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            if ($baseRef) {
                & git -c core.quotepath=false diff --name-only --diff-filter=ACMR "$baseRef...HEAD" 2>$null |
                    ForEach-Object { if ($_ -and $_ -notmatch '^warning:') { [void]$paths.Add($_.Replace('\', '/')) } }
                & git -c core.quotepath=false diff --name-only --diff-filter=ACMR "$baseRef" 2>$null |
                    ForEach-Object { if ($_ -and $_ -notmatch '^warning:') { [void]$paths.Add($_.Replace('\', '/')) } }
            }
            & git -c core.quotepath=false diff --name-only --diff-filter=ACMR HEAD 2>$null |
                ForEach-Object { if ($_ -and $_ -notmatch '^warning:') { [void]$paths.Add($_.Replace('\', '/')) } }
            & git -c core.quotepath=false diff --name-only --diff-filter=ACMR --cached 2>$null |
                ForEach-Object { if ($_ -and $_ -notmatch '^warning:') { [void]$paths.Add($_.Replace('\', '/')) } }
            & git -c core.quotepath=false ls-files --others --exclude-standard 2>$null |
                ForEach-Object { if ($_ -and $_ -notmatch '^warning:') { [void]$paths.Add($_.Replace('\', '/')) } }
        }
        finally {
            $ErrorActionPreference = $prevEap
        }
        return @($paths | Sort-Object)
    }
    finally {
        Pop-Location
    }
}

# Ordered rules: first match wins. Meta-targets like e2e/camunda-live are hints.
$pathRules = @(
    @{ Pattern = '^crbpm/connectors/'; Module = 'connectors'; Hints = @('connectors') }
    @{ Pattern = '^crbpm/backend-shared/'; Module = 'backend-shared'; Hints = @('backend-shared', 'backend') }
    @{ Pattern = '^crbpm/backend/'; Module = 'backend'; Hints = @('backend') }
    @{ Pattern = '^crbpm/api/'; Module = 'api'; Hints = @('api') }
    @{ Pattern = '^crbpm/ui/e2e/'; Module = 'ui'; Hints = @('e2e', 'ui') }
    @{ Pattern = '^crbpm/ui/'; Module = 'ui'; Hints = @('ui') }
    @{ Pattern = '^crbpm/engine-c8/'; Module = 'engine-c8'; Hints = @('engine-c8', 'backend', 'camunda-live') }
    @{ Pattern = '^crbpm/expression-c8/'; Module = 'expression-c8'; Hints = @('expression-c8', 'backend') }
    @{ Pattern = '^crbpm/expression/'; Module = 'expression'; Hints = @('expression') }
    @{ Pattern = '^crbpm/engine-spi/'; Module = 'engine-spi'; Hints = @('engine-spi', 'backend') }
    @{ Pattern = '^crbpm/engine-stub/'; Module = 'engine-stub'; Hints = @('backend') }
    @{ Pattern = '^crbpm/camunda-adapter/'; Module = 'camunda-adapter'; Hints = @('camunda-adapter', 'camunda-live') }
    @{ Pattern = '^crbpm/infra/'; Module = 'infra'; Hints = @('camunda-live') }
    @{ Pattern = '^crbpm-connectors/'; Module = 'crbpm-connectors'; Hints = @('contrib', 'connectors') }
    @{ Pattern = '^docs/testplan/'; Module = 'docs'; Hints = @() }
    @{ Pattern = '^docs/'; Module = 'docs'; Hints = @() }
    @{ Pattern = '^\.cursor/skills/run-tests-dood/'; Module = 'skill-dood'; Hints = @() }
    @{ Pattern = '^\.cursor/skills/doexecute-test-dood/'; Module = 'skill-dood'; Hints = @() }
)

function Resolve-PathMapping([string]$relPath) {
    foreach ($rule in $pathRules) {
        if ($relPath -match $rule.Pattern) {
            return $rule
        }
    }
    return @{ Pattern = ''; Module = 'other'; Hints = @() }
}

function Find-RelatedTests([string]$repoRoot, [string]$module, [string[]]$changedPaths) {
    $related = New-Object System.Collections.Generic.List[string]
    $testRoots = @(
        "crbpm/$module/src/test",
        "crbpm/$module/src/c8Test",
        "crbpm/$module/src/stubTest"
    )
    if ($module -eq 'ui') {
        $testRoots += @('crbpm/ui/src', 'crbpm/ui/e2e')
    }
    if ($module -eq 'crbpm-connectors') {
        $testRoots = @()
        $contribModulesRoot = Join-Path $repoRoot 'crbpm-connectors/modules'
        if (Test-Path $contribModulesRoot) {
            $testRoots = @(Get-ChildItem -Path $contribModulesRoot -Directory |
                ForEach-Object { "crbpm-connectors/modules/$($_.Name)/src/test" })
        }
    }

    $stems = @()
    foreach ($p in $changedPaths) {
        $leaf = [System.IO.Path]::GetFileNameWithoutExtension($p)
        if ($leaf -match '^(.*?)(Resource|Service|Client|Worker|Executor|Handler|Mapper|Filter|Validator)?$') {
            $stems += $Matches[1]
            $stems += $leaf
        }
    }
    $stems = @($stems | Where-Object { $_ -and $_.Length -ge 3 } | Select-Object -Unique)

    foreach ($root in $testRoots) {
        $abs = Join-Path $repoRoot $root
        if (-not (Test-Path $abs)) { continue }
        Get-ChildItem -Path $abs -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Name -match '\.(java|js|jsx|ts|tsx)$' -and (
                    $_.Name -match '(Test|IT|spec)\.' -or
                    $_.FullName -match '[\\/]e2e[\\/]' -or
                    $_.FullName -match '\.test\.(js|jsx)$'
                )
            } |
            ForEach-Object {
                $rel = $_.FullName.Substring($repoRoot.Length).TrimStart('\', '/').Replace('\', '/')
                $name = $_.Name
                foreach ($stem in $stems) {
                    if ($stem.Length -ge 4 -and ($name.Contains($stem) -or $rel.Contains($stem))) {
                        $related.Add($rel)
                        break
                    }
                }
            }
    }
    return ,@($related | Select-Object -Unique | Sort-Object | Select-Object -First 40)
}

$repoRoot = Resolve-RepoRoot $RepoRoot
$changed = Get-ChangedPaths $repoRoot $BaseRef

Write-Host '=== run-tests-dood coverage gate: change -> module map ==='
Write-Host "RepoRoot: $repoRoot"
if ($BaseRef) { Write-Host "BaseRef: $BaseRef" } else { Write-Host 'Scope: working tree (staged + unstaged + untracked)' }
Write-Host "Changed files: $($changed.Count)"
Write-Host ''

if ($changed.Count -eq 0) {
    Write-Host 'No local file changes detected.'
    Write-Host 'SuggestedTargets: full,camunda-live,e2e'
    Write-Host 'Note: without a diff, the agent must rely on conversation feature scope only.'
    exit 0
}

$byModule = @{}
$allHints = New-Object System.Collections.Generic.HashSet[string]

foreach ($path in $changed) {
    $mapping = Resolve-PathMapping $path
    $module = $mapping.Module
    if (-not $byModule.ContainsKey($module)) {
        $byModule[$module] = @{ Paths = New-Object System.Collections.Generic.List[string]; Hints = $mapping.Hints }
    }
    $byModule[$module].Paths.Add($path)
    foreach ($h in $mapping.Hints) { [void]$allHints.Add($h) }
}

Write-Host '## Modules touched'
foreach ($module in ($byModule.Keys | Sort-Object)) {
    $entry = $byModule[$module]
    Write-Host ""
    Write-Host "### $module ($($entry.Paths.Count) file(s))"
    foreach ($p in $entry.Paths) { Write-Host "  - $p" }
    if ($module -match '^(docs|skill-dood|other)$') {
        Write-Host '  RelatedTests: (n/a for this path class)'
        continue
    }
    $related = @(Find-RelatedTests $repoRoot $module @($entry.Paths))
    if ($related.Count -eq 0) {
        Write-Host '  RelatedTests: NONE matched by name heuristic - agent must search manually'
    }
    else {
        Write-Host '  RelatedTests:'
        foreach ($t in $related) { Write-Host "    - $t" }
    }
}

$suggested = @(@($allHints) | Where-Object { $_ -and $_ -ne 'docs' } | Sort-Object -Unique)
if ($suggested.Count -eq 0) {
    $suggested = @('full', 'camunda-live', 'e2e')
}

Write-Host ''
Write-Host '## Suggested DooD targets (heuristic)'
$suggestedCsv = ($suggested -join ',')
Write-Host "SuggestedTargets: $suggestedCsv"
Write-Host 'DefaultFullStillValid: yes (regression gate); prefer SuggestedTargets for focused feature loops after agent completeness check.'
Write-Host ''
Write-Host '## Agent next steps'
Write-Host '1. Cross-check conversation features / story acceptance criteria against RelatedTests.'
Write-Host '2. Flag completeness gaps (changed production code with no test, missing IT/e2e for user-facing flows).'
Write-Host '3. Confirm targets with the user when narrowing below full, or when gaps require new tests before running.'
