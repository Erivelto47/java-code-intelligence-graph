# Phase 4.2.1 — Java Block Throw With Pre-Statements Blueprint

## Primary input status

This blueprint is the primary input and source of truth for this phase.

Derived artifacts such as handoff, validation and completion should be generated or updated from this blueprint by the harness runner and reviewed before execution.

## Phase

Phase 4.2.1 — Java Block Throw With Pre-Statements

## Context

Phase 4 introduced Decision Trace as a deterministic semantic layer separate from Flow Graph and Project Index.

Current baseline:

- Phase 4.0 defined the Decision Trace contract.
- Phase 4.1 added simple Java `if (...) { throw new SomeException("literal"); }` extraction.
- Phase 4.1.1 separated common core, application layer, Java adapter and output writers.
- Phase 4.1.2 stabilized canonical `decisions.json` formatting.
- Phase 4.2 added:
  - simple Java early return extraction;
  - deterministic unresolved records for recognized unsupported Java decision shapes;
  - exact fixture comparisons for `decisions.json`, `decisions.md` and `decisions.mmd`.

Harness baseline:

- Harness 0.3 introduced `harness/bin/run-phase.sh`.
- Future phases should start from a blueprint under `harness/blueprints/`.
- The runner derives handoff, validation, completion and report path.
- Runtime reports should be written under `harness/reports/runs/`.

## Motivation

Phase 4.2 intentionally added unresolved records for recognized but unsupported decision shapes.

The next safest product increment is to take one already-recognized unresolved family and promote it to a supported deterministic decision shape.

This avoids jumping directly into broader `if/else`, Optional, streams, lambdas or cross-method propagation.

The selected family is:

```java
if (condition) {
    preStatement();
    throw new SomeException("literal message");
}
```

Example:

```java
if (request.name() == null || request.name().isBlank()) {
    log.warn("Invalid name");
    throw new IllegalArgumentException("Name is required");
}
```

Today this shape should be unresolved because the block contains a statement before the `throw`.

In this phase, it should become a supported `CONDITIONAL_THROW` decision when the extractor can safely identify:

- a direct `if` condition;
- a block body;
- one final direct `throw new SomeException("literal message")`;
- zero or more allowed pre-statements before the final throw;
- no nested decision propagation.

## Objective

Add conservative Java Decision Trace support for direct `if` blocks that contain allowed pre-statements before a final direct `throw`.

The phase should reduce one known unresolved family without expanding the whole decision engine.

## Goals

1. Extract deterministic `CONDITIONAL_THROW` decisions for Java `if` blocks where the final supported statement is a direct throw.
2. Preserve the existing simple if+throw behavior from Phase 4.1.
3. Preserve the early return behavior from Phase 4.2.
4. Convert the relevant unresolved fixture shape into a supported decision, or add a new fixture if clearer.
5. Keep unsupported variants as unresolved.
6. Keep core Decision Trace language-agnostic.
7. Keep Java-specific parsing/extraction inside `com.codeatlas.adapter.java.source.decision`.
8. Preserve canonical raw exact comparisons for generated artifacts.
9. Avoid Flow Graph and Project Index changes.

## Non-goals

Do not implement:

- full `if/else` support;
- nested decision propagation;
- cross-method decision propagation;
- data flow analysis;
- semantic interpretation with AI;
- Optional analysis;
- stream analysis;
- lambda analysis;
- method reference analysis;
- try/catch analysis;
- switch analysis;
- Kotlin support;
- JS/TS support;
- runtime analysis;
- changes to Flow Graph;
- changes to Project Index;
- broad parser rewrite;
- model routing, token tracking or harness observability.

## Supported shape

The supported shape should be intentionally narrow.

Accept:

```java
if (condition) {
    allowedPreStatement();
    throw new SomeException("literal message");
}
```

Also accept more than one allowed pre-statement if they are safely recognized as non-branching and non-returning.

Examples of potentially allowed pre-statements:

```java
log.warn("...");
log.info("...");
metrics.increment("...");
audit.record("...");
```

However, the implementation should not require semantic understanding of logging/metrics/audit libraries.

A safer MVP rule may be:

