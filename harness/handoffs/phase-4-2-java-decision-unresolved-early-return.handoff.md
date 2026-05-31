# Phase 4.2 Java Decision Unresolved + Early Return Handoff

## Handoff id

`PHASE-4.2-JAVA-DECISION-UNRESOLVED-EARLY-RETURN`

## Date

2026-05-27

## From role

ChatGPT Web Architect

## To role

Codex CLI Executor

## Phase

Phase 4.2 — Java Decision Unresolved + Early Return

## Primary blueprint path

```text
harness/blueprints/phase-4-2-java-decision-unresolved-early-return.blueprint.md
```

## Blueprint execution status

Execution requested by the user on 2026-05-27. Human Reviewer approval remains
required before merge or push.

## Derived artifact status

This handoff is a derived and reviewable operational artifact. It is derived
from the primary blueprint, the harness template and project conventions. If it
conflicts with the blueprint, pause execution and correct this handoff before
continuing.

## Objective

Execute Phase 4.2 by expanding Java Decision Trace with deterministic
unresolved records for recognized unsupported decision shapes and conservative
simple early return extraction.

## Context

The project already has the Decision Trace contract, Java `if` + `throw`
extractor, Java source decision adapter, `application.decision` layer,
canonical `decisions.json` formatting and the
`examples/phase-4-decision-trace/01-if-throw-validation` fixture.

Use the versioned blueprint:

```text
harness/blueprints/phase-4-2-java-decision-unresolved-early-return.blueprint.md
```

## Branch strategy

- Do not create the Phase 4.2 branch from `master`.
- Create the Phase 4.2 branch from the current consolidated Phase 4 / harness
  working branch.
- Run `git status` and `git branch --show-current` before changing files.
- Do not merge to `master`.
- Do not push unless explicitly requested.
- Respect the versioned blueprint and keep the implementation scoped to Phase
  4.2.

## Scope

- Add deterministic unresolved records for recognized Java decision shapes that
  are not safely supported.
- Add conservative extraction for simple `if` + `return` / early return.
- Keep Java-specific source parsing in
  `com.codeatlas.adapter.java.source.decision`.
- Keep core Decision Trace language-agnostic.
- Preserve compatibility of `decisions.json`, `decisions.md` and
  `decisions.mmd`.
- Add or update focused fixtures and tests for the supported and unresolved
  shapes.

## Out of scope

- Full `if/else` support.
- Nested propagation.
- Cross-method propagation.
- `Optional`, stream, lambda or method reference analysis.
- Kotlin support.
- JS/TS support.
- AI interpretation.
- Runtime analysis.
- Advanced data flow.
- Flow Graph changes unless strictly required and explicitly justified.
- Project Index changes unless strictly required and explicitly justified.
- Merge to `master`.
- Automatic push.

## Expected files/areas

- `src/main/java/com/codeatlas/adapter/java/source/decision/`
- `src/main/java/com/codeatlas/core/decision/`
- `src/main/java/com/codeatlas/output/decision/`
- `src/test/java/com/codeatlas/`
- `examples/phase-4-decision-trace/`
- `docs/phase-4-decision-trace-contract.md`, only if the contract needs a
  precise compatibility clarification.

## Required validations

```bash
git status
git branch --show-current
./gradlew test
./gradlew build
git diff --check
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/01-if-throw-validation --entrypoint com.example.UserService.create"
```

Also run Phase 4.2 fixture commands after the new fixture names are finalized.

## Expected report path

```text
harness/reports/runs/PHASE_4_2_JAVA_DECISION_UNRESOLVED_EARLY_RETURN_REPORT.md
```

## Completion criteria

- Recognized unsupported Java decision shapes produce deterministic unresolved
  records instead of being silently ignored.
- Simple early return cases produce Decision Trace decisions with `RETURN`
  outcome.
- Existing `01-if-throw-validation` fixture still passes.
- New or updated fixtures compare `decisions.json`, `decisions.md` and
  `decisions.mmd` exactly.
- Core Decision Trace remains language-agnostic.
- Java-specific logic stays in the Java adapter package.
- Flow Graph and Project Index are unchanged unless explicitly justified.
- Final report is generated in `harness/reports/runs/`.
- Human Reviewer approves before merge or push.

## Risks

- Unresolved detection can become noisy if it matches too broadly.
- Early return may be over-classified as a business decision.
- Source-text parsing can become brittle if the supported shape set expands.
- Contract changes can accidentally affect existing fixtures.

## Open questions

- Should Phase 4.2 reuse existing fixture numbering if similar draft fixtures
  already exist, or create the exact suggested fixture names from the blueprint?
- Should unsupported single-line conditional throw and unsupported block throw
  share one unresolved kind or use more specific reasons?

## Human approval required before merge/push

Yes. Merge and push remain blocked until the Human Reviewer explicitly approves
them.
