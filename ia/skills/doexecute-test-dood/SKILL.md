---
name: doexecute-test-dood
description: >-
  Executes the CRBPM automated test suite inside a disposable Docker container
  that reaches the host Docker daemon (DooD pattern), so Quarkus Dev Services
  and Testcontainers can spin up sibling Postgres / Kafka / Redis containers.
  This is the **execution** half of the former monolithic `run-tests-dood`
  skill: it runs the orchestrator scripts and validates the platform. By
  default runs **full** verification — every testable Gradle module under all
  engine build profiles (`wrapper,c8`, `wrapper`, `wrapper,stub`) including
  `backend-shared`, engine plug-in architecture tests, `:connectors:test`, the
  isolated `crbpm-connectors/` contribution build (`contrib`), Camunda 8 live
  deploy/start ITs (`camunda-live` on the host), the UI Playwright e2e suite
  (`./gradlew :ui:e2e` on the host), and an engine-neutral core grep guard.
  Bind-mounts the working tree, builds image `crbpm-dood-tests:local` when
  needed, lands JUnit/HTML reports under `tmp/dood-tests/latest/`, opens HTML
  reports in the default browser, and exposes them via JetBrains MCP. Use when
  `run-tests-dood` hands off after its coverage gate, or when the user
  explicitly asks to **execute only** / skip the coverage audit
  (`doexecute-test-dood`, "lance full direct", "skip coverage check"). Prefer
  `run-tests-dood` as the normal entry when the user asks to "run tests in
  docker" / DooD so missing suites are patched into this skill first. Pass
  `-Targets backend,api,ui` (without `full`) for a faster single-profile loop.
---

# Execute CRBPM platform tests in a DooD container

> **Entry skill:** prefer invoking [`run-tests-dood`](../run-tests-dood/SKILL.md), which audits whether this skill covers all available suites, patches gaps, then runs this skill. Invoke `doexecute-test-dood` directly only when the user asks to skip that coverage gate (e.g. "execute only", "skip coverage check", known-good default suite).

## When this applies

Invoke this skill to **execute** the platform test suite in DooD (or host-side
`camunda-live` / `e2e` targets). Typical triggers: hand-off from
`run-tests-dood`, or an explicit "execute only" / "skip coverage check" request.

This skill is **read-only** with respect to the project sources: it bind-mounts
the working tree, so uncommitted changes are picked up automatically. It does
not commit, push, or stage anything. **Coverage gaps** (suites that exist in
the repo but are not wired into `scripts/run.*`) are fixed by `run-tests-dood`,
not by this skill at run time.

## What the skill produces

- A temporary Docker image `crbpm-dood-tests:local` (built once, cached).
- A run log + aggregated JUnit/HTML reports under
  `tmp/dood-tests/<timestamp>/`, with a stable symlink/copy at
  `tmp/dood-tests/latest/` (the `tmp/` folder is already in `.gitignore`).
