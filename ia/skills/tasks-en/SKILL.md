---
name: tasks-en
description: Breaks a story into executable "prompt" tasks for a code agent with tests, documentation, and API examples; keeps asking while ambiguities remain
---

## What I do

I take an existing story and break it down into elementary tasks. Each task contains a maximally detailed prompt ready to be executed by a code agent, along with automated tests, documentation, and API examples (`.http` files). I verify the coherence of the generated tasks and clarify with the user via a `questions.md` file until everything is resolved.

## When to use me

Use this skill after creating your user stories with the `story-en` skill, when you want to move to implementation.

## File layout (`tmp/`)

Each story has its own **`<id>`** as a 3-digit number (`001`, `004`, …). Normalize user input (`1` → `001`, `story-004` → `004`).

```text
tmp/
  <id>/
    story.md                    # user story (input)
    story/
      task-001.md               # pending task
      task-001-done.md          # completed task (renamed)
      task-002.md
      task-001-examples.http    # HTTP examples (if applicable)
      task-001-examples-done.http  # renamed with the task
      questions.md              # temporary clarifications (deleted when done)
```

| File | Role |
|------|------|
| `tmp/<id>/story.md` | Story read as input |
| `tmp/<id>/story/task-YYY.md` | Detailed prompt — **pending** |
| `tmp/<id>/story/task-YYY-done.md` | Same prompt — **completed** (`-done` suffix before the extension) |
| `tmp/<id>/story/questions.md` | Task clarification questions |

**`-done` convention:** once a task is executed and validated (green-test criterion met), **rename** the prompt file and any associated files (`task-YYY-examples.http` → `task-YYY-examples-done.http`). Do not delete the content — renaming tracks progress. Pending tasks are files matching `task-*.md` **without** `-done`.

## Workflow

### Step 1 - Read the story
- Take the story number provided as argument (e.g. `/ tasks-en 001`)
- Normalize the id: `001`
- Read the file `tmp/001/story.md`
- Extract the description, acceptance criteria, tests, and expected documentation
- Read the specification and other stories (`tmp/*/story.md`) for full context

### Step 2 - Decomposition & generation with detailed prompts
- Analyze the story to identify atomic work units
- For each unit, generate an extremely detailed prompt including:
  - **Precise objective** of the task
  - **Functional description**: expected behavior, nominal and edge cases
  - **Files involved**: exact paths, nature of change (create, modify, delete)
  - **Technical constraints**: framework, patterns, project conventions, error handling, performance, security
  - **Function/class signatures**: names, parameters, types, return values
  - **Data structures**: schemas, models, DTOs, interfaces
  - **Tests to implement**: concrete unit, integration, and E2E scenarios
  - **Documentation**: what to document and where
  - **Usage examples**: code snippets, HTTP API requests
- Create the directory `tmp/<id>/story/` if it doesn't exist
- Generate files `tmp/<id>/story/task-YYY.md`

### Step 3 - Coherence verification
- Analyze the generated tasks against each other:
  - Check for circular dependencies or problematic execution order
  - Verify each task has clear inputs and outputs
  - Verify the sum of tasks covers 100% of the story acceptance criteria
  - Identify duplicates or overlaps between tasks
  - Check naming, types, and signature consistency across tasks
- Analyze coherence with the existing project:
  - Check compatibility with existing conventions and architecture
  - Verify referenced files exist or are correctly marked for creation

### Step 4 - Create questions.md
- Create a file `tmp/<id>/story/questions.md`
- Add clarification questions for:
  - Undecided implementation choices
  - Inconsistencies between tasks
  - Ambiguities in the story
  - Dependencies and execution order
  - Alternative technical proposals
- Fully follow the "question quality" rule: each question must include stake, factual context (task/story/existing-code excerpts), dependencies, detailed proposals with implications, and an answer slot
- Ground context in the project (file paths, Gradle modules, existing patterns) so the user can decide without re-reading every prompt

### Step 5 - Update
- The user answers the questions in `questions.md`
- The user types: `I've answered, update the tasks`
- Update the tasks based on the answers
- **Re-read the updated tasks**: if any ambiguity, undecided choice, inconsistency between tasks, or point that deserves a further answer remains → regenerate `questions.md`
- Return to Step 3 (do not close while something still needs clarifying)

### Step 6 - Workflow completion
- When, after re-reading, there is genuinely nothing left to clarify
- Delete the `questions.md` file
- Present the final plan to the user

## Rule: continuous clarification (mandatory)

**Never close the loop while something still needs clarifying.** After every task update, re-read the batch and ask **new** questions if:

- an implementation choice, signature, schema, or test criterion remains ambiguous or incomplete;
- an answer raises further questions (task order, touched modules, Gradle profiles, API contracts);
- two tasks contradict or overlap without an explicit decision;
- a point deserves confirmation before a code agent executes the prompt.