- allow simple expression statements before final throw;
- do not extract if any pre-statement is itself a control-flow construct;
- do not extract if any pre-statement contains an obvious `return`, `throw`, nested `if`, `switch`, `try`, loop or lambda block.

The exact accepted subset should be documented and tested.

## Required final throw shape

The final throw must still be direct and simple:

```java
throw new SomeException("literal message");
```

Required extraction:

- condition text;
- exception type;
- literal message when available;
- source class/method;
- source location when available;
- evidence that includes the relevant source snippet.

## Unsupported shapes should remain unresolved

Keep unresolved for:

```java
if (condition) {
    throw createException();
}
```

```java
if (condition) {
    if (otherCondition) {
        throw new SomeException("nested");
    }
    throw new SomeException("literal");
}
```

```java
if (condition) {
    return;
    throw new SomeException("unreachable");
}
```

```java
if (condition) {
    try {
        audit();
    } catch (Exception e) {
        throw new SomeException("wrapped");
    }
    throw new SomeException("literal");
}
```

```java
if (condition) {
    doSomething(() -> {
        throw new SomeException("lambda");
    });
    throw new SomeException("literal");
}
```

If the extractor recognizes a possible decision but cannot safely extract it, it should emit an unresolved record rather than silently ignoring it.

## Expected decision kind and category

The decision kind should remain consistent with existing conventions.

Expected kind:

```text
CONDITIONAL_THROW
```

Category:

- Preserve existing deterministic category rules.
- Use `VALIDATION` only when the existing deterministic rules support that classification.
- Do not over-classify based on weak evidence.
- If uncertain, preserve existing category behavior.

## Expected unresolved behavior

The unresolved detector should be adjusted carefully.

A shape that is now supported should no longer appear as unresolved.

Shapes still outside the supported subset should continue to produce unresolved entries if they are recognized.

The implementation should avoid duplicate output where the same source shape becomes both a decision and an unresolved record.

## Expected artifact behavior

The generated artifacts should continue to be:

```text
decisions.json
decisions.md
decisions.mmd
```

Under:

```text
.code-atlas/decisions/<package-path>/<ClassName>/<methodName>/
```

`decisions.json` remains the primary artifact.

Markdown and Mermaid remain derived views.

All fixture comparisons should remain raw exact comparisons where already established.

## Suggested fixtures

Add a new fixture:

```text
examples/phase-4-decision-trace/04-if-throw-with-pre-statements
```

Suggested class:

```java
package com.example.decisiontrace.blockthrow;

public class RegistrationGuard {
    public void validate(CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            logInvalidName(request);
            throw new IllegalArgumentException("Name is required");
        }
    }

    private void logInvalidName(CreateUserRequest request) {
        // no-op fixture helper
    }
}
```

However, be careful: a method call before throw may imply side effects. For Phase 4.2.1, this is acceptable only if we treat it as pre-statement evidence and do not infer semantics from it.

Alternative simpler fixture:

```java
package com.example.decisiontrace.blockthrow;

public class RegistrationGuard {
    public void validate(CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            System.out.println("Invalid name");
            throw new IllegalArgumentException("Name is required");
        }
    }
}
```

Use the style that best matches existing fixture conventions.

Expected result:

- 1 decision;
- 0 unresolved for the supported shape;
- outcome kind `THROW`;
- exception type `IllegalArgumentException`;
- message `Name is required`;
- condition text preserved.

Also update or add unresolved fixture coverage to ensure unsupported block shapes still remain unresolved.

## Suggested implementation areas

Likely files/areas:

```text
src/main/java/com/codeatlas/adapter/java/source/decision/
src/main/java/com/codeatlas/output/decision/
src/test/java/com/codeatlas/
examples/phase-4-decision-trace/
```

Likely classes to inspect:

```text
JavaIfThrowDecisionExtractor
JavaUnsupportedDecisionShapeDetector
JavaSourceDecisionAdapter
JavaEarlyReturnDecisionExtractor
DecisionTraceJsonWriter
DecisionTraceMarkdownWriter
DecisionTraceMermaidWriter
DecisionTraceFixtureContractTest
```

Exact names may differ; inspect the repository before changing.

## Suggested implementation order

