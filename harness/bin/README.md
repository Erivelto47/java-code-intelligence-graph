# Harness Binaries

This directory contains small operational scripts for the harness.

## run-phase.sh

Use the runner after the Human Reviewer approves a primary blueprint:

```bash
./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

The runner derives the phase id, expected handoff, validation, completion and
runtime report paths, then creates missing derived artifacts from the harness
templates. Existing derived artifacts are left unchanged by default.

Use dry-run mode to inspect paths and branch/status checks without creating
files:

```bash
./harness/bin/run-phase.sh --dry-run harness/blueprints/<phase>.blueprint.md
```

The runner blocks execution on `master` by default. A Human Reviewer can allow
that explicitly for exceptional cases:

```bash
HARNESS_ALLOW_MASTER=1 ./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
```

The runner prepares operational artifacts only. It does not implement product
scope, execute a phase autonomously, call models, push, merge or write the
runtime report.

## run-next-phase.sh

Use the next phase runner to synchronize the phase queue from blueprints,
derive paths from phase ids and prepare the next executable phase:

```bash
./harness/bin/run-next-phase.sh
```

The runner:

- reads `harness/blueprints/*.blueprint.md`;
- synchronizes `harness/phases/phase-index.tsv`;
- keeps existing statuses and commits;
- adds blueprint ids missing from the index as `planned`;
- derives blueprint, handoff, validation, completion, report and prompt paths
  from the phase id;
- validates allowed statuses;
- fails if more than one phase is marked `next`;
- blocks execution if any phase is marked `validation`;
- promotes the first `planned` phase to `next` when there is no `next` and no
  `validation`;
- blocks if the derived report for the `next` phase already exists;
- calls `./harness/bin/run-phase.sh <blueprint>`;
- generates `harness/bin/build/prompts/<phase-id>.codex-prompt.txt`;
- prints the branch, phase id, derived paths, report path, prompt path and next
  step.

Dry-run mode delegates to `run-phase.sh --dry-run` and still renders the
temporary Codex prompt under `harness/bin/build/`. It also synchronizes the
phase index, because the queue itself is versioned harness state:

```bash
./harness/bin/run-next-phase.sh --dry-run
```

Limitations:

- no `--phase` override in the MVP;
- no automatic transition to `implemented`, `approved` or `validation`;
- no Codex/model execution;
- no product implementation;
- no merge;
- no push.
