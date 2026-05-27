# Workflows

Workflows describe how a project step should run inside the harness.

They are operational documentation, not scripts. A workflow records the expected
sequence of actions, roles, inputs, outputs, validation, reporting, and review
for a repeatable execution cycle.

Workflows must stay vendor-neutral and tool-agnostic. They should use the
generic roles `Planner/Reviewer`, `Executor CLI`, and `Human Operator` instead
of depending on a specific AI tool or vendor.

Workflows should be simple, reviewable, and versioned. They are meant to make
execution easier to audit without adding automation before the project needs it.

Workflow documentation must direct the Executor CLI to write real or temporary
execution reports to `.harness/reports/runs/`. Those reports are local outputs
ignored by Git; report templates and conventions remain versioned under
`.harness/reports/`.

Future phases may introduce automation based on these conventions, but this
stage creates only documentation.

## Initial workflows

- `blueprint-execution-review.md`: defines the minimal cycle from blueprint to
  execution, validation, report, review, and next blueprint.
