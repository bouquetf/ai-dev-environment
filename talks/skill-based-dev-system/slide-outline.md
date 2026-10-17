# Slide outline — Skill-based development system

## Rules applied
- One general idea per slide.
- At most 3 axes.
- At most 3 developments per axis.
- Each development is one slide.
- At most 3 short supports on a slide, all serving that one idea.
- The workflow is one overview. The specification skill is the worked example that follows.

## Framing slides

### Title
- General idea: Skills take the repeated work I struggle with, so I can stay on the work I am good at.
- Supports: none.

### Close
- General idea: Start from a real personal problem — how you work, what you need, and the result you expect — then ask the agent to generate the skill.
- Supports:
  - Focus on your strengths and give your difficulties to the AI.
  - Specify the context as precisely as possible, rather than give instructions.
  - The method is inspired by BMAD and by harness-engineering practices.
- Source: brief close; feedback "problème réel personnel"; feedback "BMAD"; feedback talk order, conclusion.

## Axis 1 — Start from the person

### 1. Opening shift
- General idea: People usually look at what AI does well; this talk shows what the speaker does well thanks to AI, by focusing on strengths and delegating struggles.
- Supports:
  - AI agents adapt the world to each person's specificities and are a strong inclusion tool, so the human can stay on what they already do well.
- Source: brief opening; feedback on inclusion. "Intégration" here means inclusion. Feedback talk order, introduction.

## Axis 2 — The development loop

### 1. The loop
- General idea: Development runs from specification through commit, and every feedback loop returns to specification.
- Supports:
  - One diagram of the six steps on one row: specification; stories; tasks that generate code, documentation, and tests; code review; validation in a simulation close to production; description and commit. Feedback stays on the lower row, centered on the Tasks-to-Review arrow. The return arrows are segmented and travel below the row, so they do not cross Validate or Stories.
- Source: brief workflow. One support, because the six steps are one sequence.

### 2. Demo focus
- General idea: The demo focuses on the specification skills and on running tests close to production.
- Supports:
  - `specification-en` and `specification-fr` are the same step: ask until the specification is clear.
  - `run-tests-dood` validates in an environment close to production.
- Source: speaker request for a demo slide between the workflow and the environment.

## Axis 3 — How a skill works

### 1. In your tools and your language
- General idea: A skill such as `specification-fr` runs in the environment you already use, in your native language, and asks questions until ambiguity and inconsistency are gone.
- Supports:
  - IntelliJ, ACP, Cursor, and the command.
  - Interacting in the speaker's native language lets the speaker be more precise, avoid inconsistencies, stay more relevant, and reach subtleties that would otherwise be forgotten or left unsaid.
  - A question-and-answer mechanism resolves what is still unclear. The agent asks, the person confirms, and the agent does not decide for the user.
- Source: brief, how a skill works. The native-language line is the accepted feedback, moved here with the specification skill. It does not compare the French and English files.

### 2. Local Markdown
- General idea: The skill writes local Markdown and avoids external systems and MCP servers as much as possible, to spend fewer tokens.
- Supports: none.
- Source: brief, local Markdown practice.

### 3. Ask Cursor to write it
- General idea: Focus on what you want to do, state the expected result and where you need guidance, then ask Cursor to write the skill.
- Supports:
  - Focus on what you want to do.
  - State the expected result of the step.
  - State where you need guidance.
- Source: brief, how to create a skill. Asking Cursor is the idea of the slide.

## Not on slides
- A definition of what a skill is. The room is assumed to know.
- The internal anatomy of a skill file.
- A full live walkthrough, file excerpts, and a comparison of French and English skill files. The demo slide only names `specification-en`, `specification-fr`, and `run-tests-dood`.
- A catalogue slide of every named skill. Review and near-production validation are steps in the loop. The environment is the IntelliJ, ACP, Cursor, and command support.
- A team rollout plan, failure modes, and a full authoring workshop.
- Products other than this repository's skill workflow, beyond the general method the examples illustrate.
- Speaker notes, assets, and PDF export.

## Check
- Axes: 3/3
- Developments: Axis 1 = 1/3, Axis 2 = 2/3, Axis 3 = 3/3
- Framing slides: title, close
- One idea per slide: yes
- Brief topics covered: opening, six-step loop, demo focus on specification and near-prod tests, skill environment and language, local Markdown, creating a skill, close
- Feedback state: 6 accepted, 0 held, 0 new. Each accepted entry is present in the destination named in feedback.md.
- Out of scope absent: yes
- Duration: 10 minutes, remote, English, questions at the end. One demo focus slide, no full live walkthrough.