**Prefer asking over assuming.** Do not invent technical decisions just to finish faster. An empty `questions.md` is valid only after an active re-read found no reasonable doubt.

## Rule: question quality (mandatory)

Every entry in `questions.md` must let the user answer **with full awareness**, without re-reading every task prompt. A short question ("JSON API or custom?", "Order 001 then 002?") is **not enough**.

For **each** question, include **all** of the following blocks:

| Block | Expected content |
|-------|------------------|
| **Stake** | Impact on prompts, execution order, modules, green-test criteria |
| **Context** | Excerpts from tasks/story; files or patterns already in the repo; what is frozen vs fuzzy |
| **Dependencies** | Tasks, signatures, schemas, or test commands impacted |
| **Question** | Exactly one precise decision |
| **Proposals** | ≥ 2 options (plus "Other"); each option: concrete behavior, implications (files, tests, order), when it fits |
| **Answer** | Empty slot for the user |

**Forbidden:** bare options without implications; jargon not tied to existing code; packing several independent decisions into one question.

**Required:** cite `Task #YYY`, paths (`crbpm/...`), and a signature or criterion excerpt when useful. A recommendation is allowed with justification — without forcing the choice.

### Task execution and closure (code agent)

When an agent implements a task from `tmp/<id>/story/task-YYY.md`:

1. **Selection:** pick the next task **without** the `-done` suffix, respecting dependency order declared in the prompts.
2. **Implementation:** follow the prompt; satisfy the green-test criterion.
3. **Mandatory closure** — once validated:
   - Rename `task-YYY.md` → `task-YYY-done.md`
   - Rename associated files too: `task-YYY-examples.http` → `task-YYY-examples-done.http` (same rule: `-done` before the extension)
   - Rename **only after** tests pass; if the task fails, leave the file without `-done`
4. **References:** later tasks depend on "Task #YYY" (number), not the filename — status is read from the presence or absence of `-done`.

When **updating** tasks (Step 5), only modify files **without** `-done`, unless the user explicitly asks otherwise.

## Detailed prompt format

### Task file structure (task-YYY.md)

```markdown
# Task #YYY - Story #XXX : [Task title]

## Objective
[Concise description of what this task should deliver]

## Context
- Story #XXX : `tmp/<id>/story.md`
- Depends on : Task #ZZZ (if applicable)
- Required for : Task #WWW (if applicable)

## Prompt

As a code agent, you must implement the following.

### Functional description
[Detailed description of expected behavior]

**Nominal cases:**
- [Case 1]
- [Case 2]

**Edge cases:**
- [Edge case 1]
- [Edge case 2]

**Error handling:**
- [Error 1] → [Expected behavior]
- [Error 2] → [Expected behavior]

### Files involved

| File | Action | Description |
|------|--------|-------------|
| `src/feature/example.ts` | Create | New service |
| `src/feature/types.ts` | Modify | Add type X |
| `src/feature/__tests__/example.test.ts` | Create | Unit tests |

### Signatures

```typescript
// Function to implement
function example(
  param1: string,
  param2: number
): Promise<Result>
```

### Technical constraints
- **Framework** : Use [framework] (version X)
- **Pattern** : Follow [XXX] pattern already used in [reference file]
- **Style** : Respect the project's ESLint and Prettier config
- **Performance** : [constraint if applicable]
- **Security** : [constraint if applicable]
- **Tests** : Minimum 80% coverage for this task

### Tests to implement

#### Unit tests
- **File** : `src/feature/__tests__/example.test.ts`
- Scenario 1 : [Description]
  - Input : [Input data]
  - Expected result : [Output]
- Scenario 2 : [Description]
  - Input : [Input data]
  - Expected result : [Output]

#### Integration tests
- **File** : `src/feature/__tests__/integration.test.ts`
- Scenario : [Description]

### Completion criterion — green tests (mandatory)

Before considering the task done:

1. **Regression:** the listed suites pass **with no failures**; no existing test disabled or removed to hide a regression.
2. **Commands** (from `crbpm/`) :
   - `.\gradlew :module-a:test :module-b:test` — adapt to touched modules
   - `.\gradlew :backend:build -Pcrbpm.engines=wrapper,c8` — if engine profiles apply
3. **Scope:** [modules, Gradle profiles, gated live ITs if applicable]
4. **Migration:** [if moving code/tests — require colocation in the same task]
5. **Closure:** rename this file to `task-YYY-done.md` (and `task-YYY-examples.http` → `task-YYY-examples-done.http` if present) **only after** the tests above pass.

### Documentation

#### Documentation to create
- `docs/feature/README.md` : New feature documentation
- `docs/feature/examples.md` : Usage examples

#### Documentation to update
- `docs/api/README.md` : Add section on the new endpoint

### API examples (if applicable)
- File : `tmp/<id>/story/task-YYY-examples.http`

See HTTP request examples in the associated .http file.
```

