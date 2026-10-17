<#
.SYNOPSIS
    Generates a self-contained HTML report from a JUnit XML file.

.DESCRIPTION
    Used by the doexecute-test-dood skill to produce a static, file:// openable
    report for the `ui` module (Vitest JUnit output). No HTTP server required,
    no external assets.

.PARAMETER JunitFile
    Path to a JUnit XML file (Vitest / Gradle compatible <testsuites>/<testsuite>/<testcase> tree).

.PARAMETER OutputFile
    Path where the standalone HTML report will be written.

.PARAMETER Title
    Optional page title (default: "Test report").
#>
param(
    [Parameter(Mandatory = $true)][string]$JunitFile,
    [Parameter(Mandatory = $true)][string]$OutputFile,
    [string]$Title = 'Test report'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $JunitFile)) {
    throw "JUnit file not found: $JunitFile"
}

function Html-Escape([string]$value) {
    if ($null -eq $value -or $value -eq '') { return '' }
    return $value.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;').Replace('"', '&quot;')
}

function Format-Duration([double]$seconds) {
    if ($seconds -ge 1) { return ('{0:N2} s' -f $seconds) }
    return ('{0} ms' -f [int]([math]::Round($seconds * 1000)))
}

function Get-IntAttr($node, [string]$name, [int]$default = 0) {
    if ($null -eq $node -or $null -eq $node.Attributes) { return $default }
    $a = $node.Attributes[$name]
    if ($null -eq $a -or [string]::IsNullOrWhiteSpace($a.Value)) { return $default }
    return [int]$a.Value
}

function Get-DoubleAttr($node, [string]$name, [double]$default = 0.0) {
    if ($null -eq $node -or $null -eq $node.Attributes) { return $default }
    $a = $node.Attributes[$name]
    if ($null -eq $a -or [string]::IsNullOrWhiteSpace($a.Value)) { return $default }
    return [double]([System.Globalization.CultureInfo]::InvariantCulture | ForEach-Object {
        [double]::Parse($a.Value, [System.Globalization.CultureInfo]::InvariantCulture)
    })
}

[xml]$doc = Get-Content -Raw -Path $JunitFile

# Some JUnit files use a top-level <testsuite> directly (single suite). Normalize.
$suiteNodes = @()
if ($doc.testsuites) {
    foreach ($s in $doc.testsuites.testsuite) { $suiteNodes += $s }
    $rootNode = $doc.testsuites
} elseif ($doc.testsuite) {
    $suiteNodes += $doc.testsuite
    $rootNode = $doc.testsuite
} else {
    throw "Unrecognized JUnit XML root in $JunitFile"
}

$rootTests    = Get-IntAttr $rootNode 'tests'
$rootFailures = Get-IntAttr $rootNode 'failures'
$rootErrors   = Get-IntAttr $rootNode 'errors'
$rootSkipped  = Get-IntAttr $rootNode 'skipped'
$rootTime     = Get-DoubleAttr $rootNode 'time'

# Aggregate from suites when root attributes are missing (e.g. Gradle <testsuite>).
if ($rootTests -eq 0) {
    foreach ($s in $suiteNodes) {
        $rootTests    += Get-IntAttr $s 'tests'
        $rootFailures += Get-IntAttr $s 'failures'
        $rootErrors   += Get-IntAttr $s 'errors'
        $rootSkipped  += Get-IntAttr $s 'skipped'
        $rootTime     += Get-DoubleAttr $s 'time'
    }
}

$rootPassed = $rootTests - $rootFailures - $rootErrors - $rootSkipped
if ($rootPassed -lt 0) { $rootPassed = 0 }

$statusClass = if ($rootFailures + $rootErrors -gt 0) { 'fail' } else { 'pass' }
$generatedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')

