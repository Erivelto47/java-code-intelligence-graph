# Phase 4.2 Java Decision Unresolved + Early Return Validation Checklist

## Branch

Run before implementation:

```bash
git status
git branch --show-current
```

Confirm:

- The Phase 4.2 branch was not created from `master` unless the Human Reviewer
  explicitly approved consolidation first.
- The branch continues from the current consolidated Phase 4 / harness working
  branch.
- There is no merge to `master`.
- There is no push unless explicitly requested.

## Build/tests

Run:

```bash
./gradlew test
./gradlew build
git diff --check
```

Expected result:

- All commands pass.
- Any unrelated failure is documented in the Phase 4.2 report with command,
  output summary and impact.

## CLI

Existing fixture regression:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/01-if-throw-validation --entrypoint com.example.UserService.create"
```

Future early return fixture placeholder:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/02-if-return-early-return --entrypoint <early-return-entrypoint>"
```

Future unresolved decision shapes fixture placeholder:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/03-unresolved-decision-shapes --entrypoint <unresolved-entrypoint>"
```

If equivalent fixture names are chosen, replace the placeholder paths with the
final versioned fixture paths and record the decision in the report.

## Artifact comparisons

For each Phase 4.2 fixture, compare generated artifacts exactly against
`expected/`:

- `decisions.json` raw exact comparison.
- `decisions.md` raw exact comparison.
- `decisions.mmd` raw exact comparison.

The existing `DecisionTraceFixtureContractTest` pattern is the preferred
baseline for exact comparisons.

## Regression

Run the standard flow fixture command when checking that Flow Graph behavior was
not changed:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

Confirm:

- Flow Graph output did not change without an explicit reason.
- Project Index output did not change without an explicit reason.
- Existing Decision Trace fixture `01-if-throw-validation` still passes.

## Out-of-scope verification

Confirm the implementation did not add:

- Kotlin support.
- JS/TS support.
- AI interpretation.
- Runtime analysis.
- Advanced data flow.
- Full `if/else`.
- Nested propagation.
- Cross-method propagation.
- Unnecessary Flow Graph changes.
- Unnecessary Project Index changes.

## Report generated

Expected path:

```text
harness/reports/runs/PHASE_4_2_JAVA_DECISION_UNRESOLVED_EARLY_RETURN_REPORT.md
```

The report must include validation commands, results, artifact comparison
status, files changed, risks, out-of-scope confirmation and next-step
recommendation.