1. Run harness runner against this blueprint to derive operational artifacts.
2. Inspect current Java decision extraction flow.
3. Identify how Phase 4.2 recognizes unsupported block throw shapes.
4. Extend Java if+throw extraction to support final direct throw with safe pre-statements.
5. Ensure supported shapes do not also generate unresolved records.
6. Add fixture `04-if-throw-with-pre-statements`.
7. Add or update tests for:
   - extractor behavior;
   - unresolved behavior;
   - CLI output;
   - exact fixture artifact comparison.
8. Run existing fixtures 01, 02 and 03 to ensure no regression.
9. Run Flow Graph regression to ensure no unintended changes.
10. Generate report under `harness/reports/runs/`.

## Validation requirements

Required commands:

```bash
git status
git branch --show-current
./gradlew test
./gradlew build
git diff --check
```

Required CLI regressions:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/01-if-throw-validation --entrypoint com.example.UserService.create"
```

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/02-if-return-early-return --entrypoint com.example.decisiontrace.ifreturn.ImportService.process"
```

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/03-unresolved-decision-shapes --entrypoint com.example.decisiontrace.unresolved.RegistrationGuard.validate"
```

Add the new fixture command after selecting final class/entrypoint, for example:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/04-if-throw-with-pre-statements --entrypoint com.example.decisiontrace.blockthrow.RegistrationGuard.validate"
```

Required artifact checks:

- exact raw comparison for `decisions.json`;
- exact raw comparison for `decisions.md`;
- exact raw comparison for `decisions.mmd`.

Required Flow Graph regression:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

Generated Flow Graph outputs should not be included in the product diff unless intentionally changed, which is not expected.

## Completion criteria

This phase is complete only when:

1. A direct Java `if` block with allowed pre-statements before a final direct throw is extracted as a `CONDITIONAL_THROW` decision.
2. The extracted decision includes condition, throw outcome, exception type and literal message.
3. The supported shape is not also reported as unresolved.
4. Unsupported related shapes still produce deterministic unresolved records when recognized.
5. Existing fixtures 01, 02 and 03 continue to pass exact artifact comparisons.
6. New fixture 04 passes exact artifact comparisons.
7. Core Decision Trace remains language-agnostic.
8. Java-specific logic remains in the Java adapter package.
9. No Flow Graph or Project Index implementation changes are made.
10. `./gradlew test` passes.
11. `./gradlew build` passes.
12. `git diff --check` passes.
13. Runtime report is generated under `harness/reports/runs/`.
14. Human Reviewer approves before push or merge.

## Report path

Runtime report should be generated at:

```text
harness/reports/runs/PHASE_4_2_1_JAVA_BLOCK_THROW_WITH_PRE_STATEMENTS_REPORT.md
```

## Branch strategy

Do not branch from `master` during the current harness/Phase 4 consolidation cycle unless Human Reviewer explicitly says the work has been consolidated.

Use the current working branch as the base.

Do not merge to `master`.

Do not push automatically.

## Out of scope confirmation required in report

The final report must explicitly confirm that the phase did not add:

- full `if/else` support;
- nested propagation;
- cross-method propagation;
- Optional analysis;
- stream analysis;
- lambda analysis;
- method reference analysis;
- Kotlin support;
- JS/TS support;
- AI interpretation;
- runtime analysis;
- advanced data flow;
- Flow Graph changes;
- Project Index changes;
- merge to `master`;
- push.

## Known risks

1. Allowing pre-statements before throw can accidentally imply semantic understanding of side effects.
2. Source-text parsing can become fragile if too many Java block shapes are accepted.
3. Unresolved noise can grow if unsupported recognition becomes too broad.
4. Classification as `VALIDATION` must remain conservative and deterministic.
5. Duplicate decision/unresolved output must be avoided.

## Suggested next phase after this

After this phase, choose one:

1. Phase 4.2.2 — Deepen another unresolved family, such as single-line if throw.
2. Phase 4.3 — Narrow Java if/else decision shape.
3. Harness 0.4 — Enrich runner-derived artifacts from blueprint sections.

Prefer another small Decision Trace increment unless the runner becomes a bottleneck.
