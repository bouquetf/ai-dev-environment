---
name: solid-review
description: >-
  Reviews already-written CRBPM code for SOLID design, human readability and
  duplication — Java backend modules and the JavaScript/TypeScript/React
  frontend — combining IntelliJ diagnostics with custom PSI inspections and
  ESLint-based frontend metrics, and writes a numbered findings report without
  touching a single source file. Use when the user explicitly asks for a SOLID
  review, a design or quality pass, a readability review, a clean-code pass on
  the UI, or duplication detection over recent changes.
disable-model-invocation: true
---

# SOLID review (CRBPM)

Second pass over code that **already works**. This skill is **read-only**: it produces a report and changes nothing. Applying the report is the job of the **`solid-refactor`** skill.

Invoked explicitly by the user only.

## Workflow

- [ ] 1. Agree on the reviewed file set.
- [ ] 2. Collect IntelliJ diagnostics per file.
- [ ] 3. Run the structural analysis: PSI inspection for Java, ESLint metrics for the frontend.
- [ ] 4. Run the duplication analysis for both languages, plus targeted searches.
- [ ] 5. Apply the manual checklist that tools cannot cover.
- [ ] 6. Write the report, then summarize it in chat.

Both language lanes run whenever the reviewed set contains files of that
language: Java and Kotlin sources take the PSI lane, `.js` / `.jsx` / `.mjs` /
`.cjs` / `.ts` / `.tsx` sources take the frontend lane. A review that touches
`crbpm/ui/` and skips the frontend lane is incomplete.

## 1. Scope: locally modified files

Default scope is the **uncommitted local work**, never the whole branch and never the whole repository. Collect the union of these three sets from the repo root:

```powershell
git diff --name-only --diff-filter=ACMR           # modified, not staged
git diff --cached --name-only --diff-filter=ACMR  # staged
git ls-files --others --exclude-standard          # new, untracked
```

`--diff-filter=ACMR` drops deletions. Untracked files matter most: a brand-new class carries the highest design risk and appears in none of the `git diff` outputs.

Then:

1. Files or modules named by the user always win over the git-derived set.
2. Keep production sources: `crbpm/*/src/main/java/**` for the backend, `crbpm/ui/src/**` (`.js`, `.jsx`, `.mjs`, `.ts`, `.tsx`) for the frontend. Include tests — `*.test.jsx`, `crbpm/ui/e2e/**` — only if the user asks. Skip `build/`, `dist/`, `node_modules/`, generated output, resources, SQL migrations and docs.
3. Widen past the local set **only on explicit request** (for example "review the whole branch"): resolve the base with `git merge-base origin/HEAD HEAD`, falling back to `origin/main` then `origin/master` — a bare `main` often does not exist locally. Announce the resulting file count and confirm before analyzing.
4. More than ~25 files after filtering: list the modules with their file counts and ask which to prioritize, rather than reviewing everything shallowly.

State the final file list in chat before analyzing.

### Findings must target the local change

Read whole files for context, but only raise findings on code the local change **introduced or modified**. A pre-existing smell is reportable only when the local change makes it worse — for example a third branch added to a conditional chain that was already too long. Mark those findings `Pre-existing: yes` so the refactor pass knows they were not part of the current work and needs explicit approval before touching them.

## 2. IntelliJ diagnostics

Run `idea-get_file_problems` (`errorsOnly: false`, `projectPath` = repo root) on every reviewed file, frontend files included — IntelliJ analyzes `.js` / `.jsx` / `.ts` / `.tsx` too.

Keep what points at design or comprehension problems: unused members, redundant conditions, nullability, mutability leaks, `Uni`/`Multi` misuse, unreachable branches. On the frontend, the useful ones are React hook dependency warnings, ignored promises, shadowed or unused bindings and unreachable branches. Drop pure formatting noise — it is not what this review is for.

## 3a. Structural SOLID smells (Java)

