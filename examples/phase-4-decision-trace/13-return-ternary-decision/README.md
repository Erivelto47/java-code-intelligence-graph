# Phase 4.7 Fixture: Return Ternary Decision

This fixture validates Java Decision Trace extraction for a ternary expression
used directly as a return expression.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/13-return-ternary-decision --entrypoint com.example.decisiontrace.ternaryreturn.StatusDecision.resolve"
```

## Expected

- one `TERNARY_CONDITION` decision;
- condition text is `request.active()`;
- true expression text is `"active"`;
- false expression text is `"inactive"`;
- branch outcomes preserve return context.
