# Phase 4.3 Fixture: If/Else Return Branches

This fixture validates Java Decision Trace extraction for a narrow direct
`if/else` block where both branches return simple terminal values.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/06-if-else-return-branches --entrypoint com.example.decisiontrace.ifelse.FeatureToggleDecision.resolve"
```

## Expected

- one `IF_ELSE_CONDITION` decision;
- no unresolved decision items;
- true branch outcome `return true`;
- false branch outcome `return false`;
- source evidence preserved.