Read [scripts/solid-structure.kts](scripts/solid-structure.kts) and pass its full content to `idea-run_inspection_kts` with `contextPath` set to the reviewed file, once per file.

| Signal | Default threshold | Usual reading |
|---|---|---|
| Instance fields | > 6 | Class juggles several responsibilities (SRP) |
| Public methods | > 15 | Surface too wide, split the interface (SRP/ISP) |
| Top-level statements per method | > 25 | Extract intention-revealing methods |
| Parameters | > 5 | Introduce a parameter object |

Thresholds are candidate detectors, not verdicts. Always report the threshold that triggered a finding, and say when a breach is legitimate (for example a DTO or an entity with many fields is not an SRP violation).

## 3b. Structural smells (JavaScript / TypeScript / React)

The `inspection.kts` API only sees Java and Kotlin PSI, so frontend structure is measured with core ESLint metric rules from [scripts/frontend-structure.eslint.config.mjs](scripts/frontend-structure.eslint.config.mjs). Run it from `crbpm/ui`, never with `--fix`, passing the reviewed frontend files:

```powershell
npx eslint --no-config-lookup `
  --config ../../.cursor/skills/solid-review/scripts/frontend-structure.eslint.config.mjs `
  src/pages/SomePage.jsx src/utils/someHelper.js
```

The config carries no imports, so it needs nothing beyond the ESLint already installed in `crbpm/ui/node_modules`. Every rule is a warning: the command exits 0 and the findings are in its output. Prerequisite: Node on the host and the UI dependencies installed (`npm install` from `crbpm/ui`). If neither is available, record it on the report's `Tooling` line instead of dropping the lane silently.

| Signal | Rule and default threshold | Usual reading |
|---|---|---|
| Component or function too long | `max-lines-per-function` > 120 | Extract a subcomponent or a custom hook |
| Statements per function | `max-statements` > 25 | Too much orchestration in one place |
| Branching | `complexity` > 15 | Split the conditional logic out of the component |
| Nesting depth | `max-depth` > 3 | Invert guards, extract helpers |
| Callback pyramid | `max-nested-callbacks` > 3 | Name the intermediate steps |
| Stacked conditionals | `no-nested-ternary` | Extract a named helper returning the value |
| Wide signature | `max-params` > 5 | Introduce an options object |
| Module too large | `max-lines` > 400 | Module accumulating unrelated exports (SRP at file level) |

The `complexity` budget is deliberately wider than the Java one because JSX conditional rendering counts toward it.

Also run the project's own linter once for correctness rules the metric config does not carry (React hook rules, unused bindings, `react-refresh`):

```powershell
npm run lint
```

It lints the whole UI module and currently reports a small pre-existing baseline (`no-undef` in Playwright specs and `playwright.config.js`, `react-refresh/only-export-components`, one `react-hooks/exhaustive-deps`), so it exits non-zero even on a clean change set. Keep only the findings that fall inside the reviewed files, and treat the rest as context.

**TypeScript**: `.ts` / `.tsx` need a TypeScript-aware parser, which is not a `crbpm/ui` dependency today (the module is JavaScript + JSX). If TypeScript sources appear in the reviewed set, say so in the report, review them with the duplication script and the manual checklist, and flag the missing `typescript-eslint` devDependency as a prerequisite instead of silently skipping the metrics.

## 4a. Duplication (Java)

IntelliJ's *Locate duplicates* analysis is not exposed over MCP, so use [scripts/duplicated-method-bodies.kts](scripts/duplicated-method-bodies.kts): replace the `comparedFiles` placeholders, then run it via `idea-run_inspection_kts` once per reviewed file. It fingerprints method bodies of at least 4 statements after stripping comments and whitespace, so it catches exact structural copies only.

Fill `comparedFiles` with the **locally modified set plus the untouched siblings from the same packages**. Those siblings are comparison targets only — never review or edit them; their purpose is to catch the most common case, new local code re-implementing logic that already exists next to it.

