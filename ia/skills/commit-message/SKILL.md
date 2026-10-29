---
name: commit-message
description: >-
  Proposes merge-ready Conventional Commit messages that follow this project's
  CONTRIBUTING.md rules, including the three-digit story reference footer. Use
  when asked for a commit message or to summarize staged or unstaged changes.
---

# Commit message (Diapofap / CONTRIBUTING.md)

## When this applies

Use this workflow when the user asks for a merge-ready commit message or a summary of staged or unstaged changes. Follow the repository-root `CONTRIBUTING.md`; do not invent project rules.

## Source of truth

Read `CONTRIBUTING.md` when the full wording is needed. The required pattern for a commit intended for `main` or `master` is:

```
<type>(<optional-scope>): <short description>

<body — optional, when the subject is not enough>

Refs: <three-digit story id>
```

- The footer references exactly one story in `docs/stories/<id>/`, for example `Refs: 001`; do not use Jira IDs or add multiple stories.
- Use an imperative, concise subject without a final period or noise such as “WIP”, “temp”, or “try again”.
- Choose the type by intent: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `build`, `ci`, or `perf`.
- Use a clear scope when applicable: `app`, `ui`, `domain`, `persistence`, `ml`, `gradle`, or `docs`; omit it when it would be vague or span unrelated areas.

## How to analyze changes

1. From the repository root, inspect `git status` and the relevant diff (`git diff --cached` for staged work or `git diff` for unstaged work). Summarize the behavior, areas touched, and whether the change is code, documentation, configuration, or generated output.
2. Infer the type and scope from the intent and affected project area.
3. Resolve the story ID without guessing:
   - Prefer a branch name matching `*/001-*` or `story/001-*`.
   - Otherwise use the story that clearly owns the work in `docs/stories/<id>/`.
   - If no single story can be identified, ask the user once for the ID. If the work has no story, ask the user to create or identify one before proposing the mandatory footer.
4. Add a body of one to three concise lines when the subject alone does not explain the rationale, migration, or generated-file update.

## Output to the user

Provide:

1. The recommended message in one fenced block, ready to use.
2. A brief rationale for the type, scope, and wording.
3. If the changes mix unrelated concerns or stories, recommend splitting them into separate commits rather than combining them.

Do not create a commit unless the user explicitly asks.

## Example

```
feat(ui): add menu-less Diapofap window chrome

Refs: 001
```

## Notes

- Keep one story per commit; follow `CONTRIBUTING.md` for the exact footer and story-ID rules.
- If the diff is mostly generated output, say so in the body and keep the subject accurate (for example, `chore(ui): refresh generated palette`).
