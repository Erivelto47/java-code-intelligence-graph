# Phase 4.2.2 — Java Single-Line If Throw Blueprint

## Primary input status

This blueprint is the primary input and source of truth for this phase.

Derived artifacts such as handoff, validation and completion should be generated or updated from this blueprint by the harness runner and reviewed before execution.

## Phase

Phase 4.2.2 — Java Single-Line If Throw

## Context

Phase 4 introduced Decision Trace as a deterministic semantic layer separate from Flow Graph and Project Index.

Current Decision Trace baseline:

- Phase 4.0 defined the Decision Trace contract.
- Phase 4.1 added simple Java block `if (...) { throw new SomeException("literal"); }` extraction.
- Phase 4.1.1 separated common core, application layer, Java adapter and output writers.
- Phase 4.1.2 stabilized canonical `decisions.json` formatting.
- Phase 4.2 added:
  - simple Java early return extraction;
  - deterministic unresolved records for recognized unsupported Java decision shapes;
  - exact fixture comparisons for `decisions.json`, `decisions.md` and `decisions.mmd`.
- Phase 4.2.1 added support for Java block `if` decisions with allowed pre-statements before a final direct throw.

Harness baseline:

- Harness 0.3 introduced `harness/bin/run-phase.sh`.
- Future phases should start from a blueprint under `harness/blueprints/`.
- The runner derives handoff, validation, completion and report path.
- Runtime reports should be written under `harness/reports/runs/`.

## Motivation

Phase 4.2 introduced unresolved records for recognized but unsupported Java decision shapes.

After Phase 4.2.1, one safe unresolved family was promoted to supported deterministic extraction.

The next small and valuable increment is to support single-line Java `if` statements that directly throw an exception:

```java
if (condition) throw new SomeException("literal message");
```

This shape is common in validation/guard code and should map cleanly to the existing `CONDITIONAL_THROW` decision model.

The goal is to support this narrow syntax form without expanding into full parser complexity, `if/else`, nested propagation, Optional, streams or data flow.

## Objective

Add conservative Java Decision Trace support for direct single-line `if` statements that throw a direct `new SomeException("literal message")`.

## Goals

1. Extract deterministic `CONDITIONAL_THROW` decisions for Java single-line `if` throw statements.
2. Preserve existing block if+throw extraction from Phase 4.1.
3. Preserve block if+throw-with-pre-statements extraction from Phase 4.2.1.
4. Preserve early return behavior from Phase 4.2.
5. Ensure supported single-line if throw shapes no longer appear as unresolved.
6. Keep unsupported single-line variants as unresolved when recognized.
7. Keep core Decision Trace language-agnostic.
8. Keep Java-specific parsing/extraction inside `com.codeatlas.adapter.java.source.decision`.
9. Preserve canonical raw exact comparisons for generated artifacts.
10. Avoid Flow Graph and Project Index changes.

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

Accept this exact family:

```java
if (condition) throw new SomeException("literal message");
```

Examples:

```java
if (request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Name is required");
```

```java
if (!enabled) throw new IllegalStateException("Import is disabled");
```

The implementation should support:

- a direct `if` keyword;
- a condition in parentheses;
- a direct `throw new SomeException("literal message");`;
- no `else`;
- no block body;
- no nested control flow;
- no factory-created exception;
- literal message extraction when available.

## Required extraction

For supported single-line if throw statements, extract:

- condition text;
- decision kind;
- category using existing deterministic rules;
- source class;
- source method;
- source location when available;
- exception type;
- literal exception message when available;
- evidence/source snippet.

Expected decision kind:

```text
CONDITIONAL_THROW
```

Expected outcome kind:

```text
THROW
```

## Unsupported shapes should remain unresolved

Keep unresolved for:

```java
if (condition) throw createException();
```

```java
if (condition) throw new SomeException(buildMessage());
```

```java
if (condition) throw new SomeException();
```

```java
if (condition) throw new SomeException("message"); else doSomething();
```

```java
if (condition) return;
```

The existing early return extractor should handle supported early return shapes separately, not through this phase.

If the detector recognizes a possible decision but cannot safely extract it, it should emit an unresolved record rather than silently ignoring it.

## Expected unresolved behavior

A supported single-line direct if throw should no longer generate unresolved output.

Unsupported related single-line throw shapes should continue to produce unresolved entries when recognized.

Avoid duplicate output where the same source shape becomes both a decision and an unresolved record.

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

## Suggested fixture

Add a new fixture:

```text
examples/phase-4-decision-trace/05-single-line-if-throw
```

Suggested class:

```java
package com.example.decisiontrace.singleline;

public class PaymentGuard {
    public void validate(PaymentRequest request) {
        if (request.amount() == null) throw new IllegalArgumentException("Amount is required");
    }
}
```

Suggested request record:

```java
package com.example.decisiontrace.singleline;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
```

Expected result:

- 1 decision;
- 0 unresolved for the supported shape;
- decision kind `CONDITIONAL_THROW`;
- outcome kind `THROW`;
- exception type `IllegalArgumentException`;
- message `Amount is required`;
- condition text preserved.

Also update or add unresolved fixture coverage to ensure unsupported single-line throw variants still remain unresolved.

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
JavaDecisionSourceSupport
JavaSourceDecisionAdapter
JavaEarlyReturnDecisionExtractor
DecisionTraceFixtureContractTest
```

Exact names may differ; inspect the repository before changing.

## Suggested implementation order

1. Run harness runner against this blueprint to derive operational artifacts.
2. Inspect current Java decision extraction flow.
3. Identify how Phase 4.2 currently recognizes single-line if throw as unresolved.
4. Extend Java if+throw extraction to support direct single-line throw.
5. Ensure supported single-line if throw does not also generate unresolved.
6. Add fixture `05-single-line-if-throw`.
7. Add or update tests for:
   - extractor behavior;
   - unresolved behavior;
   - CLI output;
   - exact fixture artifact comparison.
8. Run existing fixtures 01, 02, 03 and 04 to ensure no regression.
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

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/04-if-throw-with-pre-statements --entrypoint com.example.decisiontrace.blockthrow.RegistrationGuard.validate"
```

Add the new fixture command after selecting final class/entrypoint, for example:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/05-single-line-if-throw --entrypoint com.example.decisiontrace.singleline.PaymentGuard.validate"
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

1. A direct Java single-line `if (condition) throw new SomeException("literal message");` is extracted as a `CONDITIONAL_THROW` decision.
2. The extracted decision includes condition, throw outcome, exception type and literal message.
3. The supported shape is not also reported as unresolved.
4. Unsupported related single-line throw shapes still produce deterministic unresolved records when recognized.
5. Existing fixtures 01, 02, 03 and 04 continue to pass exact artifact comparisons.
6. New fixture 05 passes exact artifact comparisons.
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
harness/reports/runs/PHASE_4_2_2_JAVA_SINGLE_LINE_IF_THROW_REPORT.md
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

1. Single-line source parsing can conflict with simple block parsing if match order is not explicit.
2. Unsupported single-line throw variants can create noisy unresolved records if recognition is too broad.
3. Classification as `VALIDATION` must remain conservative and deterministic.
4. Duplicate decision/unresolved output must be avoided.
5. Formatting differences in generated artifacts should not destabilize fixture comparisons.

## Suggested next phase after this

After this phase, choose one:

1. Phase 4.3 — Narrow Java if/else decision shape.
2. Phase 4.2.3 — Deepen another unresolved family, such as factory-created exceptions.
3. Harness 0.4 — Enrich runner-derived artifacts from blueprint sections.

Prefer Phase 4.3 only if the single-line and block throw families feel stable.
