# Phase 4.7 Fixture: Assignment Ternary Decision

This fixture validates Java Decision Trace extraction for a ternary expression
used in a local variable initializer.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/14-assignment-ternary-decision --entrypoint com.example.decisiontrace.ternaryassignment.StatusDecision.resolve"
```

## Expected

- one `TERNARY_CONDITION` decision;
- condition text is `request.active()`;
- true expression text is `"active"`;
- false expression text is `"inactive"`;
- branch outcomes preserve assignment context and target `label`.
