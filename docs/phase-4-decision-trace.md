# Phase 4 - Decision Trace

## Objective

Phase 4 adds deterministic decision extraction to Code Atlas. Decision Trace
captures source-level rules such as validations, conditional throws, early
returns, and outcomes in artifacts that are separate from architecture and flow
artifacts.

## Flow Graph vs Decision Trace

Flow Graph answers how methods call each other and where technical boundaries
exist. Decision Trace answers what deterministic rules exist inside a method and
what outcome each rule produces.

Decision Trace must not inflate `flow.json`, `project-index.json`, or
`entrypoints.json`. It writes its own artifacts:

```text
.code-atlas/decisions/<entrypoint-or-scope>/decisions.json
.code-atlas/decisions/<entrypoint-or-scope>/decisions.md
.code-atlas/decisions/<entrypoint-or-scope>/decisions.mmd
```

JSON is the primary artifact. Markdown and Mermaid are derived views.

## Phase 4.1 Scope

Phase 4.1 implements the first deterministic extractor for one simple Java
shape:

```java
if (condition) {
    throw new SomeException("message");
}
```

Supported command:

```bash
./gradlew run --args="analyze-decisions --project ./repo --entrypoint com.company.FooService.method"
```

Example:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/01-if-throw-validation --entrypoint com.example.UserService.create"
```

Expected output:

```text
.code-atlas/decisions/com/example/UserService/create/decisions.json
.code-atlas/decisions/com/example/UserService/create/decisions.md
.code-atlas/decisions/com/example/UserService/create/decisions.mmd
```

## Example Output

For this Java code:

```java
if (request.name() == null || request.name().isBlank()) {
    throw new IllegalArgumentException("Name is required");
}
```

Decision Trace emits:

- condition: `request.name() == null || request.name().isBlank()`
- decision kind: `CONDITIONAL_THROW`
- category: `VALIDATION`
- outcome: `THROW IllegalArgumentException`
- message: `Name is required`
- source method: `com.example.UserService.create`
- source location when available

## Known Limitations

Phase 4.1 intentionally does not support:

- `if (...) throw new SomeException("message");`
- `if` blocks with logging or other statements before the throw.
- `throw createException()`.
- `return` decisions.
- Optional chains.
- Streams.
- Lambdas.
- Method references.
- Try/catch.
- Switch.
- Nested decision propagation.
- Cross-method decision propagation.
- AI-based interpretation.

Unsupported shapes are ignored in this MVP. Later Phase 4 increments should add
decision-specific unresolved entries for unsupported but recognized constructs.

## Next Steps

- Add endpoint support for `analyze-decisions`.
- Add unresolved decision reporting for unsupported shapes.
- Add `EARLY_RETURN` support.
- Add limited `IF_ELSE_CONDITION` support.
- Link decisions to Flow Graph nodes when a flow exists.
- Add optional project-wide `index-decisions`.
- Keep AI interpretation reserved for Phase 5.
