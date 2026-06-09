# Phase 4.6 — Java boolean condition expression trace

## Goal

Extend Decision Trace to capture the structure of Java boolean expressions inside decision conditions, preserving deterministic facts such as operators, operands, grouping, and negation.

## Context

Earlier phases preserve condition text, but richer AI/context consumers need to understand composed rules such as `name != null && !name.isBlank()` without guessing the structure.

## Scope

Represent method-local boolean condition expressions for existing decision nodes:

- `&&`
- `||`
- `!`
- parentheses/grouping;
- comparison operators: `==`, `!=`, `<`, `<=`, `>`, `>=`;
- method-call predicates, for example `isBlank()`, `isEmpty()`, `contains(...)`;
- simple boolean variables and fields;
- preserve original source text for every expression node.

## Out of Scope

- full data flow;
- evaluating expression truth;
- simplifying boolean algebra;
- resolving runtime values;
- cross-method predicate expansion;
- switch/ternary/Optional/Stream behavior.

## Expected Contract

Add a deterministic `conditionExpression` or equivalent field to decision artifacts when a condition is composed.

Recommended shape:

```json
{
  "condition": "name != null && !name.isBlank()",
  "conditionExpression": {
    "kind": "AND",
    "text": "name != null && !name.isBlank()",
    "operands": [
      { "kind": "COMPARISON", "operator": "!=", "left": "name", "right": "null", "text": "name != null" },
      { "kind": "NOT", "text": "!name.isBlank()", "operand": { "kind": "METHOD_CALL", "text": "name.isBlank()" } }
    ]
  }
}
```

Use the existing model style if different; the important part is deterministic structure plus original text.

## Fixtures

Add examples under `examples/phase-4-decision-trace/`:

1. `11-boolean-and-or-not-condition`
   - condition with `&&`, `||`, `!`, and parentheses.

2. `12-comparison-and-method-predicate-condition`
   - comparison operators plus method predicate calls.

## Tests

Assert:

- operator precedence/grouping is preserved;
- parentheses affect expression tree;
- negation wraps the correct operand;
- original source text is preserved;
- condition extraction remains backward compatible for simple conditions;
- fixture outputs match exactly.

## Acceptance Criteria

- `./gradlew clean test` passes.
- Existing decision nodes still expose the original `condition` text.
- Boolean expression structure is deterministic and stable.
- No expression evaluation or AI interpretation is introduced.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/` with command results, files changed, fixtures, tests, and limitations.