### questions.md format

```markdown
# Clarification questions - Story #XXX

## Global context
- Story #XXX : [Title] (`tmp/<id>/story.md`)
- Tasks generated : [list with titles]
- Focus : [main ambiguities / inconsistencies]

## Questions

### 1. [Task #002] — API error response format
**Stake:** The error contract is referenced by #002 (endpoints) and #003 (tests + `.http`); a late change forces rewriting both prompts.
**Context:**
- Story: "structured errors" without a precise schema.
- Task #002 drafts an error body; the existing API module already uses [cite real repo pattern, e.g. `WRP-AREA-NNNN` + `traceId`].
- No written decision on JSON:API vs house envelope.
**Dependencies:** DTOs/signatures in #002; assertions in #003; `.http` examples.
**Question:** Which error response format should this story's endpoints use?
**Proposals:**
- a) **Align with existing CRBPM** (`code`, `message`, `traceId`, HTTP status) — Implications: reuse existing types/handlers; #002/#003 share the same fields; no JSON:API.
- b) **JSON:API errors** — Implications: new contract; migration or dual-stack to clarify; more work in #002 and tests.
- c) Other: paste expected JSON examples for 400 and 404.
**Answer:**

### 2. [Inconsistency #001 vs #002] — Create DTO name
**Stake:** #002 imports a type that #001 names differently → compile break between supposedly "green" tasks.
**Context:**
- Task #001 defines `CreateTaskRequest`.
- Task #002 references `NewTaskDto` in signatures.
- Target package: `com.moodys.crbpm.…` [path].
**Dependencies:** Signatures in #001/#002; tests for both tasks.
**Question:** What is the canonical name for the create DTO?
**Proposals:**
- a) **`CreateTaskRequest`** — Align #002 and its tests with #001.
- b) **`NewTaskDto`** — Rename in #001 (and #001 tests) before #002.
- c) Other: name + justification (module convention).
**Answer:**

### 3. [Story ambiguity] — Gradle engine profile scope
**Stake:** Without explicit profiles, each task's green-test criterion is ambiguous (wrapper-only vs wrapper,c8).
**Context:** Story may touch `engine-spi` / `engine-c8`; repo default is documented in AGENTS.md.
**Dependencies:** "Completion criterion — green tests" section of each task.
**Question:** Which `-Pcrbpm.engines=…` profiles are mandatory when closing #001, #002, #003?
**Proposals:**
- a) **Retro-compatible default** (`wrapper,c8` or project default) for all tasks until a dedicated wrapper-only task.
- b) **Minimal profile from #001** — Provide the task × profile matrix.
- c) Other: table of task → exact Gradle commands.
**Answer:**
```

## Step transitions

| Transition | User action | Agent action |
|------------|-------------|--------------|
| Step 1 | Provide story number (e.g. `/ tasks-en 001`) | Read story and project context |
| Step 1 → 2 | Automatic | Decompose and generate detailed prompts |
| Step 2 → 3 | Automatic | Verify task coherence |
| Step 3 → 4 | Automatic | Create `questions.md` with points to clarify |
| Step 4 → 5 | Answer in `questions.md` then type `I've answered, update the tasks` | Update tasks according to answers |
| Step 5 → 3 | Automatic | Re-verify coherence; **regenerate `questions.md` if ambiguities remain** |
| Step 5 → 6 | Automatic (no more questions **after re-read**) | Delete `questions.md`, present final plan |

**Important :** The agent does not monitor file modifications in real time. The user must always give an explicit instruction to move to the next step.

## Usage example

**Launch :**
```
/ tasks-en 001
```

**Reads `tmp/001/story.md` :**
Story describing the creation of a REST API for task management (CRUD).

**Decomposition into 3 tasks with detailed prompts :**
- `tmp/001/story/task-001.md` : Data model, database, and validations
- `tmp/001/story/task-002.md` : Full CRUD endpoints with signatures and error handling
- `tmp/001/story/task-003.md` : Automated tests, documentation, and `.http` files

**Coherence verification :**
- Task #002 references a type defined in task #001 → OK
- Task #003 covers all test cases from acceptance criteria → OK
- Error handling : task #002 and specification aligned → OK

**Creates `tmp/001/story/questions.md` :**
- Question about error response format (JSON API vs custom standard)
- Question about task execution order (task #001 must precede #002)

**Update :**
The user answers, the agent updates the tasks, iteration continues until everything is resolved.

**Execution (code agent) :**
After implementing task #001 with green tests :
- `tmp/001/story/task-001.md` → `tmp/001/story/task-001-done.md`
- Proceed to `task-002.md` (without `-done`) once dependencies are satisfied.
