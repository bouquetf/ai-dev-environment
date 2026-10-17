---
name: specification-en
description: Iterative project specification creation (English); keeps asking while ambiguities remain
---

# Skill: Specification (EN)

This guide describes how to generate a project specification iteratively with questions and answers until complete resolution.

## What I Do

I guide the creation of a complete project specification through an iterative process of questions, answers, and clarification until all inconsistencies are resolved.

## When to Use Me

Use this skill when you want to create a structured project specification from a rough idea, iterating until everything is clear and consistent.

## Process Steps

### Step 1: Initiation
- Create a documentation working directory (ex: `docs/specification/`) if it doesn't exist
- Generate the `questions.md` file if it doesn't exist
- If a specification file already exists, use its content as the specification base
- Otherwise, generate this empty specification.md file and ask the user to describe what the project will do in questions.md
- Ask open-ended questions in questions.md to better understand the project

### Step 2: Specification Creation
- Read user responses in `questions.md`
- Update `specification.md` with:
  - Structured project description
  - Clear placeholders (ex: `[TO_CLARIFY: ...]`) to identify fuzzy points
  - Inconsistency markers (ex: `[INCONSISTENCY: ...]`)
- Generate a new `questions.md` file containing:
  - Questions needed to resolve placeholders
  - For **each** question: stake, factual context, dependencies, detailed proposals (see "question quality" rule)
  - Detected inconsistencies to resolve (same level of detail)

### Step 3: Update
- Read responses in `questions.md`
- Update `specification.md` by replacing placeholders with answers
- Resolve identified inconsistencies

### Step 4: Iteration
- Relaunch Step 2 with a new agent
- Check if there are remaining placeholders, inconsistencies, **or any still-ambiguous / unclear point that deserves a further answer** (see rule below)
- If yes, continue iteration: regenerate `questions.md` and wait for answers
- If no, delete the questions.md file and consider the specification complete

## Rule: continuous clarification (mandatory)

**Never close the loop while something still needs clarifying.** After every read of the answers and every update to `specification.md`, re-read the result and ask **new** questions if:

- a point remains ambiguous, contradictory, or incomplete;
- an answer raises further questions (unresolved implications, edge cases, roles, constraints, scope);
- a product or technical choice deserves confirmation before it is locked in;
- a placeholder was replaced by wording that is still vague ("to be defined later", "as needed", etc.).

**Prefer asking over assuming.** Do not invent business or technical decisions just to finish faster. The absence of `[TO_CLARIFY: …]` / `[INCONSISTENCY: …]` markers is not enough: if the text still leaves a reasonable doubt, write the question in `questions.md` (with context and proposals when helpful) and **continue the cycle**.

Close only when, after that re-read, **no** ambiguity, inconsistency, or useful question remains — then clearly announce that the specification is complete.

## Rule: question quality (mandatory)

Every entry in `questions.md` must let the user answer **with full awareness**, without re-reading the whole specification or guessing what is at stake. A short question ("Which auth?", "SSO or OAuth?") is **not enough**.

For **each** question, include **all** of the following blocks:

| Block | Expected content |
|-------|------------------|
| **Stake** | Why the answer matters (product, technical, scope, downstream stories) |
| **Context** | What is already decided or known; relevant excerpts/facts; frozen constraints; what remains fuzzy |
| **Dependencies** | What will be blocked or shaped by the answer (other sections, placeholders, related choices) |
| **Question** | Precise, unambiguous wording; exactly **one** decision |
| **Proposals** | At least 2 concrete options (plus "Other"); **each** option describes resulting behavior, implications, and when it fits |
| **Answer** | Empty slot for the user (`- [ ]` or `Answer:` line) |

**Forbidden:** questions without context; bare option lists (`a) SSO b) OAuth`) with no implications; undefined jargon; packing several independent decisions into one question.

**Required:** ground context in the project (references to `specification.md` sections, feedbacks, already-written constraints). If an option is recommended, say so with justification — without forcing the choice.

## File Formats

