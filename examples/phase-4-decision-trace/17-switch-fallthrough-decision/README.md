# Phase 4.8 Fixture: Switch Fall-through Decision

This fixture validates Java Decision Trace extraction for syntactically visible
classic switch fall-through case grouping.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/17-switch-fallthrough-decision --entrypoint com.example.decisiontrace.switchfallthrough.StatusDecision.resolve"
```

## Expected

- one `SWITCH_DECISION` decision;
- selector text is `request.status()`;
- fall-through labels `"PENDING"` and `"QUEUED"` are preserved on the same branch;
- default branch is captured.
