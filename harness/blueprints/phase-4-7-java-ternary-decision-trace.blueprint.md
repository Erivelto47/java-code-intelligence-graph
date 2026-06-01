# Phase 4.7 — Java ternary decision trace

## Goal

Represent Java ternary expressions (`condition ? whenTrue : whenFalse`) as deterministic Decision Trace nodes when they encode business decisions or outcomes.

## Context

Ternaries are common in Java services, mappers, validators, and DTO construction. They should not disappear from Decision Trace just because they are expressions rather than statements.

## Scope

Extract ternary decisions from:

- return expressions;
- variable initializers;
- assignment expressions;
- method argument expressions when practical and deterministic.

Capture:

- condition text;
- true expression text;
- false expression text;
- source location;
- enclosing method/statement context;
- nested ternaries if present, preserving parent/child relation.

## Out of Scope

- evaluating ternary values;
- full expression data flow;
- ternaries inside lambdas/streams if not already accessible by the current parser;
- rewriting ternary to if/else;
- AI interpretation.

## Expected Contract

Recommended decision kind:

```json
{
  "kind": "TERNARY_DECISION",
  "condition": "user.active()",
  "whenTrue": { "text": "Status.ACTIVE" },
  "whenFalse": { "text": "Status.INACTIVE" },
  "sourceLocation": { "file": "...", "line": 10 }
}
```

Generated Markdown should describe ternary branches as deterministic facts, not as inferred intent.

## Fixtures

Add examples under `examples/phase-4-decision-trace/`:

1. `13-return-ternary-decision`
   - `return condition ? valueA : valueB;`

2. `14-assignment-ternary-decision`
   - local variable or field assignment using ternary.

Optional if straightforward:

3. nested ternary fixture.

## Tests

Assert:

- ternary condition/true/false expressions are captured exactly;
- return ternary has outcome context;
- assignment ternary has assignment context;
- nested ternary preserves hierarchy;
- generated fixture artifacts match exactly.

## Acceptance Criteria

- `./gradlew clean test` passes.
- Ternaries are represented separately from `if` decisions.
- Existing if/else behavior is unchanged.
- No AI interpretation is introduced.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/` with implementation summary, fixtures, tests, command results, and limitations.
