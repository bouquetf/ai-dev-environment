<#
.SYNOPSIS
    Run the CRBPM test suite inside a DooD container.

.DESCRIPTION
    Orchestrator script for the `doexecute-test-dood` Cursor skill. Builds (or
    reuses) a temporary test image, mounts the working tree, wires the host
    Docker daemon through (DooD), executes the requested Gradle test tasks,
    and aggregates JUnit / HTML reports under `tmp/dood-tests/<timestamp>/`
    (plus a stable `tmp/dood-tests/latest/` alias).

.PARAMETER Targets
    Gradle scope to test. Default: `full` + `camunda-live` + `e2e` (complete verification).
    The special target `full` runs every testable Gradle module under all
    engine build profiles (`wrapper,c8`, `wrapper`, `wrapper,stub`) — including
    `backend-shared`, `connectors`, and the engine plug-in matrix — see
    SKILL.md. `all` is an alias for `full`. Pass explicit module names
    (e.g. `backend,api,ui,connectors`) for a faster single-profile loop using
    `crbpm/gradle.properties`. The `contrib` target runs the isolated
    `crbpm-connectors/` Gradle build (contribution modules: handler, descriptor
    validation, register CLI); it is always included in `full`.
    The `camunda-live` target starts the local
    Camunda 8 stack on the host and runs gated `*LiveIT` deploy/start tests.
    The `e2e` target runs Playwright via `./gradlew :ui:e2e` on the host.
    Omit host-only targets with e.g. `-Targets full` or `-Targets backend,api,ui`.

.PARAMETER CamundaSmokeWaitMs
    Optional Gradle property forwarded to `:infra:camundaSmoke` as
    `-PcamundaSmokeWaitMs=…` when `camunda-live` is requested (slow hosts).

.PARAMETER GradleArgs
    Extra arguments forwarded to Gradle (e.g. '--tests *FooTest --info').

.PARAMETER Rebuild
    Force rebuild of the test image even if it already exists.

.PARAMETER KeepContainer
    Do not remove the test container after it exits (useful for debugging).

.PARAMETER ImageName
    Override the test image tag. Default: crbpm-dood-tests:local.

.PARAMETER ReportsDir
    Override the host reports root. Default: <repo>/tmp/dood-tests.

.PARAMETER NoBrowser
    Do not open the per-module HTML report in the default browser after the
    run. By default, the script opens `tmp/dood-tests/latest/<module>/reports/
    test/index.html` for every executed module (and only the failing ones when
    Gradle reports failures, to avoid opening dozens of tabs on success in
    large suites is not desired - currently we open the index of every
    requested module).

.EXAMPLE
    .\run.ps1
    .\run.ps1 -Targets backend,api,ui          # skip e2e
    .\run.ps1 -Targets connectors,contrib      # connectors module + contribution tree
    .\run.ps1 -Targets backend -GradleArgs '--tests *InstanceTimelineResourceTest'
    .\run.ps1 -Targets e2e                     # e2e only
    .\run.ps1 -Rebuild
    .\run.ps1 -NoBrowser
#>

[CmdletBinding()]
param(
    [string[]]$Targets,
    [string]$GradleArgs = '',
    [switch]$Rebuild,
    [switch]$KeepContainer,
    [string]$ImageName = 'crbpm-dood-tests:local',
    [string]$ReportsDir,
    [switch]$NoBrowser,
    [string]$CamundaSmokeWaitMs = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Section([string]$title) {
    Write-Host ''
    Write-Host "==> $title" -ForegroundColor Cyan
}

function Assert-DockerAvailable {
    try {
        docker version --format '{{.Server.Version}}' | Out-Null
    } catch {
        throw "Docker daemon is not reachable. Start Docker Desktop (Windows/macOS) or ensure your user is in the 'docker' group (Linux)."
    }
}

function Resolve-RepoRoot {
    $root = & git rev-parse --show-toplevel 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($root)) {
        throw "Not inside a git repository. The skill requires a working tree to mount."
    }
    return (Resolve-Path $root).Path
}

function Resolve-DockerfilePath([string]$repoRoot) {
    $rootDockerfile = Join-Path $repoRoot 'Dockerfile.dood-tests'
    if (Test-Path $rootDockerfile) { return $rootDockerfile }
    return (Join-Path $PSScriptRoot 'Dockerfile.dood-tests')
}

function Ensure-Image([string]$imageName, [string]$dockerfile, [bool]$forceRebuild) {
    $exists = $false
    try {
        $existing = docker image inspect $imageName --format '{{.Id}}' 2>$null
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($existing)) {
            $exists = $true
        }
    } catch {
        $exists = $false
    }

    if ($exists -and -not $forceRebuild) {
        Write-Host "Reusing image '$imageName' (pass -Rebuild to refresh)."
        return
    }

    Write-Section "Building image '$imageName' from $dockerfile"
    $context = Split-Path -Path $dockerfile -Parent
    & docker build --pull -t $imageName -f $dockerfile $context
    if ($LASTEXITCODE -ne 0) {
        throw "docker build failed (exit $LASTEXITCODE)."
    }
}

