---
name: explain-topic
description: >-
  Explains one subsystem, workflow, or concept that a CRBPM pull request uses,
  highlights, or modifies. Writes a French teaching note in four parts:
  introduction au topic, intuition before details, interactive figures, code.
  Use when a PR review must teach the concepts behind a change, or when the
  user asks to explain a topic, subsystem, or workflow in the codebase.
disable-model-invocation: true
---

# explain-topic

Teach one concept the reader needs in order to follow a change. One file per topic. The reader does not already know this corner of the codebase.

Write only under `tmp/`. French is required in these files.

Called by `pr-review-crbpm` once per concept listed at the end of `explain-diff`, or alone when the user names a topic.

## Which topics

Take the list from the **Concepts** section of `explain-diff.md` when that file exists.

Otherwise choose concepts the change introduces, relies on, or modifies: a subsystem boundary, a workflow, a protocol, a persistence or engine rule, a UI state machine. Skip a topic that is only a rename or a private helper with no idea to learn.

Write the ones that carry the design. If more than five qualify, write the five that block understanding and name the rest in one sentence at the top of the first note.

## Output

`tmp/pr-review/<yyyyMMdd-HHmm>-<slug>/topics/<topic-slug>.md`

Reuse the review directory. `<topic-slug>` is a short English kebab-case name (`engine-plugin`, `reactive-repository`).

## Facts before writing

Read the current code that owns the topic, not only the diff. Use `git show <base>:<path>` when the PR changes that ownership, so the note can say what moved. Verify paths and type names. Unknown stays unknown.

## Voice

Same bar as `explain-diff`: French, one idea per paragraph, define the term before stacking more terms, state what the code does.

## Required parts

Use these headings, in this order.

### Introduction au topic

- What the subject is, in a few sentences a newcomer can repeat.
- Where it lives in the code (modules, packages, entry types).
- The problem it solves on this project.
- How this PR relates to it: introduces it, uses it, or changes it. One sentence.

### Intuition before details

Same constraint as `explain-diff`: a worked example with no code, no file paths, and no type names. The example shows the problem and the outcome. It may continue the scenario from `explain-diff.md` when both notes exist.

### Interactive figures

Mermaid diagrams of the paths that make the topic understandable: who talks to whom, and the decision branches that matter. French labels. Small figures. Every branch exists in the code.

### Code

How the topic is implemented.

- Start from the stable entry point, then the collaborators.
- Cite short regions. Explain the mechanism (what data comes in, what is decided, what is returned or persisted).
- If the PR changes this path, mark the new or edited step in prose and show the previous behaviour in one paragraph when the base version differs.
- Point back to `explain-diff.md` for the change-only walk. This section explains the topic, not the whole PR.

## Done

One file per selected topic, four headings each, intuition section without code, citations that exist in the tree.