## 4b. Duplication (JavaScript / TypeScript / React)

Use [scripts/duplicated-js-blocks.mjs](scripts/duplicated-js-blocks.mjs) from the repo root. It normalizes each file into significant lines (comments, blank lines and punctuation-only lines dropped, whitespace collapsed) and reports maximal blocks of identical normalized lines, inside a file or across files. It has no dependencies and is line-based, so it covers `.js`, `.jsx`, `.ts` and `.tsx` alike.

```powershell
node .cursor/skills/solid-review/scripts/duplicated-js-blocks.mjs `
  crbpm/ui/src/pages/SomePage.jsx crbpm/ui/src/pages/SiblingPage.jsx
```

Pass the reviewed frontend files **plus untouched siblings from the same folders** as read-only comparison targets, exactly as for the Java lane. `--min-lines <n>` (default 6) lowers or raises the block size; drop to 4 when hunting small copied hooks or handlers. Identifiers are compared as written, so only exact copies are reported.

Near-duplicates need targeted searches — `idea-search_text`, `idea-search_regex`, `idea-search_symbol` (scope the results back to the locally modified files):

- repeated `if`/`switch` chains over the same enum or category (an open/closed violation waiting for a strategy);
- repeated literals: error codes, header names, timeout defaults, SQL fragments; on the frontend also `WRP-*` codes, route paths, `data-testid` values and poll intervals;
- copied mapping or builder blocks between DTO, entity and domain types; on the frontend, payload-shaping and label-formatting blocks that belong in `src/utils/`;
- a helper reimplemented locally while an existing utility or port already does it — check `src/utils/`, `src/hooks/` and `src/api/backendApi.js` before accepting a new local helper;
- near-identical action handlers differing only by the `backendApi` call and the success message (a candidate for one parameterized handler).

Duplication is only a finding when the two copies encode the **same rule** and must change together. Similar-looking code serving different rules must stay separate.

## 5. Manual checklist

Design (classes on the backend, modules / components / hooks on the frontend):

- **SRP** — one reason to change per unit; mixed concerns (HTTP mapping + persistence + business rule in one method) are the most common breach here.
- **OCP** — new behavior should plug in, not edit a chain. This codebase already offers extension seams: engine plug-ins behind `engine-spi`, `ConnectorHandler` / `handlerId` registry, callback ports. Prefer those over growing conditionals.
- **LSP** — implementations must honor the contract: no surprise `UnsupportedOperationException`, no silently narrower behavior.
- **ISP** — narrow, role-specific interfaces; flag god interfaces forcing empty implementations.
- **DIP** — depend on abstractions the module owns. In particular: the engine-neutral core must not import engine-specific types.

Readability:

- Descriptive, fully spelled variable names (repo rule): `registrationResponse`, not `r`.
- Method name matches what the body actually does, including side effects.
- Nesting depth over 3, or boolean parameters steering control flow.
- Comments narrating the code instead of explaining a constraint; comments citing tickets (forbidden by repo policy).
- Error handling: CRBPM error codes `WRP-{AREA}-{NNNN}`, `traceId` propagated, no swallowed failures.

JavaScript / TypeScript / React (metrics catch size, not these):

- **Presentation vs logic** — a page component that fetches, derives state, maps payloads and renders is four responsibilities. Data shaping belongs in `src/utils/`, request plumbing in `src/api/`, reusable stateful behavior in a `use*` hook under `src/hooks/`.
- **Hooks discipline** — hooks called unconditionally, dependency arrays complete and honest (never trimmed to silence a warning), effects with a cleanup when they subscribe, poll or start a request, `useMemo` / `useCallback` where a stable identity is actually required by a dependency array rather than sprinkled for show.
- **State shape** — one source of truth: a value derivable from props or existing state should be derived, not mirrored in another `useState` that can drift.
- **Component API** — few, named props; flag boolean props steering unrelated rendering paths, prop drilling through several levels, and props objects rebuilt inline on every render.
- **Error and empty states** — user-facing messages built from the backend envelope (`status`, `payload.code`, `payload.message`) with a real fallback, never a raw exception or a silent `catch {}`. Error-code branches must match the codes the backend actually returns.
- **Backend contract** — components call the `backendApi` client, not `fetch` directly; no hardcoded ports or absolute service URLs.
- **Naming and formatting** — same repo rules as Java: fully spelled names (`registrationResponse`, not `r`), English only, no ticket references in comments, no commented-out code left behind.
- **Rendering hygiene** — list keys stable and derived from data identity rather than the array index alone; no side effect during render.
- **Tests** — Vitest specs assert user-visible behavior through Testing Library queries rather than internals; a component that can only be tested by reaching into implementation details is usually badly split.

CRBPM-specific:

- Reactive and non-blocking on the backend: no blocking call on the event loop, no `await`-style blocking in request paths.
- English-only in code, comments and messages outside `tmp/`.
- Ports and adapters kept separate from domain logic.

## 6. Report

Write to `tmp/solid-review/<yyyyMMdd-HHmm>-<scope>.md` and copy it to `tmp/solid-review/latest.md` (`tmp/` is gitignored). Keep the numbered `SR-NNN` identifiers stable — `solid-refactor` consumes them.

```markdown
# SOLID review — <scope>

