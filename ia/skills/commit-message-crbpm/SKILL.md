---
name: commit-message-crbpm
description: >-
  Proposes commit messages for CRBPM that match CONTRIBUTING.md: Conventional
  Commits, optional scope aligned with modules, mandatory Jira footer. Use when
  the user asks for a commit message, conventional commit, or help summarizing
  staged/unstaged changes before commit; or when preparing a PR-ready message.
---

# Commit message (CRBPM / CONTRIBUTING.md)

## When this applies

Use this workflow whenever the user wants a **good, merge-ready** commit message for this repository. **Do not invent project rules**; follow [CONTRIBUTING.md](CONTRIBUTING.md) (section 5 — Commit messages).

## Source of truth

Read **`CONTRIBUTING.md`** at the repository root if you need the full wording. Minimum required pattern:

```
<type>(<optional-scope>): <short description>

<body — optional, use when the summary alone is unclear>

Refs: ATL-<id>
```

- **Footer is mandatory** for commits that will land on `main`: exactly one Jira story/bug/technical story ID (`Refs: ATL-xxxx`). **One ticket per commit** — never multiple ATL IDs in one commit message.
- **Subject line**: imperative mood, concise, no trailing period, **no** “WIP”, “temp”, “try again”, or other noise (see CONTRIBUTING §1).
- **Types**: Prefer `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `build`, `ci`, `perf` as appropriate. Match the **intent** of the diff, not the file extension alone.

## How to analyze changes

1. **Collect facts** (run from repo root):
   - `git status` — what is modified, staged vs unstaged.
   - For the set the user cares about (usually **staged**: `git diff --cached`; or **working tree**: `git diff`), summarize **what** changed: areas touched, behavior added/fixed, config vs code vs generated assets.
2. **Infer `type` and `scope`**:
   - **Scope**: Prefer CRBPM modules or areas when obvious: `api`, `backend`, `ui`, `connectors`, `infra`, `docker`, `gradle`, etc. Omit scope if it would be vague or split across many unrelated areas.
   - **Type**: e.g. new capability → `feat`; defect → `fix`; routine maintenance / tooling → `chore`; documentation only → `docs`; test-only → `test`; CI/build scripts → `ci` / `build`.
3. **Resolve `ATL-<id>`**:
   - Prefer the **current branch name** if it matches `*/ATL-<digits>-*`.
   - If missing and the user did not supply a ticket, **ask once** for the single ATL ID; do not guess.
4. **Body**: Add 1–3 short lines when the subject is not enough (rationale, migration note, or “why”).

## Output to the user

Provide:

1. **Recommended message** in a single fenced block (ready to paste into `git commit`).
2. **Brief rationale** (one short paragraph): what in the diff drove the type, scope, and wording.
3. If the change mixes **unrelated concerns** (e.g. feature + drive-by refactor), note that CONTRIBUTING expects **split commits** / separate PRs per ticket — suggest splitting instead of one overloaded message.

## Examples (shape only)

```
feat(ui): add process definition catalog to modeler palette

Refs: ATL-25203
```

```
fix(backend): validate engine id before reactive repository lookup

Prevent NPE when configuration omits default engine.

Refs: ATL-1560
```

## Notes

- **Consistency**: CONTRIBUTING examples use the footer line `Refs: ATL-xxxx` — use that form.
- **Generated files**: If the diff is mostly generated output, say so in the body and keep the subject accurate (e.g. chore(ui): refresh generated palette).
