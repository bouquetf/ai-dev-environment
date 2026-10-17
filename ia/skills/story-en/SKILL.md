---
name: story-en
description: Breaking down specifications into User Stories (English); keeps asking while ambiguities remain
---

# Skill: Stories (EN)

This guide describes how to generate user stories from a specification considering documentation and test automation.

## What I Do

I break down a specification into actionable user stories, taking into account software documentation and test automation. Each story must bring proven value to the end user.

## When to Use Me

Use this skill when you have a project specification and want to generate structured, testable, and documented user stories.

## File layout (`tmp/`)

Stories live under `tmp/` (throwaway working directory, not durable `docs/` deliverables). Each story has its own **`<id>`** as a 3-digit number (`001`, `004`, …) — normalize user input (`1` → `001`, `story-004` → `004`).

```text
tmp/
  <id>/
    story.md              # user story content
    questions.md          # temporary questions (story workflow — deleted when done)
  implementation-plan.md  # cross-story implementation plan (end of workflow)
```

| File | Role |
|------|------|
| `tmp/<id>/story.md` | User story #XXX (title, criteria, tests, dependencies) |
| `tmp/<id>/questions.md` | In-progress clarifications for that story |
| `tmp/implementation-plan.md` | Implementation order and dependencies across stories |

**Tasks** for a story are created by the `tasks-en` skill under `tmp/<id>/story/`; a completed task is renamed to `task-YYY-done.md` (see `tasks-en` skill).

## Workflow

### Case 1: No Stories Exist (start from scratch)

**Step 1 - Read the specification:**
- User provides the specification path as argument (ex: specification.md or docs/specification/specification.md)
- Read the provided specification
- Generate user stories that meet the criteria:
  - Consider existing software documentation
  - Integrate test automation
  - Bring proven value to the end user
- Create the `tmp/` directory if it doesn't exist
- For each story, create `tmp/<id>/` and generate `tmp/<id>/story.md` (e.g. `tmp/001/story.md`, `tmp/002/story.md`)

**Step 2 - Create the questions file:**
- Create a temporary file `tmp/questions.md` (global clarifications for the story batch)
- This file will contain the questions the user needs to answer

**Step 3 - Analysis and clarification:**
- Analyze the generated stories
- Verify consistency with the specification
- Identify what is unclear or missing
- Add clarification questions in questions.md with:
  - The mandatory format from the "question quality" rule (stake, context, dependencies, detailed proposals, answer slot)
  - References to the specification / documentation when applicable
  - Proposals where **each** option explains resulting behavior and implications for the affected stories

**Step 4 - Update:**
- User answers questions in questions.md
- User types: I've answered the questions, update the stories
- Update stories based on given answers
- **Re-read the updated stories**: if any ambiguity, unclear point, inconsistency with the specification, or topic that deserves a further answer remains → regenerate `questions.md` with those new questions
- Return to Step 3 (do not close while something still needs clarifying)

**Step 5 - Generate implementation plan and workflow end:**
- When, after re-reading, there is genuinely nothing left to clarify (no useful questions in questions.md)
- Delete the questions.md file
- Analyze dependencies between stories (from ## Dependencies section of each story)
- Generate the file `tmp/implementation-plan.md` containing:
  - Recommended implementation order (based on dependencies, user value, complexity)
  - Stories that can be developed in parallel (without cross-dependencies)
  - Justifications for the proposed order
  - Dependency diagram in Mermaid format
- Announce the end of the workflow

## Rule: continuous clarification (mandatory)

**Never close the loop while something still needs clarifying.** After every story update, re-read the batch and ask **new** questions if:

- an acceptance criterion, test, dependency, or user-value statement remains ambiguous or incomplete;
- an answer raises further questions (scope, roles, edge cases, order between stories);
- two stories contradict or overlap without an explicit decision;
- a point deserves confirmation before it is locked into the implementation plan.

**Prefer asking over assuming.** An empty `questions.md` is valid only after an active re-read found no reasonable doubt — not merely because the previous cycle had no formal placeholders left.

## Rule: question quality (mandatory)

Every entry in `questions.md` must let the user answer **with full awareness**, without re-reading every story or guessing what is at stake. A short question ("Which roles?", "Cypress or Playwright?") is **not enough**.

For **each** question, include **all** of the following blocks:

| Block | Expected content |
|-------|------------------|
| **Stake** | Why the answer matters for the story/stories (criteria, tests, dependencies, implementation plan) |
| **Context** | Relevant excerpts from the story/spec; what is already decided; what remains fuzzy; overlaps with other stories |
| **Dependencies** | Stories, criteria, or tests impacted by the answer |
| **Question** | Precise wording; exactly **one** decision |
| **Proposals** | ≥ 2 concrete options (plus "Other"); each option: behavior, implications on stories/tests, when it fits |
| **Answer** | Empty slot for the user |

**Forbidden:** questions without story/spec anchoring; bare options without implications; packing several independent decisions into one question.

**Required:** cite the story (`Story #00X`), the section (criterion, test, dependency…), and a short excerpt when useful. If an option is recommended, justify it without forcing the choice.

### Case 2: Stories Already Exist

- Start directly at Step 3 of Case 1
- Read existing stories in `tmp/*/story.md`
- Analyze and iterate until complete resolution (same continuous-clarification rule)

## File Formats

### Story (`tmp/<id>/story.md`)
``markdown
# Story #XXX: [Story Title]

## Description
As a [role], I want [action] so that [benefit].

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Automated Tests
- Unit test: [Description]
- Integration test: [Description]
- E2E test: [Description]

## Documentation
- [Link to impacted documentation]
- [New documentation to create]

## User Value
[Description of proven value for the end user]

## Dependencies
- [Story #YYY]
- [Existing documentation]
``

### questions.md (`tmp/questions.md` or `tmp/<id>/questions.md`)
``markdown
# Clarification Questions - Stories

## Global context
- Specification: [path]
- Stories generated / involved: [list]
- Focus: [summary of main ambiguities]

## Questions

### 1. [Story #002 — Acceptance criterion] — Auth role matrix
**Stake:** Without explicit roles, #002 acceptance criteria stay untestable and #005 (sharing) will be inconsistent.
**Context:**
- Story #002 mentions authentication but not role management.
- Spec: multi-user on the same board.
- Story #005 already assumes a "read-only guest" that #002 never defines.
**Dependencies:** Criteria in #002; description of #005; E2E login/permission tests.
**Question:** Which roles must #002 introduce, and with which board permissions?
**Proposals:**
- a) **Admin / User / Guest** — Admin: everything + members; User: card CRUD; Guest: read-only. Implications: #002 adds 3 roles; #005 aligns; E2E × 3 profiles.
- b) **Admin / User only** — No Guest at MVP. Implications: remove Guest from #005; simplify tests; defer read-only.
- c) Other: provide a role × action matrix (create board, invite, edit card, delete).
**Answer:**

