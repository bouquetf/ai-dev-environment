---
name: run-tests-dood
description: >-
  Coverage gate for CRBPM DooD platform testing. Audits whether the sibling
  skill `doexecute-test-dood` executes **all available** automated test suites
  (Gradle modules under `crbpm/`, isolated `crbpm-connectors/` contribution
  build, host `camunda-live` / `e2e`, engine-neutral core check), patches
  `doexecute-test-dood` (SKILL.md + `scripts/run.ps1` / `run.sh`) when suites
  are missing, then **invokes** the (possibly updated) `doexecute-test-dood`
  skill to run the platform validation. Also runs a change-set relevance
  helper (`scripts/assess-test-relevance.{ps1,sh}`) to propose focused
  `-Targets` when the user scoped a feature. Use when the user asks to run
  tests in Docker / DooD / containerized / reproducible test env, or to
  validate the whole platform — this is the **default entry**. Prefer
  `doexecute-test-dood` alone only when the user explicitly skips the coverage
  gate ("execute only", "skip coverage check", "lance full direct").
---

# Run CRBPM tests in DooD (coverage gate → execute)

This skill **does not** run Gradle/Docker itself. It:

1. Checks that [`doexecute-test-dood`](../doexecute-test-dood/SKILL.md) covers
   every available automated suite.
2. **Modifies** that skill (docs + orchestrator scripts) when something is
   missing.
3. Then **follows** `doexecute-test-dood` (read its `SKILL.md` and run its
   `scripts/run.ps1` / `run.sh`) with the chosen targets.

## When this applies

Typical triggers: "run tests in docker", "DooD", "Docker-out-of-Docker",
"containerized tests", "reproducible test env", "tests dans un conteneur",
"valide toute la plateforme".

Waive the coverage audit (and invoke `doexecute-test-dood` directly) only if
the user explicitly says so (e.g. "skip coverage check", "lance full direct",
"execute only").

## Relationship to `doexecute-test-dood`

| Skill | Responsibility |
|-------|----------------|
| **`run-tests-dood`** (this) | Inventory available suites ↔ what `doexecute` runs; patch gaps; choose `-Targets`; hand off |
| **`doexecute-test-dood`** | DooD image, `run.ps1`/`run.sh`, report collection, browser/MCP open, full/fast execution |

Scripts for **execution** live under
`.cursor/skills/doexecute-test-dood/scripts/`.
Scripts for **this gate** live under
`.cursor/skills/run-tests-dood/scripts/` (`assess-test-relevance.*` only).

## Agent checklist (mandatory order)

```
- [ ] 1. Inventory available automated test suites in the repo.
- [ ] 2. Diff that inventory against what doexecute-test-dood actually runs
       (SKILL.md coverage table + scripts/run.ps1 + run.sh phases/targets).
- [ ] 3. If gaps: patch doexecute-test-dood (scripts + SKILL.md) so the missing
       suites are included in `full` / default (or a documented intentional omit).
- [ ] 4. Change-set / feature relevance (assess helper + conversation context)
       → choose `-Targets` (default: full + camunda-live + e2e).
- [ ] 5. Read and follow `.cursor/skills/doexecute-test-dood/SKILL.md`
       (invoke its orchestrator with the chosen targets).
- [ ] 6. Report: coverage conclusion (patched or already complete) + execution
       summary paths from doexecute.
```

Do **not** call `doexecute-test-dood/scripts/run.ps1` until steps 1–4 are done
(or the user waived the gate).

## Step 1 — Inventory available suites

Scan the working tree for automated suites that a full platform validation
should reach. At minimum:

| Source | How to detect |
|--------|----------------|
| Gradle modules under `crbpm/` with `src/test` (or `c8Test` / `stubTest` / npm `test`) | `crbpm/*/build.gradle.kts` + test source trees |
| Isolated contribution build | `crbpm-connectors/modules/*/src/test` + root `gradlew` |
| Host Camunda live ITs | `*LiveIT` under backend / engine-c8 gated by `CRBPM_CAMUNDA_LIVE` |
| Host Playwright e2e | `crbpm/ui/e2e/` + `:ui:e2e` |
| Engine-neutral core guard | grep policy documented in doexecute (scripts there) |
| New meta-targets / compose smokes | only if they are the **intended** regression gate (do not silently add `composeSmoke` — it is intentionally omitted; see doexecute SKILL) |

Also use conversation context (story, “we just added X”) when the inventory
must include a **new** suite that landed in the same change set.

## Step 2 — Compare to `doexecute-test-dood`

Read:

- `.cursor/skills/doexecute-test-dood/SKILL.md` — especially **Coverage
  inventory**, default targets, `full` phases, `contrib` / `camunda-live` /
  `e2e`