### questions.md
```markdown
# Specification Questions

## Global context
[Project summary + specification progress + files involved]

## Questions

### 1. [Placeholder / section ref] — [Short decision title]
**Stake:** [Why decide now; impact of a wrong or deferred choice]
**Context:**
- Already decided / known: […]
- Relevant excerpt or fact: […]
- Still unclear: […]
**Dependencies:** [Sections / placeholders / choices that depend on this answer]
**Question:** [One clear decision]
**Proposals:**
- a) **[Name]** — [Concrete behavior]. Implications: […]. Fits when: […].
- b) **[Name]** — [Concrete behavior]. Implications: […]. Fits when: […].
- c) Other: [specify — what to describe]
**Answer:**

### 2. …
…

## Inconsistencies to Resolve
### I1. [Title]
**Finding:** [Conflicting statements/constraints, with citations]
**Why it matters:** […]
**Question:** [How to resolve]
**Proposals:** (same detail level as above)
**Answer:**
```

### specification.md
```markdown
# Specification - [Project Name]

## Overview
[General description]

## Features
[List of features]

## [Section with placeholder]
[TO_CLARIFY: Necessary details about...]

## [Section with inconsistencies]
[INCONSISTENCY: Identified conflict between...]
```

## Usage Example

**Launching the skill:**
```
/ specification-en
```

**Step 1 - Agent creates `docs/specification/questions.md`:**
```markdown
# Specification Questions

## Global context
No existing specification. Goal: frame the project before drafting `specification.md`.

## Questions

### 1. Scope — Objective and main capability
**Stake:** Without a clear objective, the spec risks mixing several products. This answer structures everything else.
**Context:**
- Already decided / known: nothing — cold start.
- Still unclear: product nature, problem solved, users.
**Dependencies:** Audience, technical constraints, later story split.
**Question:** What problem does the project solve, and what is the main capability of the first useful delivery?
**Proposals:**
- a) **Collaborative business tool** — e.g. shared tasks/workflows. Implications: multi-user, permissions, possible real-time.
- b) **Individual-first tool** — Deliver solo value, collaboration later. Implications: auth/sharing deferred.
- c) Other: describe the problem, primary user, and first critical screen/flow.
**Answer:**

### 2. Users — Target audience
**Stake:** Drives personas, UI vocabulary, and accessibility / mobile requirements.
**Context:** Depends on question 1.
**Dependencies:** Roles, permissions, priority journeys.
**Question:** Who are the primary users of the first delivery, and in what usage context (desktop, field, mobile)?
**Proposals:**
- a) **Professional teams** (e.g. agile, ops) — domain jargon OK; prioritize productivity and integrations.
- b) **General public / non-experts** — simple UI, onboarding, mobile-first.
- c) Other: roles, usage frequency, dominant devices.
**Answer:**

### 3. Constraints — Technical and non-functional
**Stake:** Avoid promising real-time, SSO, or offline without a realistic base.
**Context:** No known constraints yet.
**Dependencies:** Stack, hosting, auth choice, real-time strategy.
**Question:** Which constraints are non-negotiable (platforms, stack, compliance, perf, existing integrations)?
**Proposals:**
- a) **Few constraints** — free stack; prioritize time-to-market. Implications: agent will propose cautious defaults to validate.
- b) **Strong constraints** — list stack, cloud, SSO, GDPR, SLA. Implications: locked into the spec from day one.
- c) Other: list must-have vs nice-to-have.
**Answer:**
```

**User responds in `questions.md`:**
```markdown
### 1. … **Answer:** a) Web app for collaborative tasks with Kanban boards; first delivery = one shared board with columns and cards.
### 2. … **Answer:** a) Agile development teams, desktop + tablet usage.
### 3. … **Answer:** a) Mobile-compatible (responsive) required; no imposed tech stack.
```

**Step 2 - Agent generates `specification.md`:**
```markdown
# Specification - Collaborative Kanban Application

## Overview
Web application allowing agile development teams to manage their tasks via collaborative Kanban boards.

## Features
- Creation of customizable boards and columns
- Task management with drag-and-drop
- [TO_CLARIFY: Authentication system (SSO, email, OAuth?)]
- [TO_CLARIFY: Role and permission management]

## Technical Constraints
- Mobile compatible (responsive design)
- [INCONSISTENCY: No tech stack imposed but need real-time for collaboration]
```