### 2. [Story #004 — Automated test] — E2E framework for drag-and-drop
**Stake:** The E2E runner choice drives #004 scenario writing and CI.
**Context:**
- #004 requires E2E tests for task drag-and-drop.
- Repo inventory: [state whether Cypress/Playwright/Selenium already exists, or none].
**Dependencies:** "Automated Tests" section of #004; possibly other UI stories.
**Question:** Which E2E framework should #004 scenarios use?
**Proposals:**
- a) **Playwright** — Already / to add: [repo state]. Implications: modern API, good DnD support; align test docs.
- b) **Cypress** — Already / to add: [repo state]. Implications: known ecosystem; verify DnD support for the project version.
- c) **Defer / do not automate DnD at MVP** — Implications: reword #004 E2E criterion (manual or component test).
- d) Other: tool and where specs live.
**Answer:**

## Detected Inconsistencies
### I1. [Story #002 vs #005] — Guest undefined
**Finding:** #005 references a read-only guest; #002 defines no Guest role.
**Why it matters:** Risk of contradictory stories in the implementation plan.
**Question:** Should Guest be introduced in #002, or removed from #005?
**Proposals:** (same detail level)
**Answer:**
``

### implementation-plan.md (`tmp/implementation-plan.md`)
``markdown
# Story Implementation Plan

## Recommended Implementation Order
1. **Story #001** - [Title] (Priority: High)
   - Justification: [No dependencies, high user value]
2. **Story #002** - [Title] (Priority: High)
   - Justification: [Depends on Story #001, required for subsequent features]
3. **Story #003** - [Title] (Priority: Medium)
   - Justification: [Can be developed in parallel with #004]

## Stories Developable in Parallel
- **Group 1**: Story #003, Story #004 (no cross-dependencies)
- **Group 2**: Story #005, Story #006 (depend only on already implemented stories)

## Dependency Diagram (Mermaid format)
``mermaid
flowchart TD
    %% Ordered stories
    S001[Story #001: Title] --> S002[Story #002: Title]
    S002 --> S003[Story #003: Title]
    S002 --> S004[Story #004: Title]

    %% Parallel groups
    subgraph Parallel1
        S003
        S004
    end

    S003 --> S005[Story #005: Title]
    S004 --> S005
    S005 --> S006[Story #006: Title]

    subgraph Parallel2
        S005
        S006
    end
``

## Notes
- Stories should be implemented in the indicated order to respect dependencies
- Parallel groups can be developed simultaneously by different teams
``

## How to Move from One Step to Another

| Transition | User Action | Agent Action |
|------------|-------------|--------------|
| Step 1 → 2 | Provide the specification path as argument | Generates initial stories and creates questions.md |
| Step 2 → 3 | Automatic after questions.md creation | Analyzes stories and adds questions |
| Step 3 → 4 | Type I've answered the questions, update the stories | Updates stories according to answers |
| Step 4 → 3 | Automatic after update | Re-analyzes stories; **regenerates `questions.md` if ambiguities remain** |
| Step 4 → 5 | Automatic (no more questions **after re-read**) | Deletes questions.md, generates implementation-plan.md, announces end |

**Important:** The agent does not monitor file changes in real-time. The user must always give an explicit instruction for the agent to move to the next step.

## Usage Example

**Case 1 - Start from scratch:**
``
/ story-en specification.md
``

**Case 2 - Existing stories:**
``
/ story-en
``
(Agent automatically detects existing stories in `tmp/*/story.md` and starts analysis)
