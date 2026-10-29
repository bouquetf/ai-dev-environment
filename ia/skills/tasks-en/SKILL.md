---
name: tasks-en
description: Pragmatic breakdown of User Stories into executable "prompt" tasks; align with existing codebase, use assumptions, ask only blocking questions
---

# Skill: Tasks (EN)

This guide describes how to break down a story into detailed, executable prompts for a code agent with tests, documentation, and API examples. The goal is to produce actionable tasks without endless clarification cycles.

## What I Do

I take an existing story, inspect the codebase to match current conventions, propose a breakdown with assumptions baked in, and only ask **truly blocking** questions needed to produce tasks that can satisfy a "green tests" criterion.

## When to Use Me

Use this skill after creating user stories with `story-en`, when you want prompts ready to execute.

## File layout (`docs/stories/`)

```text
docs/stories/
  <id>/
    story.md                    # user story (source)
    story/
      task-001.md               # pending task
      task-001-done.md          # completed task (renamed)
      task-002.md
      task-001-examples.http
      task-001-examples-done.http
      questions.md              # BLOCKING questions only (deleted when done)
      assumptions.md            # assumptions/conventions (deleted when done)
```

| File | Role |
|------|------|
| `docs/stories/<id>/story.md` | Story read as input |
| `docs/stories/<id>/story/task-YYY.md` | Detailed prompt — **pending** |
| `docs/stories/<id>/story/task-YYY-done.md` | Detailed prompt — **completed** (`-done` before extension) |
| `docs/stories/<id>/story/questions.md` | **Blocking** questions only |
| `docs/stories/<id>/story/assumptions.md` | Inferred/applied conventions (naming, errors, tests, structure) |

**`-done` convention:** rename the prompt file and associated files (`task-YYY-examples.http` → `task-YYY-examples-done.http`) **only after** green tests are validated. Do not delete content.

## Workflow

### Step 0 - Inspect & align with existing codebase
- Normalize story id (`1` → `001`)
- Read `docs/stories/<id>/story.md`, `docs/stories/plan-implementation.md` (if present), the specification
- **Inspect the codebase** around impacted areas: naming patterns, error handling, DTO/Service/Controller structure, test organization, Gradle conventions
- **If two patterns coexist**, only raise them if they create a blocking inconsistency. Otherwise, **align with the majority pattern**

### Step 1 - Generate tasks directly
- Break the story into atomic units that can be delivered **with green tests**
- For each task, generate a detailed prompt with: objective, context, files, signatures, constraints, tests to implement, **"green tests" completion criterion** with exact commands, documentation, `.http` examples if API
- **Apply inferred conventions** directly (class/file names, error format, test placement)
- Create `docs/stories/<id>/story/` and generate `task-YYY.md`

### Step 2 - Targeted coherence check
- Check dependencies, ordering, coverage of acceptance criteria
- Check signature consistency across tasks
- Check no task leaves the suite red
- **Verify each task has a concrete "green tests" criterion**

### Step 3 - Assumptions vs blockers
- List applied conventions in `assumptions.md` as `[INFERRED - APPLIED]` or `[PROPOSED - APPLIED]`
- Only emit questions to `questions.md` if **truly blocking** for writing an executable prompt or guaranteeing green tests:
  - Unresolvable circular dependency
  - Contract between tasks not deducible and inconsistent with existing code
  - Ambiguity making it impossible to determine modules/test commands
- **If no blockers**, go straight to step 4

### Step 4 - Minimal clarification
- User can respond: `"I've answered, update the tasks"`, `"Accept all assumptions"`, `"Apply my directives: ..."`, or targeted feedback
- Update only tasks **without** `-done`
- **Maximum 2 rounds** of blocking questions. Beyond that, assume intelligently and present the final plan

### Step 5 - Closure
- Delete `questions.md` and `assumptions.md` (if present)
- Present execution plan (order, dependencies, typical commands)
- Announce ready for execution by a code agent

## Rule: minimal clarification (mandatory)

- **Align with existing first**: if an identical pattern exists in repo, use it without asking
- **Only blocking questions**: only what prevents an executable prompt with satisfiable "green tests" criterion
- **Prefer assumptions**: conventions, naming, organization → `assumptions.md`
- **Stop criterion**: *"Sufficient to generate executable prompts that achieve green tests per task"* — not "zero doubt"
- **Max 2 rounds**: strictly limit iterations
- **Accept directives**: apply immediately

## Rule: question quality (adaptable)

