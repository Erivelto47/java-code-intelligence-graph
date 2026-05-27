# Phase 4.2 Java Decision Unresolved + Early Return Completion Criteria

## Objective

Phase 4.2 is complete when Java Decision Trace emits deterministic unresolved
records for recognized unsupported decision shapes and extracts simple early
return decisions conservatively, without changing unrelated Flow Graph or
Project Index behavior.

## Required implementation results

- Specific unresolved records are generated for recognized Java decision shapes
  that are not supported safely.
- Simple `if` + `return` / early return cases are extracted as Decision Trace
  decisions.
- Early return decisions include outcome action `RETURN`.
- Unsupported recognized shapes are not converted into decisions with false
  precision.
- Existing Java `if` + `throw` extraction continues to work.
- Core Decision Trace remains language-agnostic.
- Java-specific logic stays in `com.codeatlas.adapter.java.source.decision`.

## Required fixtures

- Existing `examples/phase-4-decision-trace/01-if-throw-validation` remains
  valid.
- A focused early return fixture exists, such as
  `examples/phase-4-decision-trace/02-if-return-early-return` or an equivalent
  project-consistent name.
- A focused unresolved decision shapes fixture exists, such as
  `examples/phase-4-decision-trace/03-unresolved-decision-shapes` or an
  equivalent project-consistent name.

## Required generated artifacts

Each Phase 4.2 fixture must have exact expected artifacts:

- `expected/decisions.json`
- `expected/decisions.md`
- `expected/decisions.mmd`

Runtime generation must continue to write Decision Trace artifacts under:

```text
.code-atlas/decisions/<package-path>/<ClassName>/<methodName>/
```

## Required tests

- Unit or focused tests cover early return extraction.
- Unit or focused tests cover unresolved decision shapes.
- CLI `analyze-decisions` is covered for the relevant fixtures.
- Raw exact fixture comparison covers `decisions.json`.
- Raw exact fixture comparison covers `decisions.md`.
- Raw exact fixture comparison covers `decisions.mmd`.
- Regression confirms `01-if-throw-validation` still passes.
- Regression confirms `analyze-flow` is not broken.
- `./gradlew test` passes.
- `./gradlew build` passes.
- `git diff --check` passes.

## Required docs

- Any necessary contract clarification is documented without broadening Phase
  4.2 beyond the blueprint.
- Fixture READMEs explain the decision shape, expected decision or unresolved
  output and acceptance criteria.
- No documentation claims support for full `if/else`, nested propagation,
  streams, `Optional`, lambdas, Kotlin, JS/TS, runtime analysis or AI
  interpretation.

## Required report

The Phase 4.2 execution report must be generated at:

```text
harness/reports/runs/PHASE_4_2_JAVA_DECISION_UNRESOLVED_EARLY_RETURN_REPORT.md
```

It must include summary, motivation, branch strategy, files changed,
validations, test results, artifact comparison results, risks, out-of-scope
confirmation and suggested next phase.

## Compatibility requirements

- `decisions.json` remains compatible with the existing contract unless an
  explicit, documented contract clarification is required.
- `decisions.md` and `decisions.mmd` remain derived views of the Decision Trace
  data.
- Flow Graph artifacts do not change unless explicitly justified.
- Project Index artifacts do not change unless explicitly justified.

## Out-of-scope confirmation

The final report must explicitly confirm that Phase 4.2 did not add:

- Full `if/else`.
- Nested propagation.
- Cross-method propagation.
- `Optional`, stream, lambda or method reference analysis.
- Kotlin.
- JS/TS.
- AI interpretation.
- Runtime analysis.
- Advanced data flow.
- Unnecessary Flow Graph changes.
- Unnecessary Project Index changes.

## Human reviewer approval

Human Reviewer approval is required before merge or push.

## Ready for next phase

The project is ready for the next phase only after implementation, fixtures,
exact artifact comparisons, build/test validation, report generation and Human
Reviewer approval are complete.
