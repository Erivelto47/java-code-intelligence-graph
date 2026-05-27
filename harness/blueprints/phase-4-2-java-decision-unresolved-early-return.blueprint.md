# Phase 4.2 — Java Decision Unresolved + Early Return Blueprint

## Purpose

Phase 4.2 expands the Decision Trace layer for Java source analysis while
keeping Decision Trace separate from Flow Graph and Project Index concerns.

The phase prepares deterministic handling for recognized Java decision shapes
that are not yet safely supported, and adds conservative extraction for simple
`if` + `return` / early return decisions. It must not mix flow construction,
project indexing, runtime behavior or AI interpretation into the Decision Trace
contract.

## Current baseline

The implementation baseline already includes:

- Decision Trace contract.
- Java `if` + `throw` extractor.
- Java adapter package under `com.codeatlas.adapter.java.source.decision`.
- `application.decision` orchestration layer.
- Canonical `decisions.json` formatting.
- Fixture `examples/phase-4-decision-trace/01-if-throw-validation`.

## Goals

1. Add specific unresolved records for recognized Java decision shapes that are
   not yet supported.
2. Add conservative support for simple early return decisions.
3. Keep core Decision Trace language-agnostic.
4. Keep Java-specific logic in `com.codeatlas.adapter.java.source.decision`.
5. Preserve `decisions.json` compatibility.
6. Create small fixtures and focused tests.

## Non-goals

Phase 4.2 must not include:

- Full `if/else` support.
- Nested propagation.
- Cross-method propagation.
- `Optional` analysis.
- Stream analysis.
- Lambda analysis.
- Method reference analysis.
- Kotlin support.
- JS/TS support.
- AI interpretation.
- Runtime analysis.
- Advanced data flow.
- Flow Graph changes.
- Project Index changes.

## Target behavior: unresolved

Phase 4.2 should recognize decision-like Java shapes that are known but not yet
safe to extract as precise decisions. These shapes should produce deterministic
unresolved records in `decisions.json` instead of being silently ignored.

Examples:

```java
if (condition) throw new SomeException("message");
```

```java
if (condition) {
    log.warn("...");
    throw new SomeException("message");
}
```

```java
if (condition) {
    throw createException();
}
```

Unresolved output must not become an extracted decision with false precision.
The unresolved item should preserve enough source evidence for a reviewer or
future phase to understand which unsupported shape was recognized.

## Target behavior: early return

Phase 4.2 should detect simple direct early return decisions such as:

```java
if (condition) {
    return;
}
```

```java
if (condition) {
    return false;
}
```

```java
if (condition) {
    return null;
}
```

```java
if (condition) {
    return someSimpleExpression;
}
```

These cases should generate a Decision Trace decision with outcome action
`RETURN`.

## Conservative constraints

The implementation should accept only simple and direct forms.

Do not expand support to:

- Return expressions with complex chained calls that are hard to classify.
- Blocks with multiple statements before the return.
- `if/else`.
- Nested `if`.
- `switch`.
- `try/catch`.
- `Optional`, stream or lambda constructs.

If the implementation recognizes a potential decision but cannot support it
conservatively, it should emit a deterministic unresolved record instead of
guessing.

## Expected artifacts

Future Phase 4.2 execution should continue generating:

- `decisions.json`
- `decisions.md`
- `decisions.mmd`

under:

```text
.code-atlas/decisions/<package-path>/<ClassName>/<methodName>/
```

## Expected examples/fixtures

Suggested focused fixtures:

- `examples/phase-4-decision-trace/02-if-return-early-return`
- `examples/phase-4-decision-trace/03-unresolved-decision-shapes`

Equivalent names that stay consistent with the existing project fixture style
are acceptable, but each fixture should isolate one decision shape family.

## Testing strategy

Future Phase 4.2 should include tests for:

- Early return extraction.
- Unresolved decision shapes.
- CLI `analyze-decisions`.
- Raw exact fixture comparison for `decisions.json`.
- Raw exact fixture comparison for `decisions.md`.
- Raw exact fixture comparison for `decisions.mmd`.
- Regression that fixture `01-if-throw-validation` still passes.
- Regression that `analyze-flow` is not broken.

## Risks

- Unresolved output can become noisy if recognition is too broad.
- Early return can look like a business rule when it is only control flow.
- Regex/source-text parsing can become fragile if scope expands too far.
- Core Decision Trace must remain language-agnostic.

## Suggested implementation order

1. Model or confirm the existing unresolved structure.
2. Add a Java detector for unsupported decision shapes.
3. Add `JavaEarlyReturnDecisionExtractor`.
4. Integrate through `JavaSourceDecisionAdapter`.
5. Create fixtures.
6. Update writers only if necessary, without changing the contract
   unnecessarily.
7. Test and validate.
