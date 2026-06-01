# Phase 4.5 Fixture: Nested If Decision

This fixture validates Java Decision Trace extraction for nested `if` decisions
inside both the positive and `else` branches of an outer decision.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/10-nested-if-decision --entrypoint com.example.decisiontrace.nestedif.RoutingDecision.resolve"
```

## Expected

- one outer `IF_ELSE_CONDITION` decision with branch metadata;
- one nested decision owned by the true branch;
- one nested decision owned by the false branch;
- nested return and throw outcomes are preserved;
- generated JSON, Markdown and Mermaid expose parent/child context.
