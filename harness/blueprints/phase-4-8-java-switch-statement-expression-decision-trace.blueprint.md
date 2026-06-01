# Phase 4.8 — Java switch statement/expression decision trace

## Goal

Represent Java `switch` statements and switch expressions as deterministic Decision Trace artifacts.

## Context

Switch constructs encode important branching rules in Java applications. Modern Java also includes switch expressions with `yield` and arrow cases, which should be represented without mixing deterministic extraction with interpretation.

## Scope

Extract decisions from:

- classic `switch` statements;
- case labels with constants/enums/strings;
- `default` cases;
- fall-through cases when syntactically visible;
- arrow switch cases (`case X -> ...`);
- switch expressions assigned to variables or returned;
- `yield` in switch expressions.

Capture:

- selector expression;
- ordered cases;
- case labels;
- default case;
- branch body summary/outcomes where existing outcome extraction supports it;
- source locations.

## Out of Scope

- exhaustive enum analysis;
- runtime matching/evaluation;
- pattern matching switch unless trivially captured as text;
- data flow through switch branch variables;
- AI interpretation.

## Expected Contract

Recommended shape:

```json
{
  "kind": "SWITCH_DECISION",
  "selector": "status",
  "cases": [
    { "labels": ["PENDING"], "kind": "CASE", "outcomes": [] },
    { "labels": ["APPROVED", "ACTIVE"], "kind": "CASE", "outcomes": [] },
    { "kind": "DEFAULT", "outcomes": [] }
  ]
}
```

For switch expressions, include expression context:

```json
{
  "kind": "SWITCH_EXPRESSION_DECISION",
  "selector": "type",
  "assignedTo": "fee",
  "cases": []
}
```

## Fixtures

Add examples under `examples/phase-4-decision-trace/`:

1. `15-switch-statement-decision`
   - classic switch with multiple cases and default.

2. `16-switch-expression-decision`
   - switch expression assigned or returned.

3. Optional: `17-switch-fallthrough-decision`
   - classic fall-through case grouping.

## Tests

Assert:

- selector is captured;
- case order is preserved;
- multiple labels are preserved;
- default is captured;
- switch expression `yield`/arrow result is captured as branch output when practical;
- fixture artifacts match exactly.

## Acceptance Criteria

- `./gradlew clean test` passes.
- Switch statement and expression outputs are deterministic.
- Existing if/ternary/boolean behavior remains stable.
- No AI interpretation is introduced.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/` with implementation summary, fixtures, tests, command results, and limitations.
