# Phase 4.3.1 Fixture: If/Else Throw/Return Branches

Validates narrow Java Decision Trace extraction for direct block `if/else`
branches where one branch throws a literal exception and the other branch
returns a simple value.

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/07-if-else-throw-return-branches --entrypoint com.example.decisiontrace.ifelsemixed.AccessDecision.resolve"
```
