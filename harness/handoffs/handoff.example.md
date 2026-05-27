# Handoff Example

This file is an example only. It is not a required live handoff.

## Handoff id

`HARNESS-0.2-EXAMPLE`

## Date

2026-05-27

## From role

Human Reviewer

## To role

Codex CLI Executor

## Phase

Harness 0.2 - Roles, State and Handoff Refinement

## Objective

Refine the harness so operational participants are documented as roles while
preserving "agentic workflow" as the collaboration style.

## Context

Harness 0.1 used a previous label for operational actors, but Human Reviewer is
a human operational role, not an autonomous agent.

## Branch strategy

Continue from the active Phase 4 / harness branch. If isolating the microphase,
create the Harness 0.2 branch from the current branch, not from `master`.

## Scope

- Replace the previous actors directory with `harness/roles/`.
- Rename role documents to use the `.role.md` suffix.
- Refine role, state, handoff, workflow and report policy documentation.

## Out of scope

- Product code changes.
- Decision Trace extractor changes.
- Phase 4.2 behavior.
- Merge to `master`.
- Automatic push.

## Expected files or areas

- `harness/roles/`
- `harness/state/`
- `harness/handoffs/`
- `harness/workflows/`
- `harness/reports/README.md`
- `.gitignore`
- Root `README.md`, if needed.

## Required validations

```bash
./gradlew test
./gradlew build
git diff --check
```

## Expected report path

`harness/reports/runs/HARNESS_0_2_ROLES_STATE_HANDOFF_REFINEMENT_REPORT.md`

## Completion criteria

- The previous actors directory no longer exists.
- `harness/roles/` documents operational roles.
- State and handoff templates include the required fields.
- Report policy remains compatible with `.gitignore`.
- Validations pass or failures are documented.

## Risks

- Existing historical reports may still mention previous Harness 0.1
  terminology.
- Temporary reports under `harness/reports/runs/` are ignored and may not be
  committed unless explicitly requested.

## Open questions

- Should the next cycle continue with Harness 0.3 or resume Phase 4.2?

## Human approval required before merge/push

Yes. Merge and push remain blocked until the Human Reviewer explicitly approves
them.
