# Phase 4.9 — Java Optional decision trace

## Goal

Represent common Java `Optional` decision idioms as deterministic Decision Trace facts.

## Context

Java applications frequently encode validation, fallback, and error behavior through `Optional`. These decisions are not explicit `if` statements, but they affect outcomes such as throwing, returning defaults, or conditionally executing logic.

## Scope

Extract method-local Optional decision patterns:

- `orElseThrow(...)`;
- `orElse(...)`;
- `orElseGet(...)`;
- `ifPresent(...)`;
- `ifPresentOrElse(...)`;
- `map(...).orElse...` and `filter(...).orElse...` only when syntactically straightforward;
- preserve original call chain text.

Capture:

- source Optional expression/call chain;
- decision kind;
- present branch behavior if visible;
- empty branch behavior if visible;
- thrown exception text for `orElseThrow` when available;
- fallback value/supplier text for `orElse`/`orElseGet`.

## Out of Scope

- full lambda body extraction beyond simple visible bodies;
- deep data flow through Optional pipelines;
- resolving generic types;
- evaluating presence/absence;
- AI interpretation.

## Expected Contract

Recommended shape:

```json
{
  "kind": "OPTIONAL_OR_ELSE_THROW_DECISION",
  "expression": "repository.findById(id).orElseThrow(() -> new NotFoundException(id))",
  "emptyOutcome": {
    "kind": "THROW",
    "expression": "new NotFoundException(id)"
  }
}
```

For `ifPresentOrElse`:

```json
{
  "kind": "OPTIONAL_IF_PRESENT_OR_ELSE_DECISION",
  "presentBranch": { "text": "user -> notify(user)" },
  "emptyBranch": { "text": "() -> auditMissing(id)" }
}
```

## Fixtures

Add examples under `examples/phase-4-decision-trace/`:

1. `18-optional-or-else-throw-decision`
2. `19-optional-or-else-fallback-decision`
3. `20-optional-if-present-or-else-decision`

## Tests

Assert:

- Optional call chain text is preserved;
- `orElseThrow` produces throw/outcome facts;
- fallback expressions are captured;
- present/empty branches are distinct for `ifPresentOrElse`;
- simple Optional chains do not break existing method-local call extraction;
- fixture artifacts match exactly.

## Acceptance Criteria

- `./gradlew clean test` passes.
- Optional decisions are represented as their own decision kind or equivalent deterministic shape.
- Unsupported complex lambdas are either ignored deterministically or marked unresolved according to existing contract.
- No AI interpretation is introduced.

## Completion Report

Generate a non-versioned report in `harness/reports/runs/` with implementation summary, fixtures, tests, command results, and limitations.
