---
name: specification-en
description: Pragmatic project specification creation (English); top-down approach with brief, assumptions, and blocking questions only
---

# Skill: Specification (EN)

This guide describes how to generate a project specification that's sufficient to derive actionable user stories, favoring fast scoping over exhaustive clarification.

## What I Do

I guide creation of a structured specification starting from a user brief and codebase inspection, asking only **blocking** questions and formulating assumptions for everything else.

## When to Use Me

Use this skill to scope a project or feature before breaking it down into user stories with `story-en`.

## File layout (`docs/specification/`)

```text
docs/specification/
  specification.md   # Target specification
  questions.md        # BLOCKING questions only (temporary — deleted when done)
  assumptions.md      # Proposed/inferred assumptions (temporary — deleted when done)
```

| File | Role |
|------|------|
| `specification.md` | Structured spec sufficient to generate testable user stories |
| `questions.md` | Blocking questions requiring explicit arbitration (max 2 rounds) |
| `assumptions.md` | Decisions inferred from codebase/brief with justification, to accept or reject |

## Process

### Step 0 - Brief & Context (TOP-DOWN)
- **Create** `docs/specification/` if it doesn't exist
- **Extract brief**: if the user provides a brief (objectives, users, scope/out of scope, constraints, known stack), apply it directly without questions
- **Inspect codebase**: read `README.md`, `AGENTS.md`, `package.json`, `pom.xml`, `build.gradle*`, `docs/` to identify stack, conventions, architecture, existing patterns
- **Check existing spec**: if `docs/specification/specification.md` exists, use it as base

### Step 1 - Initial draft with assumptions
- **Generate draft** `specification.md` with structure (Overview, Goals, Scope/Out of scope, Actors, Core Features, Constraints, Architecture/Tech, Residual open points)
- **Infer intelligently**: leverage the codebase to propose stack/style/conventions consistent with existing patterns
- **List assumptions** in `assumptions.md` as `[INFERRED]` or `[PROPOSED]` with justification and impact
- **Don't ask** about things clearly inferable or already specified in the brief

### Step 2 - Identify only blockers
- **Analyze** the draft to detect only **real blockers** for moving to `story-en`:
  - Problem to solve / first useful delivery cannot be determined
  - Primary actors cannot be identified
  - Direct contradiction between already-written elements
  - Product/technical decision makes defining a coherent MVP scope impossible
- **If no blockers**: generate `assumptions.md` with remaining assumptions, suggest accepting or giving directives
- **If blockers exist**: create `questions.md` with **maximum 3-5 global questions** (condensed format by default)

### Step 3 - Update based on answers
- User can respond with:
  - `"I've answered the questions, update the specification"`
  - `"Accept all assumptions"`
  - `"Apply my directives: ..."`
  - Targeted feedback by section
- Update `specification.md` and `assumptions.md` accordingly
- **Re-read**: only regenerate `questions.md` if **new blockers** appear. Maximum **2 rounds** total

### Step 4 - Pragmatic closure
- When no blockers remain (or after 2 rounds with assumptions applied):
  - Delete `questions.md` and `assumptions.md`
  - Quick check that spec is **sufficient to derive testable user stories** (clear MVP scope, actors identified, core features defined)
  - Announce the specification is **ready for `story-en`/`story-fr`**

## Rule: targeted clarification (mandatory)

- **Only blocking**: ask only if it prevents defining the problem/MVP scope or resolving a direct contradiction
- **Assume intelligently**: everything else becomes an assumption in `assumptions.md`, justified and applied
- **Stop criterion**: *"Clear enough to let `story-en`/`story-fr` generate testable user stories without guessing the MVP scope"* — NOT "no ambiguity possible"
- **Max 2 rounds**: beyond that, formulate reasoned assumptions and conclude
- **Accept directives**: apply immediately without questioning

## Rule: question quality (adaptable)

### Condensed format (preferred)
```markdown
### Q[X] - [Short context] - [Decision to make]
**Stake:** Impact on MVP scoping / story breakdown
**Question:** [Precise wording, ONE decision]
**Proposals:**
- a) [Option 1] — Behavior + implications
- b) [Option 2] — Behavior + implications
- c) Other: [specify]
**Answer:**
```

### Full format (only for true blockers)
Include: **Stake**, **Context**, **Dependencies**, **Question**, **Proposals** (≥2 + Other with implications), **Answer**. Cite sections of `specification.md` if relevant.

**Forbidden**: questions without context, bare options, multiple decisions in one question

## File Formats

### specification.md (target structure)
```markdown
# Specification - [Project Name]

## Overview
[Problem to solve, value delivered]

## Goals
- [Primary goal]
- [Secondary goals if useful]

## Scope / Out of scope
### In scope (MVP)
- [...]
### Out of scope
- [...]

## Actors / Users
- [Primary role + usage context]

## Core Features
- [Key features for MVP]

## Constraints
- [Technical, non-functional, legal if known]

## Architecture / Stack (proposed)
[Stack consistent with existing codebase + rationale]

## Residual assumptions
[References to assumptions.md if still pending]
```

### assumptions.md
```markdown
# Assumptions - Specification

## Inferred from codebase
- [INFERRED] Stack: [...] — Justif: [detected files] — Status: [APPLIED]

## Proposed from brief
- [PROPOSED] MVP scope: [...] — Justif: [...] — Status: [TO VALIDATE/ACCEPTED/REFUSED]

## Applied globally
- [APPLIED] [...] — Impacts on scoping
```

### questions.md (blocking only)
```markdown
# BLOCKING Questions - Specification

## Global context
- Brief: [summary]
- Codebase inspected: [key findings]
- Round: [1/2]

## Blocking questions
### 1. [GLOBAL/MVP] — [Title]
**Stake:** [Why blocks story breakdown]
**Question:** [ONE decision]
**Proposals:** [...]
**Answer:**
```

## How to Move from One Step to Another

| Transition | User Action | Agent Action |
|------------|-------------|--------------|
| Start | Provide spec path/brief + context | Inspect codebase, extract brief, generate draft + assumptions |
| Draft → Clarification | "Accept all assumptions" OR answer blocking questions OR "Apply my directives: ..." | Apply decisions, refine spec |
| After update | Same form | Only re-ask if new blockers (max 2 rounds) |
| End | Automatic if sufficient | Delete questions.md/assumptions.md, confirm ready for `story-en` |

**Important:** The agent does not monitor file changes in real-time. Always give an explicit instruction to move to the next step.

**Tip:** The more precise your brief (problem, users, scope), the fewer questions will be asked.
