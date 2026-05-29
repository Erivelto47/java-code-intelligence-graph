# Phase Index

`phase-index.tsv` is the versioned phase anchor for the harness. It records the
known phases, their operational status, primary blueprint, expected runtime
report path and implementation commit when available.

The TSV columns are:

```text
order	id	status	blueprint	report	commit
```

Allowed MVP statuses:

- `planned`: known, but not selected for execution.
- `next`: the next phase selected for runner preparation.
- `in_progress`: execution has started.
- `implemented`: executed and committed, but not necessarily approved for
  merge.
- `approved`: approved by the Human Reviewer.
- `blocked`: blocked by an explicit issue or decision.
- `skipped`: intentionally skipped.

Exactly one line should use `next` at a time. `run-next-phase.sh` validates this
and fails if there are zero or multiple `next` entries.

The runner reads the phase marked `next`, validates that its blueprint exists,
delegates derived artifact preparation to `harness/bin/run-phase.sh`, and
generates a Codex prompt under `harness/bin/build/prompts/`.

The runner does not update this index automatically. Moving a phase from
`next` to `in_progress`, `implemented` or `approved` remains a conscious
versioned change. Human approval is still required before merge or push
decisions.