$css = @'
:root { color-scheme: light dark; }
* { box-sizing: border-box; }
body { margin: 0; font: 14px/1.45 -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #fafafa; color: #1f2328; }
@media (prefers-color-scheme: dark) { body { background: #0d1117; color: #e6edf3; } }
header { padding: 16px 24px; border-bottom: 1px solid rgba(127,127,127,0.25); display: flex; flex-wrap: wrap; align-items: baseline; gap: 16px; }
header h1 { margin: 0; font-size: 18px; }
.summary { display: flex; flex-wrap: wrap; gap: 10px; font-size: 13px; }
.summary span { padding: 2px 8px; border-radius: 999px; background: rgba(127,127,127,0.15); }
.summary .pass { background: rgba(46,160,67,0.18); color: #1a7f37; }
.summary .fail { background: rgba(207,34,46,0.18); color: #cf222e; }
.summary .skip { background: rgba(154,103,0,0.18); color: #9a6700; }
.controls { padding: 12px 24px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; border-bottom: 1px solid rgba(127,127,127,0.25); }
.controls input[type=search] { flex: 1; min-width: 200px; padding: 6px 10px; border-radius: 6px; border: 1px solid rgba(127,127,127,0.4); background: transparent; color: inherit; }
.controls button { padding: 6px 10px; border-radius: 6px; border: 1px solid rgba(127,127,127,0.4); background: transparent; color: inherit; cursor: pointer; }
.controls button.active { background: rgba(127,127,127,0.2); }
main { padding: 16px 24px 80px; }
details.suite { margin: 0 0 12px; border: 1px solid rgba(127,127,127,0.25); border-radius: 8px; background: rgba(255,255,255,0.6); }
@media (prefers-color-scheme: dark) { details.suite { background: rgba(255,255,255,0.04); } }
details.suite > summary { cursor: pointer; padding: 10px 14px; list-style: none; display: flex; gap: 10px; align-items: center; font-weight: 600; }
details.suite > summary::-webkit-details-marker { display: none; }
details.suite > summary::before { content: '\25B6'; font-size: 10px; opacity: 0.6; transition: transform .15s ease; }
details.suite[open] > summary::before { transform: rotate(90deg); }
.suite-stats { margin-left: auto; font-weight: 400; font-size: 12px; opacity: 0.8; }
.tests { list-style: none; margin: 0; padding: 0 14px 12px; }
.tests li { padding: 6px 0; border-top: 1px solid rgba(127,127,127,0.12); display: flex; flex-wrap: wrap; gap: 10px; align-items: baseline; }
.tests li:first-child { border-top: none; }
.tests .name { flex: 1; }
.tests .time { font-variant-numeric: tabular-nums; opacity: 0.7; font-size: 12px; }
.tests .status { font-size: 12px; padding: 1px 8px; border-radius: 999px; }
.tests .status.pass { background: rgba(46,160,67,0.18); color: #1a7f37; }
.tests .status.fail { background: rgba(207,34,46,0.18); color: #cf222e; }
.tests .status.skip { background: rgba(154,103,0,0.18); color: #9a6700; }
.failure { width: 100%; margin: 6px 0 4px; padding: 10px 12px; border-radius: 6px; background: rgba(207,34,46,0.08); border-left: 3px solid #cf222e; overflow: auto; }
.failure pre { margin: 4px 0 0; white-space: pre-wrap; word-break: break-word; font-size: 12px; }
footer { padding: 16px 24px; font-size: 12px; opacity: 0.6; }
.hidden { display: none !important; }
'@

$js = @'
(function () {
  const search = document.getElementById('search');
  const buttons = document.querySelectorAll('.controls button[data-filter]');
  let activeFilter = 'all';

  function applyFilters() {
    const term = (search.value || '').toLowerCase();
    document.querySelectorAll('details.suite').forEach((suite) => {
      let suiteVisible = 0;
      suite.querySelectorAll('.tests > li').forEach((li) => {
        const matchesFilter = activeFilter === 'all'
          || li.dataset.status === activeFilter
          || (activeFilter === 'failed' && (li.dataset.status === 'fail' || li.dataset.status === 'error'));
        const matchesSearch = term === '' || li.dataset.search.indexOf(term) !== -1;
        const visible = matchesFilter && matchesSearch;
        li.classList.toggle('hidden', !visible);
        if (visible) suiteVisible += 1;
      });
      suite.classList.toggle('hidden', suiteVisible === 0);
      if (activeFilter === 'failed' && suiteVisible > 0) suite.open = true;
    });
  }

  search.addEventListener('input', applyFilters);
  buttons.forEach((btn) => {
    btn.addEventListener('click', () => {
      buttons.forEach((b) => b.classList.toggle('active', b === btn));
      activeFilter = btn.dataset.filter;
      applyFilters();
    });
  });

  document.getElementById('expand-all').addEventListener('click', () => {
    document.querySelectorAll('details.suite').forEach((d) => { d.open = true; });
  });
  document.getElementById('collapse-all').addEventListener('click', () => {
    document.querySelectorAll('details.suite').forEach((d) => { d.open = false; });
  });
})();
'@

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine('<!doctype html>')
[void]$sb.AppendLine('<html lang="en"><head><meta charset="utf-8">')
[void]$sb.AppendLine(('<title>{0}</title>' -f (Html-Escape $Title)))
[void]$sb.AppendLine('<style>')
[void]$sb.AppendLine($css)
[void]$sb.AppendLine('</style></head><body>')

[void]$sb.AppendLine('<header>')
[void]$sb.AppendLine(('<h1>{0}</h1>' -f (Html-Escape $Title)))
[void]$sb.AppendLine('<div class="summary">')
[void]$sb.AppendLine(('<span class="{0}">{1} tests</span>' -f $statusClass, $rootTests))
[void]$sb.AppendLine(('<span class="pass">{0} passed</span>' -f $rootPassed))
if ($rootFailures -gt 0) {
    [void]$sb.AppendLine(('<span class="fail">{0} failed</span>' -f $rootFailures))
}
if ($rootErrors -gt 0) {
    [void]$sb.AppendLine(('<span class="fail">{0} errors</span>' -f $rootErrors))
}
if ($rootSkipped -gt 0) {
    [void]$sb.AppendLine(('<span class="skip">{0} skipped</span>' -f $rootSkipped))
}
[void]$sb.AppendLine(('<span>{0}</span>' -f (Format-Duration $rootTime)))
[void]$sb.AppendLine('</div></header>')

[void]$sb.AppendLine('<div class="controls">')
[void]$sb.AppendLine('<input id="search" type="search" placeholder="Filter by test or suite name..." autofocus>')
[void]$sb.AppendLine('<button type="button" data-filter="all" class="active">All</button>')
[void]$sb.AppendLine('<button type="button" data-filter="failed">Failures</button>')
[void]$sb.AppendLine('<button type="button" data-filter="skip">Skipped</button>')
[void]$sb.AppendLine('<button type="button" id="expand-all">Expand all</button>')
[void]$sb.AppendLine('<button type="button" id="collapse-all">Collapse all</button>')
[void]$sb.AppendLine('</div>')

[void]$sb.AppendLine('<main>')

foreach ($suite in $suiteNodes) {
    $suiteName = if ($suite.Attributes['name']) { $suite.Attributes['name'].Value } else { '(suite)' }
    $suiteTests    = Get-IntAttr $suite 'tests'
    $suiteFailures = Get-IntAttr $suite 'failures'
    $suiteErrors   = Get-IntAttr $suite 'errors'
    $suiteSkipped  = Get-IntAttr $suite 'skipped'
    $suiteTime     = Get-DoubleAttr $suite 'time'
    $suiteOpen     = ($suiteFailures + $suiteErrors) -gt 0

    $openAttr = if ($suiteOpen) { ' open' } else { '' }
    [void]$sb.AppendLine(('<details class="suite"{0}>' -f $openAttr))
    [void]$sb.Append('<summary>')
    [void]$sb.Append((Html-Escape $suiteName))
    [void]$sb.Append('<span class="suite-stats">')
    [void]$sb.Append(('{0} tests' -f $suiteTests))
    if ($suiteFailures -gt 0) { [void]$sb.Append((' &middot; {0} failed' -f $suiteFailures)) }
    if ($suiteErrors -gt 0) { [void]$sb.Append((' &middot; {0} errors' -f $suiteErrors)) }
    if ($suiteSkipped -gt 0) { [void]$sb.Append((' &middot; {0} skipped' -f $suiteSkipped)) }
    [void]$sb.Append((' &middot; {0}' -f (Format-Duration $suiteTime)))
    [void]$sb.AppendLine('</span></summary>')

    [void]$sb.AppendLine('<ul class="tests">')
    foreach ($case in $suite.testcase) {
        $caseName = if ($case.Attributes['name']) { $case.Attributes['name'].Value } else { '(unnamed)' }
        $caseClass = if ($case.Attributes['classname']) { $case.Attributes['classname'].Value } else { '' }
        $caseTime = Get-DoubleAttr $case 'time'

        $failureNode = $case.SelectSingleNode('failure')
        $errorNode = $case.SelectSingleNode('error')
        $skippedNode = $case.SelectSingleNode('skipped')

        $status = 'pass'
        $statusLabel = 'pass'
        if ($failureNode) { $status = 'fail'; $statusLabel = 'fail' }
        elseif ($errorNode) { $status = 'error'; $statusLabel = 'error' }
        elseif ($skippedNode) { $status = 'skip'; $statusLabel = 'skip' }

        $searchKey = ((($caseClass + ' ' + $caseName).ToLowerInvariant()) -replace '\s+', ' ').Trim()
        [void]$sb.AppendLine(('<li data-status="{0}" data-search="{1}">' -f $status, (Html-Escape $searchKey)))
        [void]$sb.Append(('<span class="status {0}">{1}</span>' -f $(if ($status -eq 'error') { 'fail' } else { $status }), $statusLabel))
        [void]$sb.Append(('<span class="name">{0}</span>' -f (Html-Escape $caseName)))
        [void]$sb.Append(('<span class="time">{0}</span>' -f (Format-Duration $caseTime)))

        $errorContainer = $failureNode
        if (-not $errorContainer) { $errorContainer = $errorNode }
        if ($errorContainer) {
            $msg = if ($errorContainer.Attributes['message']) { $errorContainer.Attributes['message'].Value } else { '' }
            $type = if ($errorContainer.Attributes['type']) { $errorContainer.Attributes['type'].Value } else { '' }
            $body = $errorContainer.InnerText
            [void]$sb.AppendLine('<div class="failure">')
            if ($type -or $msg) {
                [void]$sb.AppendLine(('<strong>{0}</strong>{1}' -f (Html-Escape $type), $(if ($msg) { ': ' + (Html-Escape $msg) } else { '' })))
            }
            if ($body) {
                [void]$sb.AppendLine(('<pre>{0}</pre>' -f (Html-Escape $body)))
            }
            [void]$sb.AppendLine('</div>')
        }
        [void]$sb.AppendLine('</li>')
    }
    [void]$sb.AppendLine('</ul></details>')
}

[void]$sb.AppendLine('</main>')
[void]$sb.AppendLine(('<footer>Generated {0} from <code>{1}</code></footer>' -f (Html-Escape $generatedAt), (Html-Escape (Split-Path -Leaf $JunitFile))))
[void]$sb.AppendLine('<script>')
[void]$sb.AppendLine($js)
[void]$sb.AppendLine('</script></body></html>')

$outDir = Split-Path -Parent $OutputFile
if ($outDir -and -not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}
Set-Content -Path $OutputFile -Value $sb.ToString() -Encoding UTF8

Write-Host ("Generated $OutputFile ({0} tests, {1} failures, {2} errors)" -f $rootTests, $rootFailures, $rootErrors)
