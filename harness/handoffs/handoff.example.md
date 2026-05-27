# Handoff Example

This file is an example only. It is not a required live handoff.

## Handoff id

`HARNESS-0.2.3-EXAMPLE`

## Date

2026-05-27

## From role

Human Reviewer

## To role

Codex CLI Executor

## Phase

Harness 0.2.3 - Blueprint as Primary Input

## Primary blueprint path

`harness/blueprints/phase-4-2-java-decision-unresolved-early-return.blueprint.md`

## Blueprint approval status

Approved in this example before execution.

## Derived artifact status

This handoff is an example of a derived and reviewable artifact. It is not a
source of truth independent from the blueprint.

## Objective

Refine the harness so the blueprint is the primary input and handoff,
validation and completion are documented as derived artifacts.

## Context

Harness 0.2.2 created a complete operational package for Phase 4.2. Harness
0.2.3 clarifies that the blueprint is the source of truth and the package files
are derived operational aids.

## Branch strategy

Continue from the active Phase 4 / harness branch. If isolating the microphase,
create the Harness 0.2.3 branch from the current branch, not from `master`.

## Scope

- Document blueprint as primary input and source of truth.
- Document handoff, validation and completion as derived artifacts.
- Refine role, state, handoff, workflow and report policy documentation.

## Out of scope

- Product code changes.
- Decision Trace extractor changes.
- Phase 4.2 implementation or behavior.
- Merge to `master`.
- Automatic push.

## Expected files or areas

- `harness/roles/`
- `harness/state/`
- `harness/handoffs/`
- `harness/blueprints/`
- `harness/validations/`
- `harness/completion/`
- `harness/workflows/`
- `harness/reports/README.md`

## Required validations

```bash
./gradlew test
./gradlew build
git diff --check
```

## Expected report path

`harness/reports/runs/HARNESS_0_2_3_BLUEPRINT_AS_PRIMARY_INPUT_REPORT.md`

## Completion criteria

- The harness documents blueprint as primary input.
- Handoff, validation and completion are documented as derived artifacts.
- State and handoff templates include blueprint-driven fields.
- Report policy remains compatible with `harness/reports/runs/`.
- Validations pass or failures are documented.

## Risks

- Existing historical reports may still mention previous harness terminology.
- Temporary reports under `harness/reports/runs/` are ignored and may not be
  committed unless explicitly requested.

## Open questions

- Should the next cycle resume Phase 4.2 using the blueprint as primary input?

## Blueprint precedence confirmation

If this handoff conflicts with the primary blueprint, pause execution and
correct this handoff before continuing. The blueprint is the source of truth.

## Human approval required before merge/push

Yes. Merge and push remain blocked until the Human Reviewer explicitly approves
them.