**And generates a new `questions.md`:**
```markdown
# Specification Questions

## Global context
Collaborative Kanban for agile teams, mobile responsive. Open placeholders: auth, roles. Inconsistency: real-time desired with no imposed stack.

## Questions

### 1. [TO_CLARIFY: Authentication] — MVP auth mode
**Stake:** Drives user model, onboarding, and possible IdP / BaaS dependency.
**Context:**
- Already decided: multi-user web app (agile teams), no imposed stack.
- Unclear: enterprise SSO vs social login vs email/password for MVP.
**Dependencies:** Roles/permissions section; hosting; compliance.
**Question:** Which authentication mechanism for the first delivery?
**Proposals:**
- a) **Email + password** — Local accounts. Implications: password reset, basic security; no SSO at MVP.
- b) **Social OAuth** (Google and/or GitHub) — Less friction for developers. Implications: provider deps; email still needed for notifications.
- c) **Enterprise SSO (OIDC/SAML)** — Required if corporate clients from day one. Implications: IdP config, longer delivery.
- d) Other: providers and whether multi-tenant.
**Answer:**

### 2. [TO_CLARIFY: Roles] — Board permission matrix
**Stake:** Without clear roles, collaboration story acceptance criteria stay fuzzy.
**Context:** Multi-user collaboration on one board; responsive mobile.
**Dependencies:** Invitations, board sharing, possible audit.
**Question:** Which roles exist on a board, and what can each do (create/edit/delete cards, manage members, workspace admin)?
**Proposals:**
- a) **Admin / Member / Guest** — Admin: everything + members; Member: card CRUD; Guest: read-only.
- b) **Admin / Member only** — No read-only guest at MVP.
- c) Other: provide a small role × action matrix.
**Answer:**

### 3. [INCONSISTENCY: Real-time vs free stack] — Collaborative sync strategy
**Finding:** "Real-time" collaboration is implied, but no stack is imposed — risk of underestimating infra.
**Why it matters:** Custom WebSocket ≠ BaaS ≠ polling; the choice changes cost and technical stories.
**Question:** What level of "real-time" and which approach for the MVP?
**Proposals:**
- a) **Near real-time via BaaS** (e.g. Supabase/Firebase) — Implications: partial vendor lock-in, faster delivery.
- b) **Custom WebSocket / SSE** — Implications: more control, more infra and test work.
- c) **Short polling / refresh at MVP** — Implications: less fluid UX, defer true real-time.
- d) Other: acceptable latency and edit conflicts (last-write-wins vs OT/CRDT).
**Answer:**
```

**Transition Step 1 → Step 2:**
The user must inform the agent that the questions are answered by simply typing:
```
I've answered the questions, continue
```
The agent does not automatically detect file modifications. The user must explicitly give the instruction to continue.

**Step 3 - User responds in `questions.md`, then types:**
```
Questions answered, update the specification
```
Agent updates `specification.md` by replacing placeholders with answers.

**Transition Step 3 → Step 4:**
Agent automatically checks if there are any placeholders left. If there are, it relaunches Step 2 with a new cycle of questions.

**Step 4 - Iteration:**
Agent relaunches Step 2, verifies there are no more placeholders, the specification is complete!

## How to Move from One Step to Another

| Transition | User Action | Agent Action |
|------------|-------------|--------------|
| Step 1 → 2 | Type `continue` or `I've answered` | Reads `questions.md` and generates `specification.md` + new `questions.md` |
| Step 2 → 3 | Type `continue` or `questions answered` | Reads answers and updates `specification.md` |
| Step 3 → 4 | Automatic | Checks remaining placeholders **and** leftover ambiguities; relaunches Step 2 (new `questions.md`) if anything still needs clarifying |
| End | Automatic | Announces that the specification is complete **only** after a re-read with no useful question left |

**Important:** The agent does not monitor file changes in real-time. The user must always give an explicit instruction for the agent to move to the next step, except for the automatic transition Step 3 → 4.

## Completion Criteria

The specification is finished when **all** of the following are true:

1. `specification.md` no longer contains any `[TO_CLARIFY: ...]` or `[INCONSISTENCY: ...]` placeholders.
2. After the post-update re-read, no ambiguous, unclear, or further-answer-worthy point remains.
3. `questions.md` has been regenerated with no useful questions left (or no longer needs to exist) — then is deleted.

## Next workflow step

Once the specification is complete, use the **`story-en`** skill to break it into user stories. Each story is written to `tmp/<id>/story.md`; implementation tasks go under `tmp/<id>/story/` (**`tasks-en`** skill). A completed task is renamed to `task-YYY-done.md`.
