# Harness 0.4.1 — Runner Prompt Output Path Blueprint

## Primary input status

This blueprint is the primary input and source of truth for this harness microphase.

Derived artifacts such as handoff, validation and completion should be generated or updated from this blueprint by the harness runner and reviewed before execution.

## Phase

Harness 0.4.1 — Runner Prompt Output Path

## Context

Harness 0.4 introduced a high-level next phase runner:

```bash
./harness/bin/run-next-phase.sh
```

The runner reads:

```text
harness/phases/phase-index.tsv
```

finds the single phase marked as `next`, delegates to:

```bash
./harness/bin/run-phase.sh
```

and generates a Codex execution prompt.

Current prompt output path:

```text
build/harness/prompts/<phase-id>.codex-prompt.txt
```

Desired prompt output path:

```text
harness/bin/build/prompts/<phase-id>.codex-prompt.txt
```

## Motivation

The generated Codex prompt is a temporary operational artifact produced by the harness scripts.

The root-level `build/` directory is appropriate for Gradle/build outputs, but the Codex prompt is more closely related to the harness script execution flow.

Keeping the generated prompt under:

```text
harness/bin/build/prompts/
```

makes it more visible and discoverable to the person running the harness scripts, while still keeping it unversioned.

This also keeps the temporary output physically close to the harness script setup:

```text
harness/bin/
  run-phase.sh
  run-next-phase.sh
  build/
    prompts/
```

## Objective

Change the generated Codex prompt output directory from:

```text
build/harness/prompts/
```

to:

```text
harness/bin/build/prompts/
```

and ensure the new temporary output directory is ignored by Git.

## Goals

1. Update `run-next-phase.sh` to write generated Codex prompts under `harness/bin/build/prompts/`.
2. Update documentation to reference the new path.
3. Update tests to expect the new path.
4. Add `.gitignore` coverage for `harness/bin/build/`.
5. Keep generated prompts unversioned.
6. Preserve the existing `run-next-phase.sh` behavior otherwise.
7. Preserve the existing `run-phase.sh` behavior.
8. Do not implement or modify product phases.

## Non-goals

Do not implement:

- Phase 4.2.3 or any new product phase;
- Decision Trace behavior changes;
- Flow Graph changes;
- Project Index changes;
- model routing;
- token tracking;
- context tracking;
- runtime observability;
- executable tool registry;
- autonomous Codex invocation;
- CI or GitHub Actions;
- merge to `master`;
- push.

## Expected behavior after this phase

Running:

```bash
./harness/bin/run-next-phase.sh
```

should generate a prompt at:

```text
harness/bin/build/prompts/<phase-id>.codex-prompt.txt
```

For example:

```text
harness/bin/build/prompts/phase-4-2-2-java-single-line-if-throw.codex-prompt.txt
```

The runner output should print this new path.

The generated prompt should not be staged or committed.

## Git ignore policy

Add or confirm `.gitignore` entry:

```gitignore
harness/bin/build/
```

Use this broader rule instead of only ignoring `harness/bin/build/prompts/`, because future harness script temporary outputs may also belong under `harness/bin/build/`.

## Codex Desktop note

This microphase may be tested through Codex Desktop instead of the IntelliJ plugin.

The implementation should not assume IntelliJ plugin behavior.

All validation should remain command-line based and reproducible from the repository root.

## Expected files to inspect or update

Likely files:

```text
harness/bin/run-next-phase.sh
harness/bin/README.md
harness/README.md
harness/prompts/README.md
harness/workflows/phase-execution.workflow.md
src/test/java/com/codeatlas/harness/NextPhaseRunnerScriptTest.java
.gitignore
```

Possibly inspect:

```text
harness/bin/run-phase.sh
harness/phases/phase-index.tsv
harness/prompts/codex-execution-prompt.template.txt
```

Do not alter these unless necessary.

## Suggested implementation order

