# Presentation brief — Skill-based development system

## Purpose
Show attendees how to build their own skill-based workflow from concrete examples, and how to adopt a mindset of looking at what they are good at, what they struggle with, and what an AI coding agent plus a specific skill can take on.

## Opening
People usually look at what AI does well. This talk opens on what the speaker does well thanks to AI: in development, focus on your strengths and delegate what you struggle with to the AI.

## Audience
- Roles: mostly developers who already use an AI coding agent; also managers and tech leads who want to understand what this approach can do and how to do it.
- Technical level: developers are practitioners of AI-assisted coding. Managers and tech leads need the operating idea and the method.
- Prior knowledge: the room is assumed to already know what a skill is. The talk does not define a skill against a prompt, a rule, or an agent.

## General subject
A general method for shaping a development workflow around reusable skills, illustrated by this repository: start from how the person already works, keep the environment where they are effective, and delegate repeated difficult work to an AI coding agent.

## Expected format
- Duration: 10 minutes.
- Language: English, spoken and written.
- Setting: remote.
- Interaction: questions at the end. The development workflow is one overview, followed by one demo slide on `specification-en` / `specification-fr` and `run-tests-dood`. A specification-style skill is then the worked example of how a skill runs and how to create one. No file excerpts. No full live walkthrough.
- Deliverables: this brief only. Slide structure, speaker notes, assets, and PDF export come from a later structuring step.

## Desired outcomes
- Attendees can create their own skill-based workflow by following the examples.
- Attendees use a simple introspection: what they are good at, what they struggle with, and what the AI and a specific skill can help with.
- Attendees can follow the development loop, see how a skill such as `specification-fr` actually runs, and leave with a way to ask the agent to write the next skill.
- The close keeps three memories:
  - focus on your strengths and give your difficulties to the AI;
  - the method is inspired by BMAD and by harness engineering;
  - specify the context as precisely as possible, rather than give instructions.

## Core message
Skills take the repeated work I struggle with, so I can stay on the work I am good at.

## High-level topics
Selected themes, in talk order. Not a slide outline.

- Open on the habit of looking at what AI does well, then shift to what the speaker does well thanks to AI: focus on strengths and delegate struggles.
- The development workflow as one loop:
  1. Specification.
  2. Split into stories.
  3. Split each story into tasks, tutorials, or prompts that generate the code, the documentation, and the tests that check conformance to the story.
  4. Code review.
  5. Validate the result in a simulated environment as close to production as possible.
  6. Description, then commit.
  Feedback loops return to specification.
- How a skill such as `specification-fr` / `specification-en` works:
  - it runs in the existing development environment: IntelliJ, ACP, Cursor, and the command;
  - the speaker uses their native language to avoid ambiguity and inconsistency;
  - a question-and-answer mechanism resolves what is still unclear;
  - it writes local Markdown files and avoids external systems and MCP servers as much as possible, to spend fewer tokens.
- How to create a skill: focus on what you want to do, state the expected result of the step, state where you need guidance, and ask Cursor to write the skill.
- Close on the three memories: strengths and delegated difficulties, BMAD and harness engineering, and specifying context rather than giving instructions.

## Out of scope
- A definition of what a skill is.
- The internal anatomy of a skill file as its own theme.
- A full live walkthrough, file excerpts, a language comparison between French and English skills, or a catalogue slide of every named skill. One demo slide may name `specification-en`, `specification-fr`, and `run-tests-dood`.
- A team rollout plan.
- Failure modes as their own theme.
- A full skill-authoring workshop. Creating a skill is the short method above, not a workshop.
- Products other than this repository's skill workflow, except as the general method those examples illustrate.
- Detailed slide structure, speaker notes, assets, and PDF export.

## Constraints and available material
- Presentation directory: `docs/presentations/skill-based-dev-system/`.
- Hard slot: 10 minutes, remote, English, questions at the end. One demo slide, no full live walkthrough.
- The opening is one beat. The workflow is one overview. The specification skill is the worked example of how a skill runs and how to ask Cursor to write one. Harness engineering stays one closing memory.
- Example names were supplied by the speaker. They have not been checked against the repository.
- No code or repository survey has been done for this brief.
