# Phase 4.10 — Java stream filter/match decision trace

## Goal

Represent common Java Stream decision operations as deterministic Decision Trace facts, focusing on `filter` and match predicates.

## Context

Business rules are often encoded in Stream pipelines, especially filtering, validation, and existence checks. The MVP should capture the decision predicate and the operation shape without attempting full data flow.

## Scope

Extract method-local Stream decision patterns:

- `.filter(predicate)`;
- `.anyMatch(predicate)`;
- `.allMatch(predicate)`;
- `.noneMatch(predicate)`;
- simple terminal outcomes around match results, for example `if (items.stream().anyMatch(...))`;
- preserve original pipeline/predicate text;
- capture lambda parameter text and body/expression text when simple.

## Out of Scope

- full pipeline data flow;
- collectors semantics;
- complex multi-statement lambdas beyond deterministic text capture;
- stream source type resolution;
- parallel stream semantics;
- AI interpretation.

## Expected Contract

Recommended shape:

```json
{
  "kind": "STREAM_MATCH_DECISION",
  "operation": "anyMatch",
  "pipeline": "items.stream().anyMatch(item -> item.invalid())",
  "predicate": {
    "parameter": "item",
    "text": "item.invalid()"
  }
}
```

For filter:

```json
{
  "kind": "STREAM_FILTER_DECISION",
  "operation": "filter",
  "predicate": {
    "parameter": "user",
    "text": "user.active()"
  }
}
```

## Fixtures

Add examples under `examples/phase-4-decision-trace/`:

1. `21-stream-filter-decision`
2. `22-stream-any-match-decision`
3. `23-stream-all-none-match-decision`

## Tests

Assert:

- operation name is captured;
- predicate text is captured;
- lambda parameter is captured for simple lambdas;
- match inside `if` condition links to the enclosing decision when practical;
- unsupported complex cases are deterministic and do not crash;
- fixture artifacts match exactly.

## Acceptance Criteria

- `./gradlew clean test` passes.
- Stream predicate decisions are visible in JSON/Markdown/Mermaid where supported.
- Existing Optional/ternary/switch/if behavior remains stable.
- No AI interpretation or full data flow is introduced.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/` with implementation summary, fixtures, tests, command results, and limitations.