function Detect-GradleTargets([string]$repoRoot) {
    # Modules that currently ship automated tests (src/test or npm test).
    # engine-stub / infra have no unit tests (stub covered via :backend:stubTest).
    $candidates = @(
        'backend-shared',
        'rbac-shared',
        'engine-spi',
        'connector-handler-spi',
        'expression',
        'expression-c8',
        'engine-c8',
        'camunda-adapter',
        'connectors',
        'api',
        'backend',
        'ui'
    )
    $present = @()
    foreach ($name in $candidates) {
        $build = Join-Path $repoRoot "crbpm/$name/build.gradle.kts"
        if (Test-Path $build) { $present += $name }
    }
    if ($present.Count -eq 0) {
        throw "No known Gradle module found under crbpm/. Adjust -Targets explicitly."
    }
    return [string[]]$present
}

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory)]
        [scriptblock]$Command
    )
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Command *>&1 | Out-Host
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Invoke-EngineNeutralCoreCheck {
    param([string]$RepoRoot)

    $crbpmRoot = Join-Path $RepoRoot 'crbpm'
    $checkScript = Join-Path $PSScriptRoot 'check-engine-neutral-core.ps1'
    Write-Section 'Engine-neutral core check (host)'
    Write-Host "& $checkScript -CrbpmRoot $crbpmRoot"
    return (Invoke-NativeCommand { & $checkScript -CrbpmRoot $crbpmRoot })
}

function Get-FullVerificationPhases() {
    @(
        @{
            Label = 'full-c8'
            EngineProfile = 'wrapper,c8'
            GradleTasks = @(
                ':backend-shared:test',
                ':rbac-shared:test',
                ':engine-spi:test',
                ':connector-handler-spi:test',
                ':expression:test',
                ':expression-c8:test',
                ':engine-c8:test',
                ':camunda-adapter:test',
                ':connectors:test',
                ':api:test',
                ':ui:test',
                ':backend:test'
            )
            ReportModules = @(
                'backend-shared', 'rbac-shared', 'engine-spi', 'connector-handler-spi', 'expression', 'expression-c8', 'engine-c8',
                'camunda-adapter', 'connectors', 'api', 'backend', 'ui'
            )
        },
        @{
            Label = 'wrapper-only'
            EngineProfile = 'wrapper'
            GradleTasks = @(
                ':backend-shared:test',
                ':rbac-shared:test',
                ':engine-spi:test',
                ':connector-handler-spi:test',
                ':expression:test',
                ':camunda-adapter:test',
                ':connectors:test',
                ':backend:build',
                ':backend:test',
                ':backend:dependenciesWrapperOnly'
            )
            ReportModules = @('backend-shared', 'rbac-shared', 'engine-spi', 'connector-handler-spi', 'backend', 'camunda-adapter', 'connectors')
        },
        @{
            Label = 'stub'
            EngineProfile = 'wrapper,stub'
            GradleTasks = @(':backend:stubTest')
            ReportModules = @('backend')
        }
    )
}

function Normalize-GradleTargets([string[]]$rawTargets) {
    $normalized = @()
    foreach ($target in $rawTargets) {
        if ($target -eq 'all') {
            $normalized += 'full'
        } else {
            $normalized += $target
        }
    }
    return [string[]]$normalized
}

function Invoke-FullVerificationInContainer(
    [string]$repoRoot,
    [string]$gradleArgs,
    [string]$imageName,
    [string]$timestampDir,
    [switch]$KeepContainer
) {
    $phases = Get-FullVerificationPhases
    $phaseResults = @()
    $overallExit = 0

    foreach ($phase in $phases) {
        Write-Section "Full verification phase: $($phase.Label) (-Pcrbpm.engines=$($phase.EngineProfile))"
        $taskList = ($phase.GradleTasks -join ' ')
        $engineProp = "-Pcrbpm.engines=$($phase.EngineProfile)"
        $gradleCommand = "./gradlew $taskList $engineProp --no-daemon --console=plain --stacktrace $gradleArgs".Trim()
        $containerCommand = "set -eo pipefail; cd /work/crbpm && chmod +x ./gradlew && $gradleCommand"

        $runArgs = @(
            'run', '--rm',
            '--init',
            '-v', "${repoRoot}:/work",
            '-v', '//var/run/docker.sock:/var/run/docker.sock',
            '-v', 'crbpm-dood-gradle:/cache/gradle',
            '-v', 'crbpm-dood-npm:/cache/npm',
            '--add-host', 'host.docker.internal:host-gateway',
            '-e', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal',
            '-e', 'TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true',
            '-w', '/work/crbpm',
            $imageName, 'bash', '-lc', $containerCommand
        )

        if ($KeepContainer.IsPresent) {
            $runArgs = $runArgs | Where-Object { $_ -ne '--rm' }
            $stamp = Get-Date -Format 'yyyyMMddHHmmss'
            $runArgs += @('--name', "crbpm-dood-tests-$stamp-$($phase.Label)")
        }

        $phaseExit = Invoke-NativeCommand { & docker @runArgs }
        [void]($phaseResults += [pscustomobject]@{
            Label = $phase.Label
            EngineProfile = $phase.EngineProfile
            ExitCode = $phaseExit
            ReportModules = $phase.ReportModules
        })
        if ($phaseExit -ne 0 -and $overallExit -eq 0) {
            $overallExit = $phaseExit
        }

        # Snapshot reports immediately: later host-side camunda-live re-runs :backend:c8Test
        # and would overwrite crbpm/*/build before end-of-run collection.
        Write-Host "Collecting reports for phase $($phase.Label)..."
        Collect-PhaseReports `
            -repoRoot $repoRoot `
            -phaseLabel $phase.Label `
            -modules $phase.ReportModules `
            -timestampDir $timestampDir
    }

    return ,[pscustomobject]@{
        ExitCode = $overallExit
        Phases = $phaseResults
    }
}

