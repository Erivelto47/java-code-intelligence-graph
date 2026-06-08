# Phase 4.8 Fixture: Switch Expression Decision

This fixture validates Java Decision Trace extraction for a switch expression
assigned to a variable, including arrow and `yield` branch outputs.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/16-switch-expression-decision --entrypoint com.example.decisiontrace.switchexpression.FeeDecision.resolve"
```

## Expected

- one `SWITCH_EXPRESSION_DECISION` decision;
- selector text is `request.type()`;
- `assignedTo` is `fee`;
- arrow expression outputs are captured as branch assignments;
- block `yield` output is captured as a branch assignment;
- default branch is captured.
