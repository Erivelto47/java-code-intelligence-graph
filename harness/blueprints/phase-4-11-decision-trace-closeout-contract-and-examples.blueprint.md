# Phase 4.11 — Decision Trace closeout contract and examples

## Goal

Close the Phase 4 MVP by consolidating the Decision Trace contract, examples, limitations, and validation matrix after phases 4.5 through 4.10.

## Context

Phase 4 should not claim to cover all Java decision semantics. It should close as a deterministic method-local MVP for common Java decision structures, with explicit boundaries and future work.

## Scope

Consolidate documentation and examples for:

- if / else / else-if;
- nested if;
- throw/return outcomes;
- method-local decision calls;
- boolean condition expression trace;
- ternary decisions;
- switch statement/expression decisions;
- Optional decision idioms;
- Stream filter/match decision idioms.

Update or create:

- `docs/phase-4-decision-trace-contract.md`;
- a Phase 4 closeout document, for example `docs/phase-4-decision-trace-closeout.md`;
- fixture index/README under `examples/phase-4-decision-trace/`;
- harness phase index status for completed phases;
- any generated documentation that explains limitations and next phase handoff.

## Out of Scope

- adding new extractor features;
- starting Phase 5 AI interpretation;
- broad Java data flow;
- interprocedural resolution beyond already implemented references;
- PSI adapter changes unless documentation needs to clarify future adapter boundaries.

## Expected Contract Clarifications

The closeout must clearly state:

- deterministic facts only;
- no AI interpretation in Phase 4 artifacts;
- method-local scope of the MVP;
- JSON is primary;
- Markdown/Mermaid are derived;
- unsupported constructs should be ignored, marked unresolved, or captured as text according to documented rules;
- limitations are intentional and versioned.

## Validation Matrix

Create a matrix with columns similar to:

```text
Construct | Supported | Artifact Shape | Fixture | Test | Notes
```

Rows should include all Phase 4 MVP constructs and known out-of-scope constructs.

## Tests

No new feature tests are required unless documentation generation or fixture index generation has tests. Run the full test suite and verify all Phase 4 fixture contract tests pass.

## Acceptance Criteria

- `./gradlew clean test` passes.
- Phase 4 MVP scope is explicit and not overclaimed.
- Contract documentation matches implemented behavior.
- Fixture index is complete.
- Harness phase index is ready to mark Phase 4 closeout as implemented after validation.
- A final closeout report states whether Phase 4 MVP is ready to close.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/PHASE_4_11_DECISION_TRACE_CLOSEOUT_REPORT.md` containing:

- commands executed;
- docs updated;
- fixture matrix summary;
- remaining limitations;
- recommendation: close Phase 4 MVP yes/no;
- recommended next branch for Phase 5.