function Get-ContribModules([string]$repoRoot) {
    $modulesRoot = Join-Path $repoRoot 'crbpm-connectors/modules'
    if (-not (Test-Path $modulesRoot)) { return [string[]]@() }
    $names = @(Get-ChildItem -Path $modulesRoot -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'build.gradle.kts') } |
        ForEach-Object { $_.Name })
    return [string[]]$names
}

function Invoke-ContribTestsInContainer(
    [string]$repoRoot,
    [string]$imageName,
    [switch]$KeepContainer
) {
    Write-Section 'Contribution tree tests (crbpm-connectors)'
    Write-Host 'Note: crbpm-connectors/ is an isolated Gradle build (no dependency'
    Write-Host '      from crbpm/*). Its handler and CLI modules resolve'
    Write-Host '      com.moodys.crbpm:connector-handler-spi from mavenLocal, so the'
    Write-Host '      SPI is published inside the container before running the tests.'
    Write-Host '      -GradleArgs is not forwarded here (task/test filters of the'
    Write-Host '      platform build do not apply to this build).'

    $publishCommand = './gradlew :connector-handler-spi:publishToMavenLocal --no-daemon --console=plain'
    $contribCommand = './gradlew test --no-daemon --console=plain --stacktrace'
    $containerCommand = "set -eo pipefail; cd /work/crbpm && chmod +x ./gradlew && $publishCommand" `
        + " && cd /work/crbpm-connectors && chmod +x ./gradlew && $contribCommand"

    $runArgs = @(
        'run', '--rm',
        '--init',
        '-v', "${repoRoot}:/work",
        '-v', '//var/run/docker.sock:/var/run/docker.sock',
        '-v', 'crbpm-dood-gradle:/cache/gradle',
        '-v', 'crbpm-dood-npm:/cache/npm',
        '--add-host', 'host.docker.internal:host-gateway',
        '-e', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal',
        '-e', 'TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true',
        '-w', '/work/crbpm-connectors'
    )

    if ($KeepContainer.IsPresent) {
        $runArgs = $runArgs | Where-Object { $_ -ne '--rm' }
        $stamp = Get-Date -Format 'yyyyMMddHHmmss'
        $runArgs += @('--name', "crbpm-dood-tests-$stamp-contrib")
    }

    $runArgs += @($imageName, 'bash', '-lc', $containerCommand)

    return (Invoke-NativeCommand { & docker @runArgs })
}

function New-TimestampDir([string]$reportsRoot) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $dir = Join-Path $reportsRoot $stamp
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    return $dir
}

function Update-LatestPointer([string]$reportsRoot, [string]$timestampDir) {
    $latest = Join-Path $reportsRoot 'latest'
    if (Test-Path $latest) {
        Remove-Item -Recurse -Force $latest
    }
    try {
        New-Item -ItemType SymbolicLink -Path $latest -Target $timestampDir -ErrorAction Stop | Out-Null
    } catch {
        # Symlink creation can require Dev Mode / admin on Windows; fall back to copy.
        Copy-Item -Recurse -Force $timestampDir $latest
    }
}

function Copy-IfExists([string]$source, [string]$dest) {
    if (Test-Path $source) {
        $destParent = Split-Path -Parent $dest
        if (-not (Test-Path $destParent)) {
            New-Item -ItemType Directory -Path $destParent -Force | Out-Null
        }
        Copy-Item -Recurse -Force $source $dest
    }
}

function Generate-JUnitHtmlReport([string]$junitFile, [string]$outputFile, [string]$title) {
    if (-not (Test-Path $junitFile)) { return $false }
    $generator = Join-Path $PSScriptRoot 'generate-junit-html-report.ps1'
    if (-not (Test-Path $generator)) {
        Write-Host "Missing JUnit HTML generator: $generator" -ForegroundColor Yellow
        return $false
    }
    try {
        & $generator -JunitFile $junitFile -OutputFile $outputFile -Title $title
        return (Test-Path $outputFile)
    } catch {
        Write-Host "Could not generate HTML report from $junitFile : $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

function Collect-ModuleReports(
    [string]$repoRoot,
    [string]$module,
    [string]$destDir,
    [string]$titleSuffix = ''
) {
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null

    $resultsSrc = Join-Path $repoRoot "crbpm/$module/build/test-results"
    $reportsSrc = Join-Path $repoRoot "crbpm/$module/build/reports/tests"

    Copy-IfExists $resultsSrc (Join-Path $destDir 'test-results')
    Copy-IfExists $reportsSrc (Join-Path $destDir 'reports')

    if ($module -eq 'ui') {
        $junitFile = Join-Path $destDir 'test-results/test/junit.xml'
        $reportHtml = Join-Path $destDir 'report.html'
        $title = if ([string]::IsNullOrWhiteSpace($titleSuffix)) {
            'Vitest (ui) - DooD run'
        } else {
            "Vitest (ui) - $titleSuffix"
        }
        Generate-JUnitHtmlReport $junitFile $reportHtml $title | Out-Null
    }
}

function Collect-Reports([string]$repoRoot, [string[]]$modules, [string]$timestampDir) {
    foreach ($module in $modules) {
        $moduleDir = Join-Path $timestampDir $module
        Collect-ModuleReports -repoRoot $repoRoot -module $module -destDir $moduleDir
    }
}

function Collect-PhaseReports(
    [string]$repoRoot,
    [string]$phaseLabel,
    [string[]]$modules,
    [string]$timestampDir
) {
    foreach ($module in $modules) {
        $moduleDir = Join-Path $timestampDir "$phaseLabel/$module"
        Collect-ModuleReports -repoRoot $repoRoot -module $module -destDir $moduleDir -titleSuffix $phaseLabel
    }
}

function Collect-ContribReports([string]$repoRoot, [string[]]$modules, [string]$timestampDir) {
    foreach ($module in $modules) {
        $destDir = Join-Path $timestampDir "contrib/$module"
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        $moduleBuild = Join-Path $repoRoot "crbpm-connectors/modules/$module/build"
        Copy-IfExists (Join-Path $moduleBuild 'test-results') (Join-Path $destDir 'test-results')
        Copy-IfExists (Join-Path $moduleBuild 'reports/tests') (Join-Path $destDir 'reports')
    }
}

function Collect-E2EReports([string]$repoRoot, [string]$timestampDir) {
    $moduleDir = Join-Path $timestampDir 'e2e'
    New-Item -ItemType Directory -Path $moduleDir -Force | Out-Null

    # JUnit XML produced by the Playwright `junit` reporter
    # (see crbpm/ui/playwright.config.js).
    $junitSrc = Join-Path $repoRoot 'crbpm/ui/build/test-results/e2e'
    Copy-IfExists $junitSrc (Join-Path $moduleDir 'test-results/e2e')

    # Playwright artifacts (screenshots, videos, traces) for failing tests.
    $artifactsSrc = Join-Path $repoRoot 'crbpm/ui/test-results'
    Copy-IfExists $artifactsSrc (Join-Path $moduleDir 'artifacts')

    # Playwright's built-in HTML report is a SPA that needs `npx playwright
    # show-report` (or a static server) to open: we keep a copy for users who
    # know that command, but the primary viewer is the self-contained
    # report.html below.
    $playwrightHtml = Join-Path $repoRoot 'crbpm/ui/playwright-report'
    Copy-IfExists $playwrightHtml (Join-Path $moduleDir 'playwright-report')

    $junitFile = Join-Path $moduleDir 'test-results/e2e/junit.xml'
    $reportHtml = Join-Path $moduleDir 'report.html'
    Generate-JUnitHtmlReport $junitFile $reportHtml 'Playwright (e2e) - DooD run' | Out-Null
}