### Condensed format (preferred)
```markdown
### Q[X] - [Task/Context] - [Blocking decision]
**Stake:** Impact on execution / green tests
**Question:** [ONE decision]
**Proposals:**
- a) [Option 1] — Behavior + implications (files/tests/order)
- b) [Option 2] — Behavior + implications
- c) Other: [specify]
**Answer:**
```

### Full format (only complex impasse)
Include: **Stake**, **Context** (task/story/existing code excerpts), **Dependencies**, **Question**, **Proposals** detailed, **Answer**. Cite `Task #YYY`, paths, signatures if useful.

**Forbidden**: aesthetic preferences without impact, bare options, multiple decisions

## Principle: green tests between tasks

- Each task must be **mergeable** with a green suite at the end
- **Atomic move**: code + tests in the same task
- **Cross-cutting refactor**: update all call sites/tests in the same task
- **No workarounds**: never disable/remove tests to hide regressions
- **Mandatory criterion** in every `task-YYY.md` with exact commands per modules/profiles

## Task execution and closure (code agent)

1. Pick next task **without** `-done`, respect dependency order
2. Follow the prompt, validate with "Completion criterion — green tests" commands
3. **Close only if green**: rename `task-YYY.md` → `task-YYY-done.md` and `task-YYY-examples.http` → `task-YYY-examples-done.http`
4. Never rename on failure

**When updating (step 4):** only modify tasks **without** `-done`

## Prompt format (task-YYY.md)

```markdown
# Task #YYY - Story #XXX : [Title]

## Objective
[What this task must deliver]

## Context
- Story #XXX : `docs/stories/<id>/story.md`
- Depends on : Task #ZZZ (if applicable)
- Required for : Task #WWW (if applicable)
- Aligned with : [reference to existing pattern in repo]

## Prompt
[Detailed instructions for the code agent]

### Files involved
| File | Action | Description |
|------|--------|-------------|
| ... | Create/Modify/Delete | ... |

### Signatures / Structures
[Types, DTOs, interfaces, key signatures]

### Tests to implement
[Concrete unit/integration/E2E scenarios]

### Completion criterion — green tests (mandatory)
1. Commands from `crbpm/` (adapt to touched modules)
   - `.\gradlew :module:test` ...
2. No regressions, no tests disabled/removed
3. If move/refactor: colocate code+tests
4. Closure: rename to `task-YYY-done.md` (+ `.http` if present) only after validation

### Documentation
[To create / to update]

### API examples (if applicable)
`docs/stories/<id>/story/task-YYY-examples.http`
```

## assumptions.md (example)
```markdown
# Assumptions & Conventions - Tasks Story #XXX

## Inferred conventions (applied)
- [INFERRED - APPLIED] DTO naming: `*Request`/`*Response` (observed in ...)
- [INFERRED - APPLIED] Error handling: aligned with existing `ErrorResponse`
- [INFERRED - APPLIED] Tests: placed under module test layout matching repo

## Proposed choices (applied with justification)
- [PROPOSED - APPLIED] Breakdown: [justified by dependencies]
- [PROPOSED - APPLIED] Order: [justified by dependencies]

## Only validate if blocking
- [TO VALIDATE - BLOCKING] [only in true impasse]
```

## questions.md (blocking only)
```markdown
# BLOCKING Questions - Story #XXX

## Global context
- Story: [title]
- Tasks generated: [list]
- Aligned with: [identified patterns]
- Round: [1/2]

## Blocking questions
### 1. [Task #XXX / Inconsistency] — [Title]
**Stake:** [Why blocks executable prompt or green tests]
**Context:** [Relevant excerpts]
**Dependencies:** [Tasks/signatures/modules]
**Question:** [ONE decision]
**Proposals:**
- a) [...]
- b) [...]
- c) Other: [...]
**Answer:**
```

## How to Move from One Step to Another

| Transition | User Action | Agent Action |
|------------|-------------|--------------|
| Start | `/ tasks-en <id>` | Inspect story + codebase, generate aligned tasks + assumptions |
| After generation | `"Accept all assumptions"` OR answer blocking questions OR `"Apply my directives: ..."` | Update if answers/directives provided |
| Update | `"I've answered, update the tasks"` | Update only tasks without `-done`; re-check (max 2 rounds) |
| End | Automatic if no blockers | Delete questions.md/assumptions.md, present final plan |

**Important:** Do not modify tasks with `-done`. Always give an explicit instruction to move to the next step.
