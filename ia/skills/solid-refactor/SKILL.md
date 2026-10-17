---
name: solid-refactor
description: >-
  Applies the accepted findings of a SOLID review to CRBPM code — Java backend
  modules and the JavaScript/TypeScript/React frontend — as small
  behavior-preserving refactors, verifying each step with IntelliJ diagnostics, a
  project build or the UI linter, the re-run inspections and the test suite. Use
  when the user explicitly asks to apply a SOLID review, execute the refactoring
  findings, clean up UI components, or make reviewed code SOLID-friendly.
disable-model-invocation: true
---

# SOLID refactor (CRBPM)

Execution half of the SOLID pass. Consumes the report produced by the **`solid-review`** skill and turns accepted findings into code.

Invoked explicitly by the user only. **Behavior must not change** — this skill refactors, it does not add or alter features.

## Workflow

- [ ] 1. Load the report and confirm the findings to apply.
- [ ] 2. Order them smallest-risk first.
- [ ] 3. Apply one finding at a time, verifying after each.
- [ ] 4. Run the tests for the touched modules.
- [ ] 5. Update the report decisions and summarize.

## 1. Input and confirmation

Read `tmp/solid-review/latest.md` (or the path the user gives). No report and no findings in the conversation: stop and ask the user to run `solid-review` first — do not improvise a review here.

Recompute the local change set before touching anything, and intersect it with the report's *Reviewed files*:

```powershell
git diff --name-only --diff-filter=ACMR
git diff --cached --name-only --diff-filter=ACMR
git ls-files --others --exclude-standard
```

Report a file that is no longer locally modified (committed, reverted or stashed since the review): the report is stale for that file — say so and skip its findings unless the user insists.

Then list the findings by `SR-NNN`, severity and one-line proposal, and **ask which ones to apply**. Default proposal: blockers and majors whose files are still locally modified. Never apply a finding the user has not accepted, never add findings that were not reviewed, and never silently promote a `Pre-existing: yes` finding — those need explicit approval since they expand the diff beyond the current work.

## 2. Ordering

Apply in this order so each step keeps the tree green:

1. Local, contained edits: extract method or helper function, rename, invert guard, remove dead code.
2. Extract collaborator or parameter object inside the same package; on the frontend, extract a subcomponent, a `use*` hook or a pure helper next to the component.
3. Deduplication: move shared logic into an existing utility or a new package-private helper (frontend: an existing module under `src/utils/`, `src/hooks/` or `src/api/`).
4. Seam changes: introduce or reuse an interface, wire a strategy behind an existing selector.

Anything beyond step 4 — module boundaries, new layer, changed public REST contract or persisted schema — **stop and ask**; it belongs to an ADR and a separate story, not to a refactor pass. On the frontend the same rule covers a changed component public API used by several pages, a new shared context or provider, a routing change, and any new npm dependency.

## 3. Applying a finding

For each accepted finding:

1. Read the full surrounding context, not just the reported lines.
2. Prefer IDE refactorings when available: `idea-rename_refactoring` for renames (it updates every reference), `idea-reformat_file` only on files already touched.
3. Keep the change **behavior-preserving**: same inputs, same outputs, same error codes, same HTTP statuses, same reactive contract (`Uni`/`Multi` in, `Uni`/`Multi` out — never introduce a blocking call). On the frontend: same rendered output, same accessible names and `data-testid` hooks, same request sequence, same user-facing strings.
4. Follow existing conventions: descriptive variable names, English only, no ticket references in comments, comments only for constraints the code cannot express.
5. Verify immediately — Java:
   - `idea-get_file_problems` on each edited file — no new warning;
   - `idea-build_project` (or `filesToRebuild` for a narrow change) — compiles;
   - re-run the relevant script from the review skill (`.cursor/skills/solid-review/scripts/solid-structure.kts`, `.cursor/skills/solid-review/scripts/duplicated-method-bodies.kts`) via `idea-run_inspection_kts` to prove the smell is gone.
