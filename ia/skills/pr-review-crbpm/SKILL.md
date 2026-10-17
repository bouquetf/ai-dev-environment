---
name: pr-review-crbpm
description: >-
  Advisory pull-request review for CRBPM in Cursor. Reads the diff against
  AGENTS.md, CONTRIBUTING.md, and the evolving checklist, then writes a French
  learning pack under tmp/pr-review (explain-diff, explain-topic, findings).
  Use when the user asks for a PR review, a branch review, a self-review
  before Ready for review, or names this skill.
disable-model-invocation: true
---

# Pull request review (CRBPM) — draft

Advisory second reader for this repository. The author stays accountable for the code and the description. This skill does not approve, merge, push, or edit source.

**Draft.** The checklist is expected to grow. When a review shows a missing rule or a repeated false positive, add one line under [checklist.md](checklist.md) → Evolution log. Do not invent a project rule to fill a gap.

## Sources, in this order

1. [AGENTS.md](../../../AGENTS.md) — local rules win over generic review advice.
2. [CONTRIBUTING.md](../../../CONTRIBUTING.md) sections 5, 6 and 8.
3. [checklist.md](checklist.md) — review passes for this skill.
4. The linked story or an ADR only when the diff's intent is unclear.

Regnology practice this mirrors: the in-house PR review agent reads `AGENTS.md` on the branch first and treats it as higher priority than global standards. Human review still gates the merge. See the source list in the checklist.

## Teaching deliverable

The reader is learning the change, not only judging it. Every review writes a French pack under `tmp/` (the only place French notes belong):

1. Read and follow [explain-diff](../explain-diff/SKILL.md). One file, four parts: background, intuition before details, interactive figures, code walk.
2. Read and follow [explain-topic](../explain-topic/SKILL.md) once per concept that file lists. Same four-part teaching shape, scoped to the subsystem or workflow.

Do not skip these files because the diff looks small. A short change still gets a short note.

## Workflow

- [ ] 1. Resolve the target.
- [ ] 2. Collect the diff and metadata.
- [ ] 3. Read the sources above.
- [ ] 4. Write `explain-diff.md` by following `explain-diff`.
- [ ] 5. Write one `topics/<topic>.md` per concept by following `explain-topic`.
- [ ] 6. Review in the checklist passes.
- [ ] 7. Write the findings report, then summarize in chat.

### 1. Target

A pull-request URL or id wins. Otherwise review the current branch against its merge base:

```powershell
git fetch origin
git merge-base origin/main HEAD
git diff --stat $(git merge-base origin/main HEAD)...HEAD
git log --oneline $(git merge-base origin/main HEAD)..HEAD
```

State the range, the commit count, and the file count before the deep read. Above roughly 400 changed lines excluding lockfiles, snapshots, and generated output, still review, and record the size as a readiness finding unless the description justifies it.

Files or modules named by the user narrow the pass. Do not widen to the whole repository.

### 2. Collect

Use facts, not the PR title alone.

- **Bitbucket** (`regnology-mcp`, after `GetDynamicTools`): `bb_get_pr` (set `includeDiff` when the local checkout is not the PR head), `bb_get_pr_commits`, `bb_get_pr_builds`, `bb_get_pr_comments`, `bb_get_pr_merge_status`.
- **GitHub**: `gh pr view`, `gh pr diff`, `gh pr checks`.
- **Jira** (`jira_get_issue`): the single `ATL-` key from the branch name or the PR body. Read the story to judge intent. Do not paste client data, tokens, or production dumps into the review.
- **Local diff**: `git diff <base>...HEAD` for the patch; read surrounding code when a hunk is not understandable alone.

Note CI: build, unit tests, lint, integration tests, security scans. A red or missing check is a finding, not something this skill re-runs unless the user asks.

### 4. explain-diff

Follow [explain-diff](../explain-diff/SKILL.md) in full. Write `explain-diff.md` in the pack directory before looking for defects. The four headings are fixed: background, intuition before details, interactive figures, code walk.

### 5. explain-topic

Follow [explain-topic](../explain-topic/SKILL.md) for each concept named at the end of `explain-diff.md`. One file per concept under `topics/`. The four headings are fixed: introduction au topic, intuition before details, interactive figures, code.

### 6. How to read

Review only what the change introduces or modifies. A pre-existing issue is reportable when this change makes it worse. Mark those `Pre-existing: yes`.

Each finding needs evidence: a quoted line, a failed check, or a rule citation (`AGENTS.md`, `CONTRIBUTING.md`, `docs/adr/…`, or a checklist bullet). If a reference might not exist in the repo, verify it before commenting. Say when you are unsure.

Suggested code comments stay in English and do not cite Jira keys or task numbers.

For a deep SOLID or duplication pass, stop and point at the `solid-review` skill. Do not repeat that analysis here.

### 7. Report

Write the pack in one directory and mirror it to `tmp/pr-review/latest/`:

```text
tmp/pr-review/<yyyyMMdd-HHmm>-<slug>/
  explain-diff.md
  topics/<topic-slug>.md
  review.md
```

`explain-diff.md` and each topic file are French and follow the sub-skills. `review.md` is the findings list, also French, so the whole pack is readable together.

```markdown
# Revue — <titre ou branche>

Cible : <url ou base...HEAD>
Périmètre : <n commits, n fichiers, ~n lignes>
Lecture : [explain-diff.md](explain-diff.md)
Verdict : prêt | prêt avec remarques | pas prêt
Bloquants : <n> | Majeurs : <n> | Mineurs : <n> | Détails : <n>
Contrôles : <verts / rouges / inconnus — nommer ce qui manque>

## En bref
<Ce que le changement fait, s'il correspond au ticket, et la première chose à corriger. Renvoyer vers explain-diff pour le parcours complet.>

## Remarques

### PR-001 — <titre court>
- Sévérité : bloquant | majeur | mineur | détail
- Passe : préparation | correction | règles du projet | tests | sécurité | conception | hygiène
- Endroit : `path/to/File.java:120-140`
- Déjà là avant : non | oui — <en quoi ce changement l'aggrave>
- Preuve : <code cité, nom du contrôle, ou extrait du diff>
- Règle : <AGENTS.md / CONTRIBUTING.md / puce de la checklist>
- Pourquoi c'est gênant : <conséquence concrète>
- Piste : <plus petit changement, ou « à trancher »>

## Accepté tels quels
<Points examinés et laissés, avec la raison.>

## Non vérifié
<CI, navigateur, ou exécution que cette passe n'a pas pu lancer.>
```

Severity: **bloquant** breaks a merge gate or a hard repo rule; **majeur** is a likely defect or a missing test on new behaviour; **mineur** is a local cleanup; **détail** is preference. An empty finding list is valid — say the diff looks ready for a human reviewer.

In chat, give the directory path, the topic list, the counts, and the top findings. Do not paste the teaching notes.

## Guardrails

- Do not post a Bitbucket or GitHub comment, approve, or merge unless the user explicitly asks. Default deliverable is the French pack under `tmp/pr-review/`.
- Do not edit product source in this skill.
- Do not treat an AI comment (including this report) as approval. CONTRIBUTING still requires a non-author human approval and a maintainer merge.
- Do not force-push, rebase, or rewrite history from this skill.
- No finding without evidence. No invented API, helper, or project rule.
