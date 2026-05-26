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

## Package Architecture

Phase 4.1.1 separates Decision Trace into language-agnostic product contracts,
application orchestration, language adapters, and output writers:

```text
com.codeatlas.core.decision
  Decision Trace records and enums shared by every language adapter.

com.codeatlas.application.decision
  DecisionAnalyzer, DecisionAnalysisRequest, DecisionAnalysisResult,
  LanguageDecisionAdapter, and output path resolution.

com.codeatlas.adapter.java.source.decision
  Java source-text Decision Trace adapter and Java-specific extractors.

com.codeatlas.output.decision.json
com.codeatlas.output.decision.markdown
com.codeatlas.output.decision.mermaid
  Decision Trace writers. JSON remains the primary artifact; Markdown and
  Mermaid are derived.
```

The core package owns concepts such as decisions, expressions, outcomes,
source locations, evidence, unresolved items, metadata, and links. It must not
depend on Java source parsing, IntelliJ PSI, Kotlin PSI, TypeScript AST APIs, or
any language-specific node model.

The application package receives the CLI request, selects a
`LanguageDecisionAdapter`, gets a common `DecisionTrace`, and writes the
decision artifacts. The Java adapter is the first implementation. Future Kotlin
or JS/TS support should enter as new adapters without adding language-specific
concepts to `com.codeatlas.core.decision`.

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

## Phase 4.1.1 Scope

Phase 4.1.1 is an architecture-only refactor. It does not add new extraction
behavior. `analyze-decisions` still produces the same Decision Trace artifacts
for the Phase 4.1 Java `if` plus `throw` fixture, but the CLI now goes through
the application layer and the Java extractor lives under the Java adapter
package.

## Next Steps

- Add endpoint support for `analyze-decisions`.
- Add unresolved decision reporting for unsupported shapes.
- Add `EARLY_RETURN` support.
- Add limited `IF_ELSE_CONDITION` support.
- Link decisions to Flow Graph nodes when a flow exists.
- Add optional project-wide `index-decisions`.
- Keep AI interpretation reserved for Phase 5.
