# Harness Binaries

This directory contains small operational scripts for the harness.

## Blueprint runner skeleton

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
