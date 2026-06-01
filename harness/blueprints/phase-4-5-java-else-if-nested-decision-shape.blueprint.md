# Phase 4.5 — Java else-if and nested decision shape

## Goal

Extend Decision Trace to represent Java `else if` chains and nested `if` decisions deterministically, without AI interpretation and without changing Flow Graph artifacts.

## Context

Phase 4 already supports basic Java `if`, `if/else`, throw/return decisions, mixed throw/return branches, and method-local helper calls. This phase closes the gap for common branching shapes where decisions are chained or nested.

## Scope

Implement deterministic extraction for:

- `if (...) { ... } else if (...) { ... } else { ... }`
- multiple `else if` branches;
- nested `if` blocks inside `then`, `else`, and `else if` branches;
- branch ordering preservation;
- parent/child relation between nested decisions;
- branch outcome detection when nested branches throw or return.

## Out of Scope

- boolean expression normalization beyond preserving the condition text;
- switch statements/expressions;
- ternary expressions;
- interprocedural traversal beyond already supported method-local call references;
- AI interpretation;
- data flow analysis.

## Expected Contract

`decisions.json` should make the chain and nesting explicit enough for downstream agents to answer:

- what is checked first;
- what is checked next if the first condition fails;
- which branch owns each outcome;
- which decision is nested inside another branch.

Recommended shape additions, if compatible with the current contract:

```json
{
  "kind": "IF_ELSE_IF_CHAIN",
  "condition": "request == null",
  "branches": [
    { "kind": "IF", "condition": "request == null", "order": 1 },
    { "kind": "ELSE_IF", "condition": "request.name() == null", "order": 2 },
    { "kind": "ELSE", "order": 3 }
  ],
  "children": [
    { "kind": "IF_CONDITION", "parentBranchOrder": 3 }
  ]
}
```

Keep the implementation aligned with the existing core model. Do not force this exact schema if the current model has a better equivalent; preserve backward compatibility.

## Fixtures

Add at least these examples under `examples/phase-4-decision-trace/`:

1. `09-else-if-chain`
   - one method with `if/else if/else`;
   - at least one branch throws;
   - at least one branch returns.

2. `10-nested-if-decision`
   - one outer `if`;
   - nested `if` inside the positive branch;
   - nested `if` inside the `else` branch if practical.

Each fixture should include expected generated artifacts according to the existing fixture convention.

## Tests

Add or extend tests to assert:

- `else if` branch order is preserved;
- `else` is not confused with `else if`;
- nested decisions keep parent/branch context;
- outcomes inside nested decisions are preserved;
- generated fixture artifacts match exactly.

Suggested test areas:

- Java decision extractor unit tests;
- fixture contract tests;
- writer tests if schema/output rendering changes.

## Acceptance Criteria

- `./gradlew clean test` passes.
- New fixtures are deterministic and stable.
- `decisions.json`, `decisions.md`, and `decisions.mmd` expose else-if/nested structure clearly.
- No AI interpretation is introduced.
- Existing Phase 4 fixtures remain unchanged unless there is a deliberate contract-compatible improvement.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/` summarizing:

- implementation files changed;
- fixtures added;
- tests added/updated;
- command results;
- compatibility notes;
- unresolved limitations.
