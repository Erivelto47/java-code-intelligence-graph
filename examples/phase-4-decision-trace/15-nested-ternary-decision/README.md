# Phase 4.7 Fixture: Nested Ternary Decision

This fixture validates Java Decision Trace extraction for a nested ternary and
parent/child relationship preservation.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/15-nested-ternary-decision --entrypoint com.example.decisiontrace.ternarynested.StatusDecision.resolve"
```

## Expected

- one parent `TERNARY_CONDITION` decision for `request.active()`;
- one child `TERNARY_CONDITION` decision for `request.admin()`;
- child decision is attached to the parent `WHEN_TRUE` branch;
- return branch expression text is preserved.