Scope: local uncommitted changes (<count> files) | <or: explicit file list / branch, on user request>
Reviewed files: <count> (<n> Java, <n> frontend) | Blockers: <n> | Major: <n> | Minor: <n>
Tooling: idea-get_file_problems, solid-structure.kts, duplicated-method-bodies.kts, frontend-structure.eslint.config.mjs, duplicated-js-blocks.mjs, npm run lint
<Name any lane that could not run and why — a skipped lane is a gap in the review, not a silent detail.>

## Reviewed files
<One project-relative path per line, so the refactor pass can check the set is still current.>

## Summary
<Two or three sentences: overall design health, the one thing worth fixing first.>

## Findings

### SR-001 — <short title>
- Severity: blocker | major | minor | nit
- Principle: SRP | OCP | LSP | ISP | DIP | readability | duplication | hooks | state | component API
- Location: `path/to/File.java:120-168` | `crbpm/ui/src/pages/SomePage.jsx:51-120`
- Pre-existing: no | yes — <how the local change makes it worse>
- Evidence: <inspection message, duplicate counterpart, or quoted code>
- Why it matters: <concrete consequence: what breaks or slows down later>
- Proposed change: <smallest behavior-preserving refactor>
- Blast radius: <files, public API, tests likely touched; flag any git-clean file>
- Decision: pending

## Deliberately accepted
<Threshold breaches that are fine, with the reason — prevents re-litigating them next review.>
```

Severity: **blocker** breaks a repo rule (blocking call in reactive path, engine type in neutral core, French in source); **major** will cost real time on the next change; **minor** is a local cleanup; **nit** is preference.

In chat, give the report path, the counts, and the top three findings in prose. Do not paste the whole report.

## Guardrails

- Never edit source in this skill, not even an obvious one-liner. Report it. On the frontend that also means never running ESLint with `--fix`.
- Stay inside the local change set. Untouched code is context, not review material — no repository-wide audit unless the user asks for one. `npm run lint` covers the whole UI module: keep only what falls inside the reviewed files.
- Review every language present in the change set. Java-only findings on a change that also touched `crbpm/ui/` means the review is half done.
- No finding without evidence — an inspection message, a duplicate counterpart, or quoted code.
- Do not invent rules: cite `AGENTS.md`, `CONTRIBUTING.md` or `docs/adr/` when a finding rests on a project rule.
- Architecture-level breaches (module boundaries, new abstraction layers) get flagged as needing an ADR, not a quick refactor.
- Empty result is a valid result: say the code is clean rather than manufacturing nits.
