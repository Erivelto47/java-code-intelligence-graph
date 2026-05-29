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

Use the next phase runner when `harness/phases/phase-index.tsv` marks exactly
one phase as `next`:

```bash
./harness/bin/run-next-phase.sh
```

The runner:

- reads `harness/phases/phase-index.tsv`;
- validates that exactly one phase is marked `next`;
- validates that the next phase blueprint exists;
- calls `./harness/bin/run-phase.sh <blueprint>`;
- generates `harness/bin/build/prompts/<phase-id>.codex-prompt.txt`;
- prints the branch, phase id, derived paths, report path and next step.

Dry-run mode delegates to `run-phase.sh --dry-run` and still renders the
temporary Codex prompt under `harness/bin/build/`:

```bash
./harness/bin/run-next-phase.sh --dry-run
```

Limitations:

- no `--phase` override in the MVP;
- no automatic status update in `phase-index.tsv`;
- no Codex/model execution;
- no product implementation;
- no merge;
- no push.