- The per-module HTML reports **opened automatically in the default browser**
  (suppress with `-NoBrowser` / `--no-browser`). The system browser is the
  primary viewer because IntelliJ's native test-runner tool window cannot
  ingest the Gradle JUnit XML produced by an external DooD run (see
  [Why not the IntelliJ test runner?](#why-not-the-intellij-test-runner)).
- Convenience access through IntelliJ via the JetBrains MCP server (open
  `summary.txt`, browse XML files, navigate to failing tests).
- No host-side daemon, JDK, or Node install is needed.

## High-level workflow (agent checklist)

```
- [ ] 1. Confirm Docker is available on the host (Docker Desktop on Windows).
- [ ] 2. Honour `-Targets` from the caller (`run-tests-dood` or the user).
       Default when none given: `full` + `camunda-live` + `e2e`.
- [ ] 3. Build the temporary image if missing or `-Rebuild` was requested.
- [ ] 4. Run the container with DooD wiring and project mount
       (`scripts/run.ps1` / `scripts/run.sh`).
- [ ] 5. Aggregate reports under `tmp/dood-tests/latest/`.
- [ ] 6. Open per-module HTML reports in the default browser
       (the orchestrator script does this; suppress with -NoBrowser).
- [ ] 7. Open `summary.txt` (and failing JUnit XML on Gradle failure) in
       IntelliJ via the JetBrains MCP server.
- [ ] 8. Report status + report paths back to the user.
```

The orchestrator scripts in `scripts/` perform steps 1–6 end-to-end. **Step 7
is performed by the agent** (IDE access). See
[Opening reports via the JetBrains MCP server](#opening-reports-via-the-jetbrains-mcp-server).
The agent should **call the script for the current host shell** rather than
re-implement the test-run logic inline.

**Coverage / relevance gate:** owned by [`run-tests-dood`](../run-tests-dood/SKILL.md)
(inventory of available suites vs what this skill runs, plus change-set
relevance). Do **not** re-run that gate here unless the user invoked this skill
directly and asked for a focused `-Targets` decision.
## Default invocation

> **Agent default — do not disable the browser, do not skip host targets.** When
> invoking the orchestrator on behalf of the user, **do not** pass
> `-NoBrowser` / `--no-browser`. Auto-opening the per-module HTML reports and
> running the full unit + integration + Camunda live + e2e suite are the
> intended defaults for a full platform validation.
>
> **`-Targets` / `--targets`:** do **not** silently narrow below `full` +
> `camunda-live` + `e2e` unless (1) the user **explicitly** asked for a
> focused set (e.g. "skip e2e", "seulement connectors"), or (2) `run-tests-dood`
> (or the user) already scoped the feature and passed explicit targets. Always
> state the chosen targets in the reply. Suppress the browser only when the user
> asks ("no browser", "headless", CI context).

### Windows / PowerShell (this repo's primary host)

```powershell
.\.cursor\skills\doexecute-test-dood\scripts\run.ps1
```

Useful flags:

| Flag | Purpose |
|------|---------|
| *(none)* | Default: **`full`** + **`camunda-live`** + **`e2e`**. |
| `-Targets full` | Full DooD verification only (skip Camunda live + e2e). |
| `-Targets full,camunda-live,e2e` | Same as default. |
| `-Targets backend,api,ui` | Fast loop: `:module:test` once using `crbpm/gradle.properties` profile. |
| `-Targets backend` | Single module, fastest feedback loop. |
| `-Targets camunda-live` | Camunda 8 stack + `*LiveIT` only (skip DooD + e2e). |
| `-Targets ui,connectors,e2e` | Platform Connectors UI story loop (Vitest + connectors ITs + Playwright). |
| `-Targets connectors,contrib` | Connectors module + isolated `crbpm-connectors/` contribution build. |
| `-Targets contrib` | Contribution tree only (`crbpm-connectors/` Gradle build). |
| `-Targets e2e` | Playwright e2e only (skip DooD + Camunda live). |
| `-Targets all` | Alias for `full`. |
| `-CamundaSmokeWaitMs 180000` | Forward `-PcamundaSmokeWaitMs=…` to `:infra:camundaSmoke` (slow hosts). |
| `-GradleArgs '--tests *FooTest'` | Forward extra Gradle args (test filtering, `--info`, …). |
| `-Rebuild` | Force rebuild of the test image (e.g. after `Dockerfile.dood-tests` changes). |
| `-KeepContainer` | Keep the test container after it exits (debugging). |
| `-ReportsDir <path>` | Override the host reports folder. |
| `-NoBrowser` | Do **not** auto-open per-module HTML reports in the default browser. **Opt-in only — never set by the agent unless the user asks for it.** |

### Linux / macOS / CI

```bash
./.cursor/skills/doexecute-test-dood/scripts/run.sh
```

Same flags via long options (`--targets`, `--gradle-args`, `--rebuild`,
`--keep-container`, `--reports-dir`, `--no-browser`,
`--camunda-smoke-wait-ms`). `--no-browser` follows
the same agent rule as `-NoBrowser`: **opt-in only, never set by the agent
unless the user asks for it.** On Linux/macOS the script uses `xdg-open` /
`open` / `$BROWSER` (in that order) to launch the default browser; the run
does not fail if none is available.

## Module / test detection

The orchestrator supports two Gradle modes:

### Default: `full` verification

When no `-Targets` / `--targets` is given, or when `full` / `all` is listed,
the script runs **three sequential phases** inside the DooD container (each
phase is a fresh container invocation). This covers the engine plug-in
architecture test matrix (see `docs/testplan/engine-plugin-architecture.md`)
and all shipped stories that rely on automated tests:

| Phase | `-Pcrbpm.engines` | Gradle tasks (summary) |
|-------|-------------------|-------------------------|
| `engine-neutral` | *(host, no Gradle)* | `scripts/check-engine-neutral-core.{ps1,sh}` — grep guard on `backend`, `backend-shared`, `rbac-shared`, `engine-spi`, and `api` sources |
| `full-c8` | `wrapper,c8` | `backend-shared`, `rbac-shared`, `engine-spi`, `connector-handler-spi`, `expression`, `expression-c8`, `engine-c8`, `camunda-adapter`, `connectors`, `api`, `ui`, `backend:test` (+ `c8Test` via `finalizedBy`; `topologyProbeTest` via `engine-c8:test`) |
| `wrapper-only` | `wrapper` | `backend-shared`, `rbac-shared`, `engine-spi`, `connector-handler-spi`, `expression`, `camunda-adapter`, `connectors`, `backend:build`, `backend:test`, `backend:dependenciesWrapperOnly` (+ `WrapperOnlyEnginesIT`) |
| `stub` | `wrapper,stub` | `backend:stubTest` (`EnginePlugInExtensibilityIT`) |

After those phases, the orchestrator runs the **`contrib`** step in a fourth
container invocation (also implied by `full`, see below), then `camunda-live`
and `e2e` are **appended** to the default target list and run on
the host after the DooD Gradle phases complete (`camunda-live` first, then
`e2e`, so the Camunda stack is torn down before the e2e compose stack starts).

### `contrib`: the isolated `crbpm-connectors/` build

`crbpm-connectors/` (sibling of `crbpm/`) is a **separate Gradle build** — no
`crbpm/*` module depends on it, so its tests are **not** reachable from
`/work/crbpm` tasks. The `contrib` target therefore runs its own container step:

```bash
cd /work/crbpm            && ./gradlew :connector-handler-spi:publishToMavenLocal
cd /work/crbpm-connectors && ./gradlew test
```

The SPI publish is required because contribution modules resolve
`com.moodys.crbpm:connector-handler-spi` from `mavenLocal()`; both commands run
in the **same** container so the local Maven repository is shared.

`contrib` is enabled automatically with `full` (and thus in the default run),
and can be requested alone or combined with modules (`-Targets connectors,contrib`).
`-GradleArgs` / `--gradle-args` is **not** forwarded to this build: filters that
target platform tasks would fail against the contribution build.

Reports land under `tmp/dood-tests/<timestamp>/contrib/<module>/`. To keep the
number of browser tabs reasonable, contribution reports are opened
automatically **only when the `contrib` step fails**.

When `full` is requested, the orchestrator runs the **engine-neutral core
check** on the host **before** the three DooD Gradle phases. It scans
`crbpm/backend/src/main`, `crbpm/backend-shared/src/main`,
`crbpm/rbac-shared/src/main`, `crbpm/engine-spi/src/main`, and `crbpm/api/src/main` for disallowed
`camunda` / `zeebe` references (historical Flyway scripts under
`db/migration/` are excluded; `BpmnVendorExtensionValidator` may list
`camunda.org` / `zeebe.io` as forbidden vendor URI fragments only). If this
check fails, the DooD phases are skipped. Scripts:
`scripts/check-engine-neutral-core.ps1` (Windows) /
`scripts/check-engine-neutral-core.sh` (Linux/macOS).

**Report snapshot timing:** Gradle HTML/JUnit output under `crbpm/*/build/` is
copied into `tmp/dood-tests/<timestamp>/` **immediately after each DooD phase**
(or after an explicit module run), **before** host-side `camunda-live` re-runs
`:backend:c8Test` with a `*LiveIT` filter and overwrites `build/reports/tests/c8Test`.
The `full-c8/backend/reports/c8Test` tree therefore reflects the full hermetic
`c8Test` suite (including `MultiEngineRegistrationIT`), not the filtered live run.

### Fast path: explicit module names

Pass `-Targets backend,api,ui,connectors` (without `full`) to run `:module:test` once
using the engine profile from `crbpm/gradle.properties` (default
`wrapper,c8`). Useful while iterating on a single area. Any module with a
`:test` task can be listed (including `backend-shared`, `engine-spi`,
`expression`, `expression-c8`, `engine-c8`, `camunda-adapter`).

### Coverage inventory (current project)

| Module / suite | How `full` (+ defaults) covers it |
|----------------|----------------------------------|
| `backend-shared` | `:backend-shared:test` in `full-c8` and `wrapper-only` |
| `rbac-shared` | `:rbac-shared:test` in `full-c8` and `wrapper-only` (role inheritance) |
| `engine-spi` | `:engine-spi:test` in `full-c8` and `wrapper-only` |
| `connector-handler-spi` | `:connector-handler-spi:test` in `full-c8` and `wrapper-only` (descriptor schema + handler context) |
| `expression` | `:expression:test` in `full-c8` and `wrapper-only` |
| `expression-c8` | `:expression-c8:test` in `full-c8` only |
| `engine-c8` | `:engine-c8:test` (+ `topologyProbeTest`) in `full-c8` only |
| `camunda-adapter` | `:camunda-adapter:test` (+ `c8Test` via `finalizedBy` when c8 enabled) |
| `connectors` | `:connectors:test` in `full-c8` and `wrapper-only` (catalog, lifecycle, config ITs, JVM discovery + Docker register-agent smoke) |
| `crbpm-connectors` (contribution tree) | `contrib` step → `crbpm-connectors/gradlew test` (`connector-annotations`, `demo-echo`, `descriptor-validate`, `http-request`, `register-client`) after `:connector-handler-spi:publishToMavenLocal` |
| `api` | `:api:test` in `full-c8` |
| `ui` (Vitest) | `:ui:test` in `full-c8` |
| `backend` | `:backend:test` (+ hermetic `c8Test` via `finalizedBy` in `full-c8`) |
| `backend` stub | `:backend:stubTest` in `stub` phase |
| `engine-stub` | No unit tests; exercised by `:backend:stubTest` |
| `infra` | No unit tests; host `:infra:camundaSmoke` via `camunda-live` |
| Camunda live ITs | Host `camunda-live` → `:backend:c8Test` filtered `*LiveIT` (`CRBPM_CAMUNDA_LIVE` + `CRBPM_CONNECTOR_EXECUTION_LIVE`, connectors standalone :18083, adapter worker overlay) |
| Playwright e2e | Host `e2e` → `:ui:e2e` (smoke + tenant demo + **connectors-admin**) |
| Engine-neutral core | Host grep guard before DooD phases |

**Intentionally not in the default run:** root `composeSmoke` (full C8 compose health then tear-down). Overlaps with e2e/camunda stacks; run manually with `.\gradlew composeSmoke` from `crbpm/` when needed.

### Other rules

1. **Explicit override**: if the user passes `-Targets` / `--targets`, the
   script honours it verbatim (except `all` → `full`).
2. **Always reactive**: tests run with `./gradlew … --no-daemon --console=plain`
   from `/work/crbpm` inside the container (host-side targets run from `crbpm/`
   on the host).

If neither Gradle nor npm is detected, the script aborts with a clear message
rather than running an empty container.

## Camunda live target (host-side deploy / start ITs)

`camunda-live` is a **meta-target** the orchestrator recognises in `-Targets` /
`--targets`. It is **included in the default run** alongside `full` and `e2e`.
When listed (default, `-Targets camunda-live`, or any explicit list containing
it), the script:

1. **Does not** run inside the DooD container. Live ITs need the backend test
   process on the host (`quarkus.http.test-port` **18081**) so Camunda
   Connectors (running in the host Docker stack) can call back via
   `host.docker.internal:18081`.
2. Invokes `./gradlew :infra:camundaSmoke` on the host, which brings up the
   Camunda 8 compose overlay (`crbpm/docker/docker-compose.c8.yml`) plus the
   `camunda-adapter` service (adapter on host port **18082**), and waits until
   Zeebe / Operate / Connectors / adapter health checks pass.
3. Sets `CRBPM_CAMUNDA_LIVE=true` **and** `CRBPM_CONNECTOR_EXECUTION_LIVE=true`, starts
   connectors standalone on host port **18083** (`docker-compose.connectors-standalone.yml`
   plus `docker-compose.connectors-live.yml`). Before
   startup it publishes `connector-handler-spi`, runs the contribution build's
   `prepareJvmDiscovery`, and the live overlay mounts the resulting handlers into Connectors
   and points execution callbacks at `http://host.docker.internal:18081`. Hibernate stays on
   the docker profile `validate` strategy against connectors SQL only. It then recreates
   `camunda-adapter` with `docker-compose.adapter-connector-worker.yml`, and runs:

   ```text
   ./gradlew :backend:c8Test -Pcrbpm.engines=wrapper,c8 \
     --tests 'com.moodys.crbpm.engine.c8.zeebe.*LiveIT'
   ```

   This executes the gated live integration tests (`*LiveIT` in that package):

   - `ZeebeDeployLiveIT` — BPMN deploy to Zeebe via the adapter
   - `ZeebeStartLiveIT` — start process instance
   - `ZeebeStartCompensationLiveIT` — start compensation against a live engine
   - `ZeebeStatusSyncLiveIT` — instance status sync / callbacks
   - `ZeebeConnectorExecutionLiveIT` — connector job execution against a live engine (requires the worker overlay + connectors on 18083; skipped unless `CRBPM_CONNECTOR_EXECUTION_LIVE=true`)
   - `XorCelRoutingLiveIT` — CEL / XOR routing against a live engine
   - `XorCelAfterConnectorLiveIT` — connector output routing through an ids-only CEL / XOR settlement (requires the worker overlay + connectors on 18083)

4. **Always tears down** in a `finally` block, even on failure:

   ```text
   docker compose -f docker-compose.connectors-standalone.yml -f <skill override> down -v
   ./gradlew :infra:camundaAdapterDown :infra:camundaDown
   ```

5. Collects `backend/build/reports/tests/c8Test` and matching JUnit XML under
   `tmp/dood-tests/latest/camunda-live/backend/`.
6. Adds `Camunda live : <code>` to `summary.txt` alongside Gradle and e2e exits.

### Host prerequisites for `camunda-live`

- Docker Desktop (or Linux daemon) running — same as DooD / e2e.
- **Java 21** on `PATH` for Gradle on the host.
- **~8 GB RAM** free for the Camunda 8 stack (Zeebe, Operate, Connectors, …).
- Host ports **26500** (gRPC), **28080** / **28081** (Operate / Tasklist),
  **18082** (camunda-adapter), and **18083** (connectors standalone for live
  connector execution). See `crbpm/infra/build.gradle.kts` and
  `docs/crbpm-camunda8-dev.md`.
- Slow machines: pass `-CamundaSmokeWaitMs 180000` (PowerShell) or
  `--camunda-smoke-wait-ms 180000` (bash) to extend the smoke readiness window.

### Skipping Camunda live

Pass an explicit target list **without** `camunda-live`, e.g. `-Targets full,e2e`
or `-Targets backend,api,ui`. Hermetic `c8Test` classes (WireMock / no live
engine) still run inside the DooD `full-c8` phase via `backend:test` →
`finalizedBy c8Test`.

## E2E target (host-side Playwright)

`e2e` is **not** a Gradle subproject — it is a meta-target the orchestrator
recognises in `-Targets` / `--targets`, **and it is included in the default
run** when no `-Targets` is given (alongside the auto-detected Gradle
modules). Pass an explicit list without `e2e` (e.g. `-Targets backend,api,ui`)
to skip it. When listed (e.g. default invocation, `-Targets
backend,api,ui,e2e`, or `-Targets e2e` to run it alone), the script:

1. **Does not** run e2e inside the DooD container. Playwright drives Chromium
   against a full `docker-compose` stack (see `crbpm/docker/docker-compose.e2e.yml`,
   project name `crbpm-e2e`) and the stack itself uses the host Docker daemon,
   so nesting it inside DooD would just add a hop without isolation benefits.
2. Invokes `./gradlew :ui:e2e --no-daemon --console=plain --stacktrace` from
   the repo's `crbpm/` directory **on the host**. That task (defined in
   `crbpm/ui/build.gradle.kts`) chains:
   - `e2eUp` — rebuilds **api / backend / ui / connectors / camunda-adapter**
     images plus the **`crbpm-e2e-postgres`** image (Backend + Connectors SQL
     migrations baked in via `docker/postgres-e2e.Dockerfile`), then brings the
     `crbpm-e2e` compose stack up with `--wait`. Compose files:
     `docker-compose.yml` + `docker-compose.connectors.yml` +
     `docker-compose.c8.yml` + `docker-compose.e2e.yml`. The e2e overlay runs
     Connectors over plain HTTP (mTLS off) so the Backend proxy can reach the
     catalog without cert mounts.
   - `e2eTest` — `npm run test:e2e` (Playwright `chromium` project), including
     `e2e/connectors-admin.spec.js` (platform Connectors hub + connectivity
     Test wizard against the live Connectors sandbox).
   - `e2eDown` — `finalizedBy`, runs even on test failure, removes containers
     and the dedicated Postgres volume.
3. Collects the artifacts into the same `tmp/dood-tests/latest/` tree as
   the other modules:
   - `crbpm/ui/build/test-results/e2e/junit.xml` → `e2e/test-results/e2e/junit.xml`
     (source of truth for tooling — Playwright's `junit` reporter is
     enabled in `crbpm/ui/playwright.config.js`).
   - `crbpm/ui/test-results/` (screenshots, videos, traces) → `e2e/artifacts/`.
   - `crbpm/ui/playwright-report/` (Playwright's built-in HTML SPA) →
     `e2e/playwright-report/` for users who want `npx playwright show-report`.
4. Reuses `generate-junit-html-report.ps1` / `.py` to render a single-file
   `tmp/dood-tests/latest/e2e/report.html` (same look and feel as the
   `ui/report.html` generated for Vitest).
5. Adds `E2E exit : <code>` to `summary.txt` alongside the Gradle exit.

### Host prerequisites for `e2e`

- Docker Desktop (or a Linux daemon) running — same as the DooD path.
- **Java 21** on `PATH` (the Gradle wrapper invokes `quarkusBuild` /
  `dockerBuild` on api/backend/connectors before `e2eUp`). Note: the JDK
  shipped inside the DooD image is not reachable for this target — it runs on
  the host.
- **Node.js + npm** for the first run; `./gradlew :ui:e2e` will trigger
  `npm install` and `playwright install chromium` automatically.
- Host ports `15173` (UI), `18080` (API), `18081` (Backend), `18083`
  (Connectors), `15432` (Postgres) free (the compose override shifts them away
  from the dev defaults so a regular `composeUp` stack can keep running on
  `5173`/`8080`/`8081`/`8083`/`5432` in parallel).

### Platform Connectors UI coverage (story validation)

The default skill run (`full` + `camunda-live` + `e2e`) and the focused loop
`-Targets ui,connectors,e2e` both validate the platform Connectors admin story:

| Automated check | Where it runs |
|-----------------|---------------|
| Vitest unit tests (systems list, actions, versions lifecycle, RJSF platform config, Test wizard) | DooD `:ui:test` (`full-c8` or `-Targets ui`) |
| Vitest + MSW integration (create system through the real `backendApi` client) | DooD `:ui:test` |
| Connectors catalog / lifecycle / config service ITs | DooD `:connectors:test` |
| Playwright E2E: admin sees **Connectors**, seeds a system via Backend proxy, opens the **Test** modal wizard, gets a live sandbox response (no browser call to `:8083`) | Host `e2e` → `connectors-admin.spec.js` |

Camunda live ITs are **not** required for that story; use
`-Targets ui,connectors,e2e` when iterating on Connectors UI only.

## Workspace exposure (uncommitted changes)

The script **mounts the repository root** (the directory returned by
`git rev-parse --show-toplevel`) into `/work` inside the container, read-write:

- All staged + unstaged + untracked-not-ignored files are visible to the
  container immediately — no `git stash`, no `docker cp`, no rebuild.
- The container's `WORKDIR` is `/work/crbpm` so the Gradle wrapper picks up the
  project layout described in `AGENTS.md` (Quarkus 3.30.5 multi-module).
- Gradle's user home and npm cache point to **named Docker volumes**
  (`crbpm-dood-gradle`, `crbpm-dood-npm`) so the host's `~/.gradle` and
  `~/.npm` are not touched and runs stay fast on repeat invocations.

## DooD wiring

The container reaches the host Docker daemon so Testcontainers / Quarkus Dev
Services can start sibling Postgres containers:

- `-v //var/run/docker.sock:/var/run/docker.sock` (Docker Desktop on Windows
  translates this to its named pipe automatically).
- `--add-host host.docker.internal:host-gateway` for Linux hosts; no-op on
  Docker Desktop.
- `-e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` so Testcontainers
  connects to sibling containers via the host gateway, not the test
  container's `localhost`.
- `-e TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true` to keep the Ryuk reaper
  working when the daemon enforces user namespaces.

## Reports tree

After the run, the script copies the standard Gradle / Vitest outputs into a
flat, IntelliJ-friendly tree:

```
tmp/dood-tests/
├── latest/                       # always points to the most recent run
│   ├── summary.txt               # exit codes + per-phase lines when `full`
│   ├── full-c8/                  # phase 1 reports (wrapper,c8)
│   │   ├── backend-shared/ … engine-spi/ … backend/ … connectors/ … ui/
│   ├── wrapper-only/             # phase 2 reports (wrapper)
│   │   ├── backend-shared/ … backend/ … camunda-adapter/ … connectors/
│   ├── stub/                     # phase 3 reports (wrapper,stub)
│   │   └── backend/              # stubTest JUnit + HTML
│   ├── contrib/                  # crbpm-connectors/ contribution build
│   │   ├── demo-echo/ … descriptor-validate/ … register-client/
│   ├── backend/                  # only when using explicit -Targets backend
│   ├── api/
│   ├── ui/
│   │   └── report.html           # Vitest static HTML
│   ├── camunda-live/             # when `camunda-live` was requested
│   │   └── backend/
│   │       ├── reports/c8Test/index.html
│   │       └── test-results/c8Test/
│   └── e2e/                      # when `e2e` was requested
│       ├── test-results/e2e/junit.xml
│       ├── artifacts/
│       └── report.html
└── 20260527-171942/              # timestamped snapshot of the same content
```

`tmp/` is already covered by the repo `.gitignore`, so nothing leaks into git.

## Opening reports in the default browser

The orchestrator scripts open selected HTML reports after the run (Windows:
`Start-Process`; Linux/macOS: `xdg-open` / `open` / `$BROWSER`). Pass
`-NoBrowser` / `--no-browser` to skip this.

**Default `full` run** auto-opens: `full-c8/backend`, `full-c8/connectors`,
`full-c8/ui`, `wrapper-only/backend`, `wrapper-only/connectors`, `stub/backend`,
plus `camunda-live/backend` and `e2e/report.html` when those targets run.
Other modules tested in DooD (e.g. `api`, `camunda-adapter`, `engine-c8`) have
reports **collected** under `tmp/dood-tests/latest/<phase>/<module>/` but are
not opened automatically (to limit browser tabs). Contribution modules follow
the same rule and open only when the `contrib` step fails. Open manually, e.g.:

`tmp/dood-tests/latest/full-c8/connectors/reports/test/index.html`

For a single-module loop (`-Targets connectors`), the connectors report opens
automatically.

The HTML report is the recommended viewer for DooD runs because:

- It is the native Gradle test report, already rich (suites, failures,
  durations, stack traces, output capture).
- It works offline, no network needed.
- It avoids the IntelliJ limitation described below.

If the script cannot find a browser opener (typical for headless CI), it logs
a message and continues — it never fails the run for this reason.

**Vitest (`ui` module) and Playwright (`e2e` target):** the bundled HTML
reporters for both runners are ES-module SPAs that require an HTTP server to
open (Vitest needs `vite preview`, Playwright needs `npx playwright
show-report`) and often embed absolute container or host paths, so they
cannot be opened as `file://` reliably. The skill avoids both problems by
**not relying** on those HTML reporters: Vitest is configured (in
`crbpm/ui/vite.config.js`) to emit JUnit XML, Playwright is configured (in
`crbpm/ui/playwright.config.js`) to emit JUnit XML alongside its built-in
HTML, and the orchestrator builds its own self-contained reports:

1. Vitest writes `crbpm/ui/build/test-results/test/junit.xml` inside the
   container; the orchestrator copies it under
   `tmp/dood-tests/latest/ui/test-results/test/junit.xml`.
2. `scripts/generate-junit-html-report.ps1` (Windows) /
   `scripts/generate-junit-html-report.py` (Linux/macOS, via `python3`)
   parses that XML and writes a **single-file** `tmp/dood-tests/latest/ui/
   report.html` with inline CSS/JS (search box, status pills, failure
   stack traces, expand/collapse).
3. The orchestrator opens that `report.html` directly via the system file
   association — no server, no port, fully offline.

To regenerate the page manually (e.g. on a different machine that received the
`tmp/dood-tests/` folder):

```powershell
.\.cursor\skills\doexecute-test-dood\scripts\generate-junit-html-report.ps1 `
  -JunitFile tmp\dood-tests\latest\ui\test-results\test\junit.xml `
  -OutputFile tmp\dood-tests\latest\ui\report.html `
  -Title 'Vitest (ui) - DooD run'
```

```bash
python3 .cursor/skills/doexecute-test-dood/scripts/generate-junit-html-report.py \
  --junit tmp/dood-tests/latest/ui/test-results/test/junit.xml \
  --output tmp/dood-tests/latest/ui/report.html \
  --title 'Vitest (ui) - DooD run'
```

The same flow is applied to `e2e/test-results/e2e/junit.xml` after a
Playwright run — the orchestrator writes `tmp/dood-tests/latest/e2e/report.html`
with the same look and feel. Pass `--title 'Playwright (e2e) - DooD run'` if
you regenerate it manually.

The generator is reusable on any standard JUnit XML (Gradle, Surefire,
Playwright, …) if you ever want a single-file viewer for another source.

## Why not the IntelliJ test runner?

IntelliJ's **Run / Test Results** tool window is the nicest UI for navigating
test results, but it cannot be populated from a DooD run today:

- The JetBrains MCP server exposes file/build/run tools but **no
  `import_tests_from_file` action** (as of 2026-05). `open_file_in_editor`
  on a `TEST-*.xml` opens it as text, not in the test runner.
- IntelliJ's manual **Run → Import Tests from File…** expects an XML format
  produced by an IntelliJ-run test session, not the standard Gradle /
  Surefire `TEST-*.xml` we produce. Importing those typically yields
  *"Test framework quit unexpectedly"*.
- To populate the test runner natively, tests must be **launched from the
  IDE** (e.g. `idea-execute_run_configuration` on a Gradle test config),
  which defeats the purpose of running them in DooD.

The skill therefore picks the system browser for the rich UI and uses MCP
only for the auxiliary navigation described next.

## Opening reports via the JetBrains MCP server

The skill assumes the **JetBrains IntelliJ MCP server is connected** (tools
prefixed `idea-*` are available). The system browser is the primary report
viewer (see [Opening reports in the default browser](#opening-reports-in-the-default-browser));
MCP is used for the supporting actions that *do* benefit from being inside
the IDE.

### Decision rules

1. **Always open the run summary first** so the user sees module-level exit
   codes:

   ```
   idea-open_file_in_editor(filePath = "tmp/dood-tests/latest/summary.txt")
   ```

2. **Do not** re-open the HTML reports via MCP — the orchestrator script has
   already opened them in the default browser. Re-opening them as text in
   the IDE only clutters the editor tabs.

3. **On Gradle failure** (non-zero `Gradle exit` in `summary.txt`), open the
   **first JUnit XML** of each failing module so IntelliJ shows the stack
   trace inline and you can jump to the failing class:

   ```
   idea-find_files_by_glob(
     globPattern = "tmp/dood-tests/latest/<module>/test-results/test/TEST-*.xml"
   )
   # then for each path returned:
   idea-open_file_in_editor(filePath = "<that path>")
   ```

   Prefer XML files whose `<testsuite>` element has `failures > 0` or
   `errors > 0`. A quick `idea-search_in_files_by_regex` with
   `<testsuite[^>]*(failures|errors)="[1-9]` over
   `tmp/dood-tests/latest/<module>/test-results/test` narrows the list when
   there are many.

   **On Camunda live failure** (non-zero `Camunda live` in `summary.txt`), open
   `tmp/dood-tests/latest/camunda-live/backend/reports/c8Test/index.html` in the
   browser (the orchestrator does this automatically unless `-NoBrowser`), and
   inspect `tmp/dood-tests/latest/camunda-live/backend/test-results/c8Test/`
   for failing `TEST-*.xml` files.

   **On e2e failure** (non-zero `E2E exit`), open
   `tmp/dood-tests/latest/e2e/test-results/e2e/junit.xml` — Playwright emits
   a single JUnit aggregate per run, not one file per suite. The matching
   screenshot/video/trace for each failing test sits under
   `tmp/dood-tests/latest/e2e/artifacts/<spec>-<test-id>/`.

4. **Do not open** the timestamped sibling (`tmp/dood-tests/<stamp>/`) —
   `latest/` always mirrors it and keeps editor tabs stable across re-runs.

### Project path

All `idea-*` calls in this repo should pass
`projectPath = "C:/work/src/maers-brt-lug"` (or the equivalent for the host
in use) to avoid ambiguous-call resolution, per the JetBrains MCP guidance.

### When the MCP server is not connected

If the `idea-*` tools are not available in the current session, the browser
opening done by the script still works. The agent should additionally
**print the paths** so the user can open them manually:

- `tmp/dood-tests/latest/summary.txt`
- `tmp/dood-tests/latest/<module>/reports/test/index.html`
- `tmp/dood-tests/latest/<module>/test-results/test/*.xml` (raw JUnit XML)

Never try to invoke `idea-*` tools speculatively — only when they are listed
as available.

## Image definition

The temporary image is built from `scripts/Dockerfile.dood-tests`, which
provides:

- `eclipse-temurin:21-jdk` (matches the Java 21 toolchain pinned in
  `gradle/libs.versions.toml`).
- Node.js 20 + npm for the `ui` module's Vitest run.
- Docker CLI for DooD.
- `tini` as PID 1 so `Ctrl+C` cleans up the Gradle / npm processes.

If a `Dockerfile.dood-tests` already exists at the **repo root** the script
uses that one instead, so teams can customise the runtime (extra system
packages, mirror registry, etc.) without forking the skill.

## Troubleshooting

- **"Cannot connect to the Docker daemon"** → start Docker Desktop, or on
  Linux ensure the current user is in the `docker` group.
- **Testcontainers cannot reach Postgres** → confirm the container was started
  with `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`; this is set by the
  orchestrator by default.
- **Reports missing for a module** → check `tmp/dood-tests/latest/summary.txt`
  for that module's exit code; the Gradle test task may have failed before
  producing reports.
- **Image build is slow on first run** → expected (downloads JDK + Node).
  Subsequent runs reuse the cached image; pass `-Rebuild` only when the
  Dockerfile itself changes.
- **`idea-open_file_in_editor` returns "project not found"** → pass
  `projectPath` explicitly (e.g. `C:/work/src/maers-brt-lug`). If the IDE has
  multiple projects open, the MCP server requires this hint.
- **Reports not appearing in IntelliJ tabs** → check that the JetBrains MCP
  plugin is enabled in the IDE and that the agent sees `idea-*` tools in this
  session; otherwise the skill prints fallback paths.
- **Browser does not open** → the script logs `Could not open …` (Windows) or
  `No browser opener found …` (Linux) but does not fail the run. Pass
  `-NoBrowser` / `--no-browser` if this is by design (CI), or open the HTML
  manually from `tmp/dood-tests/latest/<module>/reports/test/index.html`
  (Gradle modules) or `tmp/dood-tests/latest/ui/report.html` (Vitest).
- **`ui/report.html` missing after a DooD run** → on Windows the generator
  needs PowerShell 5.1+ (always present); on Linux/macOS it needs `python3`
  on `$PATH`. If absent, install it or regenerate the file later from
  `tmp/dood-tests/latest/ui/test-results/test/junit.xml`.
- **`e2e/report.html` missing after an e2e run** → check `summary.txt`:
  if `E2E exit` is non-zero and `playwright.config.js` did not run the
  `junit` reporter (e.g. `e2eUp` failed before reaching `e2eTest`), the
  XML is absent. The script logs `Generate-JUnitHtmlReport` failure but
  does not crash; inspect `tmp/dood-tests/latest/e2e/artifacts/` and the
  Gradle output for the underlying error.
- **`contrib` fails on `Could not find com.moodys.crbpm:connector-handler-spi`**
  → the SPI publish step did not run or failed. The `contrib` step publishes it
  to `mavenLocal()` in the same container before `crbpm-connectors/gradlew test`;
  check the Gradle output of `:connector-handler-spi:publishToMavenLocal` at the
  top of that step.
- **`DockerRegisterAgentSmokeIT` cannot POST to the register API** → the test
  starts the register-agent image as a **sibling** container. When it runs inside
  DooD, the QuarkusTest port only exists in the test container's network
  namespace, so the agent joins that namespace (`--network container:<id>`) and
  uses `http://localhost:<test-port>`; on a plain host it uses
  `host.docker.internal` + `--add-host …:host-gateway`. If the container id
  cannot be resolved by the daemon (custom `--hostname`), the test falls back to
  the host gateway and the POST will fail: re-run without overriding the
  container hostname.
- **`camunda-live` / `:infra:camundaSmoke` times out** → the Camunda stack can
  take several minutes on first pull. Retry with `-CamundaSmokeWaitMs 180000` /
  `--camunda-smoke-wait-ms 180000`, ensure ~8 GB RAM is free, and check
  `docker compose -f docker-compose.yml -f docker-compose.c8.yml ps` under
  `crbpm/docker/`.
- **`ZeebeDeployLiveIT` connection refused on 18082** → confirm
  `camunda-adapter` is healthy (`curl http://localhost:18082/health`) and
  that nothing else binds port 18082.
- **`ZeebeConnectorExecutionLiveIT` skipped or connectors unreachable on 18083** →
  the orchestrator starts the connectors standalone stack and the adapter worker
  overlay before the live ITs and exports `CRBPM_CONNECTOR_EXECUTION_LIVE=true`.
  If `crbpm-connectors-standalone` exits with
  `Schema-validation: missing table [connector_execution_audit]`, the connectors
  persistence unit is still mapping Backend entities. `quarkus.hibernate-orm.packages`
  must be `com.moodys.crbpm.connectors`. Do not switch schema management to `update`.
  Check the `docker compose … up` line in the run log.
- **`ZeebeStatusSyncLiveIT` callback failures** → live ITs must run on the
  **host** (not inside DooD) so Connectors can reach the backend test port on
  `host.docker.internal:18081`. Do not run `camunda-live` manually inside the
  DooD container.
- **`e2eUp` fails with `crbpm-e2e-ui is unhealthy`** → the compose stack
  uses `wget http://127.0.0.1/health.json` for the UI healthcheck (BusyBox
  `wget` resolves `localhost` to IPv6; nginx only listens on IPv4). Make
  sure `crbpm/docker/docker-compose.e2e.yml` still pins `127.0.0.1`.
- **`e2e` host port collisions** → the override binds 15173/18080/18081/
  18083/15432 on the host. Run `:ui:e2eDown` to free them, or stop any
  container already listening on these ports.
- **`e2eUp` fails with `crbpm-e2e-connectors exited` / missing catalog
  tables** → the E2E Postgres image must include Connectors migrations
  (`V028`–`V030`). Rebuild with `.\gradlew :ui:e2ePostgresBuild` (or a full
  `:ui:e2e`) so `crbpm-e2e-postgres` is current; do not mount only Backend
  migrations into `/docker-entrypoint-initdb.d`.
- **`Engine-neutral` check failed** → inspect the console output for
  `file:line:` hits. Allowed exceptions: Flyway scripts under
  `db/migration/`; `camunda.org` / `zeebe.io` string literals in
  `BpmnVendorExtensionValidator` only. Re-run manually:
  `.\.cursor\skills\doexecute-test-dood\scripts\check-engine-neutral-core.ps1
  -CrbpmRoot crbpm` (Windows) or
  `bash .cursor/skills/doexecute-test-dood/scripts/check-engine-neutral-core.sh crbpm`
  (from the repo root).
- **Stale `http://127.0.0.1:4175/` browser tab from an older run** → this
  skill no longer starts a `vite preview` server. The orchestrator now kills
  any leftover PID recorded in `tmp/dood-tests/.vitest-preview.pid` before
  opening the new static report; close the obsolete tab.

## Scope guarantees (per `AGENTS.md`)

- Suite-coverage patches belong to `run-tests-dood`; this skill only executes.
- This skill **does not** modify project sources, dependencies, or CI config
  beyond one-off, pre-discussed reporter changes in `crbpm/ui/vite.config.js`
  (Vitest reporters: `default` + `junit` only) and `crbpm/ui/playwright.config.js`
  (`list` + `html` + `junit` reporters), so the static report generator has
  a stable input. No further config edits happen at run time.
- It **does not** install anything on the host outside Docker, except via
  the `camunda-live` and `e2e` targets which delegate to Gradle on the host
  (`:infra:camundaSmoke`, `:backend:c8Test`, `:ui:e2e`; npm install / Playwright
  install for e2e only).
- It **does not** stage or commit anything; the working tree is read inside
  the container only.
- Host-side side-effects are limited to:
  - launching the default browser on `tmp/dood-tests/latest/<module>/reports/
    test/index.html` and `tmp/dood-tests/latest/e2e/report.html` (opt-out
    via `-NoBrowser` / `--no-browser`);
  - for the `camunda-live` target: running `:infra:camundaSmoke`,
    `:backend:c8Test` with `CRBPM_CAMUNDA_LIVE=true`, and tearing down via
    `:infra:camundaAdapterDown` + `:infra:camundaDown` on the host (Camunda 8
    compose overlay + adapter);
  - for the `e2e` target only: running `./gradlew :ui:e2e` on the host,
    which rebuilds api/backend/ui/connectors/camunda-adapter images plus
    `crbpm-e2e-postgres`, and brings up / tears down the `crbpm-e2e`
    docker-compose stack (volume `crbpm_e2e_postgres_data` is destroyed by
    the matching `down -v`);
  - opening report files in IntelliJ through the JetBrains MCP server
    (`idea-open_file_in_editor`, `idea-find_files_by_glob`,
    `idea-search_in_files_by_regex`). No code edits, no run-config changes,
    no builds are triggered via MCP.

If the user asks for behaviour beyond running tests (e.g. "also publish the
report somewhere", "also rebuild the dev image"), confirm before extending
the orchestrator.
