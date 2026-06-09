# Phase 4.5 Fixture: Else-If Chain

This fixture validates Java Decision Trace extraction for a direct
`if/else if/else` chain with ordered branches and terminal outcomes.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/09-else-if-chain --entrypoint com.example.decisiontrace.elseif.AccessDecision.resolve"
```

## Expected

- one `IF_ELSE_IF_CHAIN` decision;
- branch order is preserved across `if`, multiple `else if` branches and `else`;
- one branch throws;
- multiple branches return;
- source evidence preserves the full chain.
