# Phase 4.6 Fixture: Boolean And/Or/Not Condition

This fixture validates Java Decision Trace condition-expression extraction for
`&&`, `||`, `!`, and explicit grouping parentheses.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/11-boolean-and-or-not-condition --entrypoint com.example.decisiontrace.booleancondition.AccessPolicy.resolve"
```

## Expected

- one `EARLY_RETURN` decision;
- `expression.text` preserves the original condition text;
- `expression.conditionExpression` has an `AND` root;
- the grouped `OR` expression is preserved below a `GROUP` node;
- negation wraps `request.locked()`.