- `.cursor/skills/doexecute-test-dood/scripts/run.ps1` and `run.sh` —
  `Get-FullVerificationPhases` / phase task lists, `contrib` wiring, default
  `$Targets`

A **gap** exists when a suite from step 1 is **not** executed by the default
`full` (+ `camunda-live` + `e2e`) path and is **not** documented as an
intentional omit.

Examples of past gaps (pattern to spot):

- New Gradle module with tests never listed in `full-c8` / `wrapper-only`
- New isolated tree (e.g. `crbpm-connectors/`) not reachable from `/work/crbpm`
  tasks → needs a dedicated target like `contrib`
- New host-only meta-target missing from default `$Targets`

## Step 3 — Patch `doexecute-test-dood` when needed

When a gap is found, **update the execute skill** before running it:

1. Extend `scripts/run.ps1` and `scripts/run.sh` so the suite runs (phase task
   list, new meta-target, report collection, summary line, browser policy).
2. Update `doexecute-test-dood/SKILL.md` (coverage inventory, flags table,
   reports tree, troubleshooting).
3. Keep PowerShell and bash **in sync**.
4. Summarize the patch to the user in 2–4 lines (what was missing, what you
   wired).

Do **not** invent product tests under `src/test` as part of this skill unless
the user asked to add tests; this gate only ensures the **orchestrator** runs
suites that already exist.

If the only issue is “feature under discussion has no automated test yet”,
list the gap and ask whether to (1) add tests first, (2) run anyway, or
(3) narrow targets — same policy as before, owned here rather than in
doexecute.

## Step 4 — Change-set relevance (target choice)

After the skill-coverage audit, choose `-Targets` for the hand-off:

### Assess helper (optional but preferred for focused loops)

```powershell
.\.cursor\skills\run-tests-dood\scripts\assess-test-relevance.ps1
```

```bash
bash .cursor/skills/run-tests-dood/scripts/assess-test-relevance.sh
```

Optional: `-BaseRef origin/main`.

| Does | Does not |
|------|----------|
| List changed files | Decide if a feature is “done” |
| Map paths → modules + target hints | Replace the skill-coverage inventory (steps 1–3) |
| Heuristic name-match to nearby `*Test` / `*IT` | Invoke Docker / Gradle |
| Print `SuggestedTargets` | Auto-patch `doexecute-test-dood` |

### Target policy

- **Default:** `full` + `camunda-live` + `e2e` (full platform validation).
- **Focused loop:** only when the user scoped the area (e.g. “seulement
  connectors”) or confirmed a narrower `SuggestedTargets` list.
- Do **not** silently drop `camunda-live` / `e2e` from a default full run
  without stating it.
- Honour explicit user `-Targets`, but still report if they miss modules
  touched by the diff **or** if step 2 found skill-coverage gaps you just
  patched (those patches apply to future `full` runs regardless).

## Step 5 — Execute via `doexecute-test-dood`

1. **Read** `.cursor/skills/doexecute-test-dood/SKILL.md` and follow it.
2. Invoke the orchestrator from the repo root:

   ```powershell
   .\.cursor\skills\doexecute-test-dood\scripts\run.ps1
   ```

   ```bash
   ./.cursor/skills/doexecute-test-dood/scripts/run.sh
   ```

3. Forward any user flags (`-Targets`, `-GradleArgs`, `-Rebuild`, …).
4. **Do not** pass `-NoBrowser` / `--no-browser` unless the user asked.
5. After the run, open `tmp/dood-tests/latest/summary.txt` in the IDE
   (JetBrains MCP) per doexecute guidance.

## Step 6 — Reply shape

Keep the user reply short:

1. **Coverage:** “already complete” **or** “patched doexecute: …”
2. **Targets** chosen for this run
3. **Result** + `tmp/dood-tests/latest/` (and failing suites if any)

## Waivers

| User says | Agent does |
|-----------|------------|
| “skip coverage check”, “lance full direct”, “execute only” | Skip steps 1–3 (note waiver); still may use step 4 if useful; run doexecute |
| “skip e2e” / “seulement backend” | Keep coverage audit; narrow `-Targets` as requested |
| “no browser” / CI | Forward `-NoBrowser` to doexecute |

## Scope guarantees

- May **edit** `.cursor/skills/doexecute-test-dood/**` to wire missing suites.
- Does **not** silently rewrite product `src/main` or invent `src/test` coverage
  without user agreement.
- Does **not** commit or push.
- Execution side-effects (Docker, browser, host Gradle for live/e2e) are those
  of `doexecute-test-dood` after hand-off.