1. Run `git status` and confirm current branch.
2. Inspect current `run-next-phase.sh` prompt output path.
3. Change prompt output directory to `harness/bin/build/prompts/`.
4. Update docs that mention `build/harness/prompts/`.
5. Update tests that assert prompt path.
6. Add `.gitignore` rule for `harness/bin/build/`.
7. Run the next phase runner in dry-run and normal mode.
8. Confirm generated prompt appears under `harness/bin/build/prompts/`.
9. Confirm generated prompt is ignored by Git.
10. Generate runtime report under `harness/reports/runs/`.

## Validation requirements

Required commands:

```bash
git status
git branch --show-current
bash -n harness/bin/run-next-phase.sh
./harness/bin/run-next-phase.sh --dry-run
./harness/bin/run-next-phase.sh
./gradlew test --tests com.codeatlas.harness.NextPhaseRunnerScriptTest
./gradlew test
./gradlew build
git diff --check
```

Also verify:

```bash
git status --short
```

Expected:

- generated prompt under `harness/bin/build/prompts/`;
- generated prompt is ignored by Git;
- `harness/reports/runs/` report is ignored by Git;
- pre-existing untracked `makefile`, if present, remains untouched.

## Completion criteria

This phase is complete only when:

1. `run-next-phase.sh` generates Codex prompts under `harness/bin/build/prompts/`.
2. The old output path `build/harness/prompts/` is no longer referenced by active docs/tests/scripts.
3. `.gitignore` ignores `harness/bin/build/`.
4. `run-next-phase.sh --dry-run` passes.
5. `run-next-phase.sh` passes.
6. `NextPhaseRunnerScriptTest` passes.
7. `./gradlew test` passes.
8. `./gradlew build` passes.
9. `git diff --check` passes.
10. No product behavior is changed.
11. No Decision Trace, Flow Graph or Project Index behavior is changed.
12. Runtime report is generated under `harness/reports/runs/`.
13. No push or merge is performed.

## Runtime report path

Generate the report at:

```text
harness/reports/runs/HARNESS_0_4_1_RUNNER_PROMPT_OUTPUT_PATH_REPORT.md
```

## Report requirements

The report should contain:

```text
# Harness 0.4.1 Runner Prompt Output Path Report

## Summary

## Motivation

## Branch strategy used

## What changed

## Prompt output path before

## Prompt output path after

## Gitignore behavior

## Codex Desktop note

## Documentation updated

## Tests executed

## Test results

## Files changed

## Out of scope confirmation

## Known risks

## Suggested next phase
```

## Branch strategy

Do not branch from `master` during the current harness/Phase 4 consolidation cycle unless the Human Reviewer explicitly says the work has been consolidated.

Use the current working branch as the base.

A suitable branch name is:

```text
harness-0-4-1-runner-prompt-output-path
```

Do not merge to `master`.

Do not push automatically.

## Out of scope confirmation required in report

The final report must explicitly confirm that the phase did not add:

- product phase implementation;
- Decision Trace behavior changes;
- Flow Graph changes;
- Project Index changes;
- model routing;
- token tracking;
- context tracking;
- runtime observability;
- executable tool registry;
- CI or GitHub Actions;
- merge to `master`;
- push.

## Known risks

1. Existing local generated prompts under `build/harness/prompts/` may remain on disk, but they should no longer be produced by the runner.
2. Documentation may contain old references to `build/harness/prompts/`; search and update active docs.
3. The new path is intentionally inside `harness/bin/build/`, so `.gitignore` must prevent accidental commits.
4. Codex Desktop should run the same shell commands from the repo root; avoid tool-specific assumptions.

## Suggested next phase

After this microphase, execute the next product phase using:

```bash
./harness/bin/run-next-phase.sh
```

and consume the generated prompt from:

```text
harness/bin/build/prompts/<phase-id>.codex-prompt.txt
```

Then proceed with the next `next` phase from `harness/phases/phase-index.tsv`.