function Open-InBrowser([string]$path) {
    if (-not (Test-Path $path)) { return $false }
    try {
        Start-Process -FilePath $path | Out-Null
        return $true
    } catch {
        Write-Host "Could not open '$path' in browser: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

function Stop-LegacyVitestPreviewServer([string]$reportsRoot) {
    # Backwards compat: kill the detached `vite preview` server from earlier
    # versions of the skill, if it is still hanging around.
    $pidFile = Join-Path $reportsRoot '.vitest-preview.pid'
    if (-not (Test-Path $pidFile)) { return }
    $raw = (Get-Content -Path $pidFile -Raw).Trim()
    if ($raw -match '^\d+$') {
        Stop-Process -Id ([int]$raw) -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -Path $pidFile -Force -ErrorAction SilentlyContinue
}

function Open-ReportsInBrowser([string]$repoRoot, [string]$reportsRoot, [string[]]$modules) {
    $opened = @()
    foreach ($module in $modules) {
        $candidates = @(
            (Join-Path $reportsRoot "latest/$module/report.html"),
            (Join-Path $reportsRoot "latest/$module/reports/c8Test/index.html"),
            (Join-Path $reportsRoot "latest/$module/reports/test/index.html"),
            (Join-Path $reportsRoot "latest/$module/reports/index.html")
        )
        $hit = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
        if (-not $hit) {
            Write-Host "No HTML report found for module '$module' under tmp/dood-tests/latest/." -ForegroundColor Yellow
            continue
        }
        if (Open-InBrowser $hit) { $opened += $hit }
    }
    return ,$opened
}

function Get-GradlewPath([string]$repoRoot) {
    $crbpmDir = Join-Path $repoRoot 'crbpm'
    $isWin = [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT
    $gradlewBat = Join-Path $crbpmDir 'gradlew.bat'
    $gradlewSh = Join-Path $crbpmDir 'gradlew'
    $gradlew = if ($isWin -and (Test-Path $gradlewBat)) { $gradlewBat } else { $gradlewSh }
    if (-not (Test-Path $gradlew)) {
        throw "Gradle wrapper not found at $gradlew."
    }
    return $gradlew
}

function Invoke-GradleOnHost(
    [string]$repoRoot,
    [string[]]$gradleTaskArgs,
    [hashtable]$extraEnv = @{}
) {
    $crbpmDir = Join-Path $repoRoot 'crbpm'
    $gradlew = Get-GradlewPath $repoRoot
    Push-Location $crbpmDir
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $savedEnv = @{}
    foreach ($key in $extraEnv.Keys) {
        $savedEnv[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        Set-Item -Path "Env:$key" -Value $extraEnv[$key]
    }
    try {
        return (Invoke-NativeCommand { & $gradlew @gradleTaskArgs })
    } finally {
        foreach ($key in $extraEnv.Keys) {
            if ($null -eq $savedEnv[$key]) {
                Remove-Item "Env:$key" -ErrorAction SilentlyContinue
            } else {
                Set-Item -Path "Env:$key" -Value $savedEnv[$key]
            }
        }
        $ErrorActionPreference = $previousErrorActionPreference
        Pop-Location
    }
}

function Collect-CamundaLiveReports([string]$repoRoot, [string]$timestampDir) {
    $moduleDir = Join-Path $timestampDir 'camunda-live/backend'
    New-Item -ItemType Directory -Path $moduleDir -Force | Out-Null
    Copy-IfExists (Join-Path $repoRoot 'crbpm/backend/build/test-results/c8Test') (Join-Path $moduleDir 'test-results/c8Test')
    Copy-IfExists (Join-Path $repoRoot 'crbpm/backend/build/reports/tests/c8Test') (Join-Path $moduleDir 'reports/c8Test')
}

function Wait-HttpOk([string]$url, [int]$timeoutSeconds = 180) {
    $deadline = [datetime]::UtcNow.AddSeconds($timeoutSeconds)
    while ([datetime]::UtcNow -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return 0
            }
        } catch {
            Start-Sleep -Seconds 3
            continue
        }
        Start-Sleep -Seconds 3
    }
    Write-Host "Timed out waiting for $url" -ForegroundColor Yellow
    return 1
}

function Get-ConnectorsLiveComposeArgs([string]$repoRoot) {
    return @(
        '-f', 'docker-compose.connectors-standalone.yml',
        '-f', 'docker-compose.connectors-live.yml'
    )
}

function Start-ConnectorExecutionLivePrereqs([string]$repoRoot) {
    Write-Host "Building the connectors image for the live connector-execution stack."
    $buildExit = Invoke-GradleOnHost -repoRoot $repoRoot -gradleTaskArgs @(
        ':connectors:dockerBuild',
        ':connector-handler-spi:publishToMavenLocal',
        '--no-daemon', '--console=plain', '--stacktrace'
    )
    if ($buildExit -ne 0) {
        return $buildExit
    }

    Write-Host "Preparing contributed connector handlers for JVM discovery."
    $contribDir = Join-Path $repoRoot 'crbpm-connectors'
    $contribGradlew = if ($env:OS -eq 'Windows_NT') {
        Join-Path $contribDir 'gradlew.bat'
    } else {
        Join-Path $contribDir 'gradlew'
    }
    Push-Location $contribDir
    try {
        $discoveryExit = Invoke-NativeCommand {
            & $contribGradlew prepareJvmDiscovery --no-daemon --console=plain --stacktrace
        }
    } finally {
        Pop-Location
    }
    if ($discoveryExit -ne 0) {
        return $discoveryExit
    }

    Write-Host "Starting connectors standalone on host port 18083 (QuarkusTest backend catalog URL)."
    $dockerDir = Join-Path $repoRoot 'crbpm/docker'
    $composeArgs = Get-ConnectorsLiveComposeArgs $repoRoot
    Push-Location $dockerDir
    $previousPort = [Environment]::GetEnvironmentVariable('CRBPM_CONNECTORS_PORT', 'Process')
    Set-Item -Path 'Env:CRBPM_CONNECTORS_PORT' -Value '18083'
    try {
        $standaloneExit = Invoke-NativeCommand { & docker compose @composeArgs up -d --force-recreate }
    } finally {
        if ($null -eq $previousPort) {
            Remove-Item 'Env:CRBPM_CONNECTORS_PORT' -ErrorAction SilentlyContinue
        } else {
            Set-Item -Path 'Env:CRBPM_CONNECTORS_PORT' -Value $previousPort
        }
        Pop-Location
    }
    if ($standaloneExit -ne 0) {
        return $standaloneExit
    }
    $connectorsReady = Wait-HttpOk 'http://127.0.0.1:18083/health'
    if ($connectorsReady -ne 0) {
        Write-Host "Connectors standalone did not become healthy; last container logs:" -ForegroundColor Yellow
        [void](Invoke-NativeCommand { & docker logs --tail 40 crbpm-connectors-standalone })
        return $connectorsReady
    }

    Write-Host "Recreating camunda-adapter with the connector job-worker overlay."
    $dockerDir = Join-Path $repoRoot 'crbpm/docker'
    Push-Location $dockerDir
    try {
        $workerExit = Invoke-NativeCommand {
            & docker compose `
                -f docker-compose.yml `
                -f docker-compose.c8.yml `
                -f docker-compose.adapter-live.yml `
                -f docker-compose.adapter-connector-worker.yml `
                up -d --build --force-recreate camunda-adapter
        }
    } finally {
        Pop-Location
    }
    if ($workerExit -ne 0) {
        return $workerExit
    }
    return (Wait-HttpOk 'http://127.0.0.1:18082/health')
}

function Stop-ConnectorExecutionLivePrereqs([string]$repoRoot) {
    Write-Host "Stopping connectors standalone stack used by connector-execution live ITs."
    $dockerDir = Join-Path $repoRoot 'crbpm/docker'
    if (-not (Test-Path $dockerDir)) {
        return
    }
    $composeArgs = Get-ConnectorsLiveComposeArgs $repoRoot
    Push-Location $dockerDir
    try {
        [void](Invoke-NativeCommand { & docker compose @composeArgs down -v })
    } finally {
        Pop-Location
    }
}

function Invoke-CamundaLiveOnHost(
    [string]$repoRoot,
    [string]$gradleArgs,
    [string]$camundaSmokeWaitMs
) {
    Write-Section "Camunda 8 live tests on host (infra + *LiveIT)"
    Write-Host "Note: camunda-live runs OUTSIDE the DooD container. It starts the"
    Write-Host "      local Camunda 8 stack + camunda-adapter on the host Docker"
    Write-Host "      daemon, then runs gated backend live ITs (deploy/start/sync/"
    Write-Host "      CEL routing + connector execution). Requires ~8 GB RAM and Java 21 on the host."
    $liveExit = 0
    try {
        $smokeArgs = @(
            ':infra:camundaSmoke',
            '--no-daemon', '--console=plain', '--stacktrace'
        )
        if (-not [string]::IsNullOrWhiteSpace($camundaSmokeWaitMs)) {
            $smokeArgs += "-PcamundaSmokeWaitMs=$camundaSmokeWaitMs"
        }
        $liveExit = Invoke-GradleOnHost -repoRoot $repoRoot -gradleTaskArgs $smokeArgs
        if ($liveExit -ne 0) {
            return $liveExit
        }

        $liveExit = Start-ConnectorExecutionLivePrereqs -repoRoot $repoRoot
        if ($liveExit -ne 0) {
            return $liveExit
        }

        $liveArgs = @(
            ':backend:c8Test',
            '-Pcrbpm.engines=wrapper,c8',
            '--no-daemon', '--console=plain', '--stacktrace',
            '--tests', 'com.moodys.crbpm.engine.c8.zeebe.*LiveIT'
        )
        if (-not [string]::IsNullOrWhiteSpace($gradleArgs)) {
            $liveArgs += $gradleArgs.Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)
        }
        $liveExit = Invoke-GradleOnHost -repoRoot $repoRoot -gradleTaskArgs $liveArgs -extraEnv @{
            CRBPM_CAMUNDA_LIVE = 'true'
            CRBPM_CONNECTOR_EXECUTION_LIVE = 'true'
        }
        if ($liveExit -ne 0) {
            Write-Host "Camunda live tests failed; adapter and connectors logs:" -ForegroundColor Yellow
            [void](Invoke-NativeCommand { & docker logs --tail 200 crbpm-camunda-adapter })
            [void](Invoke-NativeCommand { & docker logs --tail 200 crbpm-connectors-standalone })
        }
        return $liveExit
    } finally {
        Write-Section "Stopping Camunda 8 test stack"
        Stop-ConnectorExecutionLivePrereqs -repoRoot $repoRoot
        [void](Invoke-GradleOnHost -repoRoot $repoRoot -gradleTaskArgs @(
            ':infra:camundaAdapterDown', ':infra:camundaDown',
            '--no-daemon', '--console=plain'
        ))
    }
}

function Invoke-E2EOnHost([string]$repoRoot, [string]$gradleArgs) {
    Write-Section "Running e2e suite on host (./gradlew :ui:e2e)"
    Write-Host "Note: e2e runs OUTSIDE the DooD container because it boots a"
    Write-Host "      docker-compose stack (crbpm-e2e: api, backend, ui,"
    Write-Host "      connectors, camunda-adapter, postgres-e2e) and drives"
    Write-Host "      Chromium via Playwright in crbpm/ui/. Host ports:"
    Write-Host "      15173 / 18080 / 18081 / 18083 / 15432. Java 21 and"
    Write-Host "      Node.js must be available on the host."
    $argList = @(':ui:e2e', '--no-daemon', '--console=plain', '--stacktrace')
    if (-not [string]::IsNullOrWhiteSpace($gradleArgs)) {
        $argList += $gradleArgs.Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)
    }
    return (Invoke-GradleOnHost -repoRoot $repoRoot -gradleTaskArgs $argList)
}

# ---- main -----------------------------------------------------------------

Assert-DockerAvailable
$repoRoot = Resolve-RepoRoot
Write-Host "Repository root : $repoRoot"

if (-not $ReportsDir) {
    $ReportsDir = Join-Path $repoRoot 'tmp/dood-tests'
}
New-Item -ItemType Directory -Path $ReportsDir -Force | Out-Null
$timestampDir = New-TimestampDir $ReportsDir
Write-Host "Reports dir     : $timestampDir"

$dockerfile = Resolve-DockerfilePath $repoRoot
Ensure-Image -imageName $ImageName -dockerfile $dockerfile -forceRebuild:$Rebuild.IsPresent

if (-not $Targets -or $Targets.Count -eq 0) {
    $Targets = @('full', 'camunda-live', 'e2e')
}
$Targets = Normalize-GradleTargets $Targets

# Split host-only meta-targets from DooD-runnable Gradle modules.
$gradleTargets = @($Targets | Where-Object { $_ -notin @('e2e', 'camunda-live') })
$wantsE2E = ($Targets -contains 'e2e')
$wantsCamundaLive = ($Targets -contains 'camunda-live')
$wantsFullVerification = ($gradleTargets -contains 'full')
$wantsContrib = ($gradleTargets -contains 'contrib') -or $wantsFullVerification

if ($wantsFullVerification) {
    $gradleTargets = @($gradleTargets | Where-Object { $_ -ne 'full' })
}
$gradleTargets = @($gradleTargets | Where-Object { $_ -ne 'contrib' })

Write-Host "Gradle targets  : $(if ($wantsFullVerification) { 'full (all engine profiles)' } elseif ($gradleTargets.Count -gt 0) { $gradleTargets -join ', ' } else { '(none)' })"
if ($wantsContrib) { Write-Host "Contrib tree    : enabled (crbpm-connectors isolated Gradle build)" }
if ($wantsCamundaLive) { Write-Host "Camunda live    : enabled (host-side :infra:camundaSmoke + *LiveIT)" }
if ($wantsE2E) { Write-Host "E2E             : enabled (host-side ./gradlew :ui:e2e)" }

$gradleExit = 0
$fullVerificationResult = $null
$engineNeutralExit = 0

if ($wantsFullVerification) {
    $engineNeutralExit = Invoke-EngineNeutralCoreCheck -RepoRoot $repoRoot
}

if ($wantsFullVerification -and $engineNeutralExit -eq 0) {
    $fullVerificationResult = Invoke-FullVerificationInContainer `
        -repoRoot $repoRoot `
        -gradleArgs $GradleArgs `
        -imageName $ImageName `
        -timestampDir $timestampDir `
        -KeepContainer:$KeepContainer.IsPresent
    $gradleExit = $fullVerificationResult.ExitCode
} elseif ($wantsFullVerification) {
    Write-Host 'Skipping DooD full verification because engine-neutral core check failed.' -ForegroundColor Yellow
    $gradleExit = $engineNeutralExit
} elseif ($gradleTargets.Count -gt 0) {
    $gradleTaskList = $gradleTargets | ForEach-Object { ":$($_):test" }

    $gradleCommand = "./gradlew $($gradleTaskList -join ' ') --no-daemon --console=plain --stacktrace $GradleArgs".Trim()
    $containerCommand = "set -eo pipefail; cd /work/crbpm && chmod +x ./gradlew && $gradleCommand"

    $runArgs = @(
        'run','--rm',
        '--init',
        '-v', "${repoRoot}:/work",
        '-v', '//var/run/docker.sock:/var/run/docker.sock',
        '-v', 'crbpm-dood-gradle:/cache/gradle',
        '-v', 'crbpm-dood-npm:/cache/npm',
        '--add-host', 'host.docker.internal:host-gateway',
        '-e', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal',
        '-e', 'TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true',
        '-w', '/work/crbpm'
    )

    if ($KeepContainer.IsPresent) {
        $runArgs = $runArgs | Where-Object { $_ -ne '--rm' }
        $stamp = Get-Date -Format 'yyyyMMddHHmmss'
        $runArgs += @('--name', "crbpm-dood-tests-$stamp")
    }

    $runArgs += @($ImageName, 'bash', '-lc', $containerCommand)

    Write-Section "Running tests in DooD container"
    Write-Host "docker $($runArgs -join ' ')"

    $gradleExit = Invoke-NativeCommand { & docker @runArgs }

    Write-Section "Collecting DooD reports"
    Collect-Reports -repoRoot $repoRoot -modules $gradleTargets -timestampDir $timestampDir
} else {
    Write-Host "No Gradle modules requested - skipping DooD test run." -ForegroundColor Yellow
}

$contribExit = 0
$contribModules = @()
if ($wantsContrib) {
    $contribModules = Get-ContribModules $repoRoot
    if ($contribModules.Count -eq 0) {
        Write-Host 'No crbpm-connectors module found - skipping contribution tree tests.' -ForegroundColor Yellow
    } else {
        try {
            $contribExit = Invoke-ContribTestsInContainer `
                -repoRoot $repoRoot `
                -imageName $ImageName `
                -KeepContainer:$KeepContainer.IsPresent
        } catch {
            Write-Host "Contribution tree run failed: $($_.Exception.Message)" -ForegroundColor Red
            $contribExit = 1
        }
        Write-Host 'Collecting contribution tree reports...'
        Collect-ContribReports -repoRoot $repoRoot -modules $contribModules -timestampDir $timestampDir
    }
}

$camundaLiveExit = 0
if ($wantsCamundaLive) {
    try {
        $camundaLiveExit = Invoke-CamundaLiveOnHost -repoRoot $repoRoot -gradleArgs $GradleArgs -camundaSmokeWaitMs $CamundaSmokeWaitMs
    } catch {
        Write-Host "Camunda live orchestration failed: $($_.Exception.Message)" -ForegroundColor Red
        $camundaLiveExit = 1
    }
}

$e2eExit = 0
if ($wantsE2E) {
    try {
        $e2eExit = Invoke-E2EOnHost -repoRoot $repoRoot -gradleArgs $GradleArgs
    } catch {
        Write-Host "E2E orchestration failed: $($_.Exception.Message)" -ForegroundColor Red
        $e2eExit = 1
    }
}

Write-Section "Collecting reports"
# Full verification and explicit DooD module runs snapshot Gradle output before
# camunda-live (host) can overwrite crbpm/*/build. Only collect host-only targets here.
if ($wantsE2E) {
    Collect-E2EReports -repoRoot $repoRoot -timestampDir $timestampDir
}
if ($wantsCamundaLive) {
    Collect-CamundaLiveReports -repoRoot $repoRoot -timestampDir $timestampDir
}

$summaryPath = Join-Path $timestampDir 'summary.txt'
$e2eExitDisplay = if ($wantsE2E) { "$e2eExit" } else { 'n/a' }
$camundaLiveExitDisplay = if ($wantsCamundaLive) { "$camundaLiveExit" } else { 'n/a' }
$engineNeutralExitDisplay = if ($wantsFullVerification) { "$engineNeutralExit" } else { 'n/a' }
$summaryLines = [System.Collections.Generic.List[string]]::new()
$summaryLines.Add("Run at        : $(Get-Date -Format o)")
$summaryLines.Add("Image         : $ImageName")
$summaryLines.Add("Targets       : $($Targets -join ', ')")
$summaryLines.Add("Gradle args   : $GradleArgs")
$summaryLines.Add("Engine-neutral: $engineNeutralExitDisplay")
$summaryLines.Add("Gradle exit   : $gradleExit")
$summaryLines.Add("Contrib exit  : $(if ($wantsContrib) { "$contribExit" } else { 'n/a' })")
$summaryLines.Add("Camunda live  : $camundaLiveExitDisplay")
$summaryLines.Add("E2E exit      : $e2eExitDisplay")
if ($wantsFullVerification -and $null -ne $fullVerificationResult) {
    foreach ($phase in $fullVerificationResult.Phases) {
        $summaryLines.Add("Phase $($phase.Label) (-Pcrbpm.engines=$($phase.EngineProfile)) exit : $($phase.ExitCode)")
    }
}
$summaryLines.Add("Repo root     : $repoRoot")
$summaryLines.Add("Reports dir   : $timestampDir")
$summaryLines | Set-Content -Path $summaryPath -Encoding UTF8

Update-LatestPointer -reportsRoot $ReportsDir -timestampDir $timestampDir

Write-Section "Summary"
Get-Content $summaryPath | Write-Host

# Stop any leftover `vite preview` server from previous skill versions so the
# user does not see a stale http://127.0.0.1:4175/ page after this run.
Stop-LegacyVitestPreviewServer $ReportsDir

if (-not $NoBrowser.IsPresent) {
    Write-Section "Opening reports in default browser"
    $browserModules = @()
    if ($wantsFullVerification) {
        $browserModules += 'full-c8/backend'
        $browserModules += 'full-c8/connectors'
        $browserModules += 'full-c8/ui'
        $browserModules += 'wrapper-only/backend'
        $browserModules += 'wrapper-only/connectors'
        $browserModules += 'stub/backend'
    } else {
        $browserModules += $gradleTargets
    }
    # Contribution modules are small CLI/handler suites: only surface them when
    # they fail, to keep the number of browser tabs reasonable.
    if ($wantsContrib -and $contribExit -ne 0) {
        $browserModules += ($contribModules | ForEach-Object { "contrib/$_" })
    }
    if ($wantsCamundaLive) { $browserModules += 'camunda-live/backend' }
    if ($wantsE2E) { $browserModules += 'e2e' }
    if ($browserModules.Count -gt 0) {
        $opened = Open-ReportsInBrowser -repoRoot $repoRoot -reportsRoot $ReportsDir -modules $browserModules
        foreach ($url in $opened) {
            Write-Host "Opened: $url"
        }
    }
}

$contribExitDisplay = if ($wantsContrib) { "$contribExit" } else { 'n/a' }
$overallExit = if ($engineNeutralExit -ne 0) { $engineNeutralExit }
    elseif ($gradleExit -ne 0) { $gradleExit }
    elseif ($contribExit -ne 0) { $contribExit }
    elseif ($camundaLiveExit -ne 0) { $camundaLiveExit }
    elseif ($e2eExit -ne 0) { $e2eExit }
    else { 0 }
if ($overallExit -ne 0) {
    Write-Host "Tests reported failures (engine-neutral=$engineNeutralExitDisplay, gradle=$gradleExit, contrib=$contribExitDisplay, camunda-live=$camundaLiveExitDisplay, e2e=$e2eExitDisplay). See $timestampDir." -ForegroundColor Yellow
    exit $overallExit
}

Write-Host "All requested tests passed. Reports: $timestampDir" -ForegroundColor Green