6. Verify immediately — frontend (there is no compile step, so the linter and the specs are the safety net):
   - `idea-get_file_problems` on each edited file — no new warning;
   - `npm run lint` from `crbpm/ui` — no finding on the edited files, and no new finding elsewhere. The module carries a small pre-existing baseline (`no-undef` in Playwright specs and `playwright.config.js`, `react-refresh/only-export-components`, one `react-hooks/exhaustive-deps`), so capture the output **before** the first edit and compare, instead of expecting zero. Never reach a clean run by adding a disable comment or trimming a hook dependency array;
   - re-run the frontend scripts to prove the smell is gone: the metric config
     (`npx eslint --no-config-lookup --config ../../.cursor/skills/solid-review/scripts/frontend-structure.eslint.config.mjs <files>` from `crbpm/ui`, never `--fix`) and
     `node .cursor/skills/solid-review/scripts/duplicated-js-blocks.mjs <files>` from the repo root;
   - `npx vitest run <spec>` for the specs covering the touched components, as a fast loop before the module run in step 4.
7. If a change grows past its finding, revert to the last good state and report the finding as needing a plan instead.

Existing tests should keep passing **unchanged**. A test that must be modified to stay green usually means the behavior moved: stop and ask before touching it. This applies to Vitest and Playwright specs too — a selector or `data-testid` that has to move is a behavior change, not a refactor. Adding a test is only in scope when a newly extracted collaborator, hook or helper has genuinely testable logic of its own.

## 4. Verification

Run the test suite for the touched modules through the **`run-tests-dood`** skill (coverage gate then **`doexecute-test-dood`**), with targets limited to the affected modules (for example `-Targets connectors,backend`, or `-Targets ui` for a frontend-only refactor). Full verification only if the refactor crossed several modules or the user asks.

A refactor that changed rendering, routing or the pages exercised by Playwright also needs `-Targets ui,e2e`, since Vitest alone does not cover the real stack.

Report failures with the failing test names and whether they existed before the refactor. Never mark the pass complete on a red suite.

## 5. Output

1. Update each applied finding in the report: `Decision: applied` (or `deferred — <reason>`), plus the files touched.
2. In chat, state what changed and what it bought: the findings applied, the files touched, the verification result, and anything deliberately left out.
3. Offer a commit message via the **`commit-message-crbpm`** skill — type `refactor`, scope from the touched module, single `Refs: ATL-<id>` footer. Do not commit unless the user asks.

## Guardrails

- **Scope**: edit only files that are both covered by an accepted finding and already locally modified. Touching a git-clean file (needed, for instance, to deduplicate against existing code) requires explicit approval: name the file, say why, and wait. No drive-by cleanup of neighboring code, no dependency bumps, no reformatting whole files.
- **Keep the diff attributable**: at the end, `git status --short` must show the same files as before plus only the ones the user approved.
- **No behavior drift**: public API, error codes, database schema, configuration keys and log semantics stay as they are unless the user explicitly approves. On the frontend that includes rendered markup, user-facing text, `data-testid` values, routes and the request sequence hitting `backendApi`.
- **No over-engineering**: do not introduce an interface with a single implementation, a factory, or a generic abstraction just to satisfy a principle. On the frontend, do not split a component into a pile of one-line children, wrap everything in `useMemo` / `useCallback`, or add a context for state that one component owns. A refactor that makes the code harder to read has failed, whatever the acronym it serves.
- **Never silence the frontend tooling**: no `eslint-disable`, no removed hook dependency, no `?.` chain added just to make a warning disappear. Fix the cause or report the finding as needing a plan.
- **No dependency changes**: adding an npm package (a TypeScript parser, a lint plugin, a utility library) is out of scope for a refactor pass — propose it and wait.
- **Ask when ambiguous**: two reasonable designs, or a fix touching more than a handful of files — propose both briefly and let the user choose.
- **One concern per step**: keep the diff reviewable; suggest splitting into several commits when findings are unrelated.
