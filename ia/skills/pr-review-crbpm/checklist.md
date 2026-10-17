# PR review checklist (CRBPM) — draft

Apply every pass that the diff can touch. Skip a pass only when the change cannot affect it, and say so in the report's `Not verified` or summary.

Practices below are adapted from Regnology pages retrieved through `regnology-mcp` (2026-09-29) and from this repo's `AGENTS.md` / `CONTRIBUTING.md`. They are a starting set, not a full copy of those pages.

## Readiness

Blockers when missing, unless the description justifies an exception:

- Scope is reviewable. Platform checklist: about 400 changed lines excluding snapshots and lockfiles. Prefer smaller. A large diff needs a reason in the PR body.
- Description says what and why, the risk, and the Jira link. One story, bug, or technical story — not a task, subtask, or epic (`CONTRIBUTING.md`).
- Branch name looks like `type/ATL-xxxx-desc`.
- Commits are meaningful Conventional Commits with a single `Refs: ATL-xxxx` footer. No "WIP" or "temp" subjects.
- Branch is based on current `origin/main` (or the stated release branch for a hotfix).
- Generated files, lockfiles, and wrapper noise are absent or explained.
- CI is green: build, unit tests, lint, integration tests, security scans. Unknown status is recorded, not assumed green.

## Correctness

- Happy path matches the linked story. Edge cases that the change can hit are handled (empty, missing config, partial failure).
- Errors are not swallowed. User-facing and API errors follow `WRP-{AREA}-{NNNN}` and keep a `traceId` where that pattern already applies.
- Public APIs, DB schema, and feature behaviour stay compatible, or the rollout is described.
- No change that only looks right because a helper, endpoint, or config key was assumed. Verify names against the repo.

## Project rules (`AGENTS.md`)

- Backend and API stay reactive: `Uni` / `Multi` / async clients. No blocking JDBC or other blocking I/O on the event loop.
- New Gradle libraries go through `gradle/libs.versions.toml`, not string coordinates.
- Ports come from configuration, not literals in code.
- English only in code, comments, logs, and messages. `tmp/` is the exception and must not be referenced from outside `tmp/`.
- Comments and docs do not cite Jira keys or task numbers. Error codes are allowed.
- UI follows the existing React function-component style. No drive-by refactors outside the change.
- Engine plug-in boundaries stay intact (SPI vs optional engines). A new architectural split needs an ADR, not a silent package.

## Tests

- New or changed behaviour has a test, or the report explains why a test is not practical.
- Bug fixes include a regression test when one can fail before the fix.
- Tests are independent. `@QuarkusTest` and REST Assured match existing module tests.
- Do not demand a coverage percentage the repo does not already enforce. If CI publishes coverage, note a drop; do not invent a gate.

## Security and privacy

- Inputs validated. No string-built queries, path traversal, or secrets in code, logs, or fixtures.
- Authn/authz changes keep least privilege. `tenantId` and `engineId` stay off public APIs.
- This pass does not replace SAST or the security scan in CI. If those checks are red or absent, say so.
- No client data, tokens, or production payloads in the review notes.

## Design (lightweight)

Stay shallow. Hand a structural or duplication concern to `solid-review`.

- The change does one thing. No unused abstraction "for later".
- Names are fully spelled and match behaviour.
- Comments explain why, not what.
- Hot-path cost is called out only with a concrete reason (extra remote call, unbounded query, work on the event loop).

## Hygiene

- Review tone in the report is specific and about the code.
- Unresolved blocker threads are merge blockers. Nits are not.
- Cross-module impact names the other module and the contract that moved.

## Out of scope for this draft

Record these if seen; do not build tooling for them yet:

- Posting the report back to Bitbucket (`@bitbucket_mcp` is the in-house agent's trigger, not this skill's).
- Mutation testing, Sonar quality-gate numbers, and a formal two-tier reviewer role. The Platform code-review pages describe those; this repo's merge gate remains `CONTRIBUTING.md` (green CI, one non-author approval, maintainer merge).
- Approving or merging.

## Sources

- [AI Learning Pathway — Post-Training Recap](https://confluence.regnology.net/spaces/AAR/pages/315092079) — Cursor as the diff reader; author accountable; small commits; human merge gate; verify every reference.
- [AI PR Review Tool: User Guide & FAQ](https://confluence.regnology.net/spaces/PRM/pages/285138967) — assist, do not replace; `AGENTS.md` first; advisory comments; dismiss bad comments by encoding the correction in repo rules.
- [Feeding the AI PR Tool with dedicated rules](https://confluence.regnology.net/spaces/PRM/pages/285138861) — local `AGENTS.md` overrides global standards.
- [In House Ai review agent](https://confluence.regnology.net/spaces/PRM/pages/274812056) — cite the rule; do not treat the agent as a SAST replacement; learning is a human edit to the rules, not memory of past PRs.
- [Code Review](https://confluence.regnology.net/spaces/PLF/pages/248879290) and [Code Review Process](https://confluence.regnology.net/spaces/PLF/pages/307758192) — readiness, tests, security, small diffs, automate style so humans review behaviour.
- This repo: `AGENTS.md`, `CONTRIBUTING.md`.

## Evolution log

- 2026-09-29 — Initial draft. Checklist is intentionally shorter than the Platform code-review page. Add a bullet only after a real review shows the gap.
- 2026-09-29 — Learning pack is mandatory: French `explain-diff.md` plus one `explain-topic` note per concept, under `tmp/pr-review/`. Findings stay in `review.md` beside them.
