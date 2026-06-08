# Phase 4.8 Fixture: Switch Statement Decision

This fixture validates Java Decision Trace extraction for a classic switch
statement with ordered cases, multiple labels and a default branch.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/15-switch-statement-decision --entrypoint com.example.decisiontrace.switchstatement.StatusDecision.resolve"
```

## Expected

- one `SWITCH_DECISION` decision;
- selector text is `request.status()`;
- case order is preserved;
- labels `"APPROVED"` and `"ACTIVE"` are preserved on the same branch;
- default branch is captured.
