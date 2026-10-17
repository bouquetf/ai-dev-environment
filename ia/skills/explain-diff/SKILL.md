---
name: explain-diff
description: >-
  Explains a CRBPM pull request or branch diff as a French teaching note for
  someone learning the change. Writes tmp/pr-review/.../explain-diff.md in four
  parts: background, intuition before details, interactive figures, code walk.
  Use when a PR review needs the explanatory document, or when the user asks
  to explain a diff, a PR, or what a branch changed.
disable-model-invocation: true
---

# explain-diff

Produce one French Markdown file that teaches the change. The reader is learning the work the way a junior would: what the code did before, what the PR is for, then the paths, then the implementation.

Write only under `tmp/` (gitignored). French is required in that file. Do not put this note under `docs/`.

Called by `pr-review-crbpm` after the diff is known, or alone when the user asks to explain a change.

## Output

`tmp/pr-review/<yyyyMMdd-HHmm>-<slug>/explain-diff.md`

`<slug>` is the PR id or a short branch name. Reuse the directory the review skill already created. If this skill runs alone, create the directory.

## Facts before writing

Resolve the same range as the review: PR head, or `git merge-base origin/main HEAD`.

- History **before** the change: `git log --oneline -20 <base> -- <touched paths>`
- Previous shape: `git show <base>:<path>` for the files the new behaviour hangs on. Read enough to describe the old flow. Do not paste whole files into the note.
- New shape: the diff and the current files. Verify every name you mention.

If a fact is not in the history or the diff, say it is unknown. Do not invent a previous design.

## Voice

- French, complete sentences, one idea per paragraph.
- Define a concept the first time it appears, in plain words, before using it as a known term.
- State what the code does. Skip comments about what it does not do.
- The second part contains no code. The fourth part may cite short regions with line numbers.

## Required parts

Use these headings, in this order.

### Background

État du code avant l'introduction des changements, et l'historique d'avant les changements.

- What a caller could already do, and which types or modules owned that behaviour.
- The recent history of those files: what the last meaningful commits were about, in prose (subjects paraphrased, not a raw log dump).
- Where the new work attaches. Stop before describing the new solution.

### Intuition before details

But de la PR, puis un exemple travaillé sans aucun code.

- The goal in one short paragraph, tied to the linked story when there is one.
- One concrete scenario (who starts it, what they have, what they get back, what fails). No fenced code, no file paths, no class or function names.
- The decision the new behaviour has to make, still in that scenario.

### Interactive figures

Chemins visuels. Diagrammes Mermaid qui montrent ce qui se passe, y compris les chemins de décision du code ajouté.

- At least one `flowchart` or `sequenceDiagram` of the new path from the caller's point of view.
- One diagram whose branches are the real decisions in the added code (guards, error cases, engine or protocol choices). Labels in French.
- Keep each diagram small. Split when a figure needs more than about a dozen nodes.
- Every branch must match the code. Do not draw a path that does not exist.

### Code walk

Description du code dans un ordre logique pour comprendre ce qui a été implémenté et comment.

- Order the walk by the runtime story (entry point, then the steps), not by the order of files in the diff.
- For each step: the role of the type, why it sits there, and the few lines that carry the decision. Cite them.
- Name the trade-off only when the diff shows one (a new type, a new port, a sync point).
- Close with **Concepts** : each subsystem, workflow, or concept this PR introduces, uses, or modifies, one line each, with the matching `topics/<slug>.md` name. `explain-topic` writes those files. If this skill runs alone, still list the concepts and write the topic files only when the user asked for them.

## Done

The file exists, the four headings are present, the intuition section has no code, and every cited path exists in the range you reviewed.
