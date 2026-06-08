# Phase Index

`phase-index.tsv` is the versioned phase anchor for the harness. It records the
known phases, their operational status and implementation commit when
available. Paths are not recorded in the TSV; they are derived from the phase
id by the harness scripts.

The TSV columns are:

```text
order	id	status	commit
```

Derived path rules:

```text
Blueprint:  harness/blueprints/<id>.blueprint.md
Handoff:    harness/handoffs/<id>.handoff.md
Validation: harness/validations/<id>.validation.md
Completion: harness/completion/<id>.completion.md
Prompt:     harness/bin/build/prompts/<id>.codex-prompt.txt
Report:     harness/reports/runs/<ID_UPPER_SNAKE>_REPORT.md
```

Example report conversion:

```text
phase-4-3-java-if-else-decision-shape
PHASE_4_3_JAVA_IF_ELSE_DECISION_SHAPE_REPORT.md
```

Allowed statuses:

- `planned`: known, but not selected for execution.
- `next`: the next phase selected for execution.
- `in_progress`: execution has started; commit must remain `TBD`.
- `validation`: execution produced a report and is waiting for Human Reviewer
  approval; commit must remain `TBD`.
- `implemented`: approved and backed by a real implementation commit hash.
- `approved`: approved by the Human Reviewer.
- `blocked`: blocked by an explicit issue or decision.
- `skipped`: intentionally skipped.

`run-next-phase.sh` synchronizes the index from `harness/blueprints/` before it
selects work. To enqueue many phases, save their blueprints as:

```text
harness/blueprints/<id>.blueprint.md
```

Then run:

```bash
./harness/bin/run-next-phase.sh
```

Blueprint ids missing from the index are appended as `planned` with commit
`TBD`. Existing lines, statuses and commits are preserved. Lines are not
deleted just because a blueprint is missing; the runner prints a warning for
those ids.

Queue rules:

- At most one line may use `next`; multiple `next` rows fail.
- Any `in_progress` row blocks execution of every `next` phase.
- Any `validation` row blocks execution of every `next` phase.
- If there is no `next`, no `in_progress` and no `validation`, the first
  `planned` phase is promoted to `next`.
- If there is no `next` and no `planned`, the runner reports that no next phase
  is available.
- If the derived report already exists for the `next` phase, execution is
  blocked because the phase may already be waiting for validation.

Codex execution must update the active phase with:

```bash
./harness/bin/update-phase-index-status.sh start <phase-id>
./harness/bin/update-phase-index-status.sh validation <phase-id>
```

Those commands preserve row order and TSV format, change only the selected
phase and keep commit as `TBD`. They do not promote another phase to `next`.
The harness does not mark phases as `implemented` or `approved` automatically.
Those transitions require human review and a real commit hash. Human approval is
still required before merge or push decisions.
