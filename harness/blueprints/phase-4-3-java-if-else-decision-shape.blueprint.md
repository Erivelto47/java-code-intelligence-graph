# Phase 4.3 — Narrow Java If/Else Decision Shape Blueprint

## Primary input status

This blueprint is the primary input and source of truth for this phase.

Derived artifacts such as handoff, validation and completion should be generated or updated from this blueprint by the harness runner and reviewed before execution.

## Phase

Phase 4.3 — Narrow Java If/Else Decision Shape

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
- Phase 4.2.2 added support for direct single-line Java `if (...) throw new SomeException("literal message");`.

Harness baseline:

- Harness 0.3 introduced `harness/bin/run-phase.sh`.
- Harness 0.4 introduced `harness/bin/run-next-phase.sh`.
- Harness 0.4.1 moved generated Codex prompts to `harness/bin/build/prompts/`.
- Future phases should start from a blueprint under `harness/blueprints/`.
- `harness/phases/phase-index.tsv` marks the next phase.
- Runtime reports should be written under `harness/reports/runs/`.

## Motivation

The Decision Trace extractor now supports several narrow Java guard/validation shapes:

- block if + direct throw;
- block if + early return;
- block if + allowed pre-statements + final direct throw;
- single-line if + direct throw;
- unresolved records for recognized unsupported shapes.

The next useful increment is to add a conservative `if/else` decision shape.

However, `if/else` can quickly become complex because it may represent:

- validation;
- business branching;
- data transformation;
- control flow;
- alternative return values;
- nested decisions;
- mixed side effects.

This phase should therefore support only a narrow, deterministic shape.

## Objective

Add conservative Java Decision Trace support for simple direct `if/else` blocks where both branches produce direct, simple terminal outcomes.

The phase must avoid broad branch interpretation and must not introduce AI interpretation or data-flow analysis.

## Goals

1. Extract deterministic `IF_ELSE_CONDITION` decisions for a narrow Java `if/else` shape.
2. Support simple terminal branch outcomes only.
3. Preserve all existing Phase 4.1, 4.2, 4.2.1 and 4.2.2 behavior.
4. Preserve unresolved output for unsupported or unsafe `if/else` shapes.
5. Keep core Decision Trace language-agnostic.
6. Keep Java-specific extraction inside `com.codeatlas.adapter.java.source.decision`.
7. Preserve canonical raw exact comparisons for generated artifacts.
8. Avoid Flow Graph and Project Index changes.

## Non-goals

Do not implement:

- broad if/else interpretation;
- nested if/else propagation;
- cross-method propagation;
- data flow analysis;
- variable assignment branch analysis;
- Optional analysis;
- stream analysis;
- lambda analysis;
- method reference analysis;
- try/catch analysis;
- switch analysis;
- Kotlin support;
- JS/TS support;
- AI interpretation;
- runtime analysis;
- changes to Flow Graph;
- changes to Project Index;
- model routing, token tracking or harness observability.

## Supported shape

Support only direct block `if/else` where both branches are simple and terminal.

Primary supported family:

```java
if (condition) {
    return simpleValue;
} else {
    return otherSimpleValue;
}
```

Examples:

```java
if (request.enabled()) {
    return true;
} else {
    return false;
}
```

```java
if (status == null) {
    return "UNKNOWN";
} else {
    return status;
}
```

Optional supported family if it fits the existing model cleanly:

```java
if (condition) {
    throw new SomeException("literal message");
} else {
    return simpleValue;
}
```

But this mixed throw/return support should be added only if it does not complicate the extractor. Prefer a smaller first implementation over a broad one.

If mixed branches increase complexity, keep Phase 4.3 limited to `return`/`return` branches and emit unresolved for mixed `throw`/`return`.

## Branch outcome constraints

Supported outcomes should be simple and direct.

Accept simple returns such as:

```java
return true;
return false;
return null;
return "literal";
return variableName;
return request.value();
```

Be conservative with complex expressions.

Do not support:

```java
return service.call().map(...).orElse(...);
return new ComplexObject(...);
return computeSomething(arg1, arg2);
```

unless existing code already has a safe simple-expression policy.

If uncertain, mark as unresolved.

## Required extraction

For supported if/else decisions, extract:

- condition text;
- decision kind;
- source class;
- source method;
- source location when available;
- true branch outcome;
- false branch outcome;
- evidence/source snippet.

Expected decision kind:

```text
IF_ELSE_CONDITION
```

Expected outcome kind for return branches:

```text
RETURN
```

The model should represent both branches clearly without claiming business meaning.

Category should be conservative:

- use `CONTROL_FLOW` if available and appropriate;
- otherwise use existing safe default such as `UNKNOWN`;
- do not classify as `VALIDATION` unless deterministic existing rules clearly justify it.

## Unsupported shapes should remain unresolved

Keep unresolved for:

```java
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}
```

```java
if (condition) {
    return true;
}
```

This is already an early return shape and should not be treated as if/else.

```java
if (condition) {
    if (otherCondition) {
        return true;
    }
    return false;
} else {
    return true;
}
```

```java
if (condition) {
    return true;
} else if (otherCondition) {
    return false;
} else {
    return null;
}
```

```java
if (condition) {
    try {
        return service.call();
    } catch (Exception e) {
        return false;
    }
} else {
    return true;
}
```

```java
if (condition) {
    return service.call();
} else {
    throw createException();
}
```

If the detector recognizes a possible if/else decision but cannot safely extract it, emit an unresolved record rather than silently ignoring it.

## Expected unresolved behavior

A supported if/else return/return shape should not also generate unresolved output.

Unsupported if/else variants should produce unresolved records when recognized.

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

Markdown and Mermaid writers may need small updates to render if/else branch outcomes clearly.

## Suggested fixture

Add a new fixture:

```text
examples/phase-4-decision-trace/06-if-else-return-branches
```

Suggested class:

```java
package com.example.decisiontrace.ifelse;

public class FeatureToggleDecision {
    public boolean resolve(FeatureRequest request) {
        if (request.enabled()) {
            return true;
        } else {
            return false;
        }
    }
}
```

Suggested record:

```java
package com.example.decisiontrace.ifelse;

public record FeatureRequest(boolean enabled) {
}
```

Expected result:

- 1 decision;
- 0 unresolved for the supported shape;
- decision kind `IF_ELSE_CONDITION`;
- condition text `request.enabled()`;
- true branch outcome `return true`;
- false branch outcome `return false`;
- source evidence preserved.

Also update or add unresolved fixture coverage for unsupported if/else shapes.

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
JavaDecisionSourceSupport
JavaIfThrowDecisionExtractor
JavaEarlyReturnDecisionExtractor
JavaUnsupportedDecisionShapeDetector
JavaSourceDecisionAdapter
DecisionTraceMarkdownWriter
DecisionTraceMermaidWriter
DecisionTraceFixtureContractTest
```

Potential new class:

```text
JavaIfElseDecisionExtractor
```

Exact names may differ; inspect the repository before changing.

## Suggested implementation order

1. Run `./harness/bin/run-next-phase.sh` after marking this phase as `next` in `harness/phases/phase-index.tsv`.
2. Review generated prompt under `harness/bin/build/prompts/`.
3. Inspect current Java decision extraction flow.
4. Add a narrow `JavaIfElseDecisionExtractor` or equivalent.
5. Integrate it through `JavaSourceDecisionAdapter`.
6. Ensure early return and if/throw extractors keep precedence where appropriate.
7. Ensure supported if/else does not also generate unresolved.
8. Add fixture `06-if-else-return-branches`.
9. Add or update tests for:
   - extractor behavior;
   - unresolved behavior;
   - CLI output;
   - exact fixture artifact comparison.
10. Run existing fixtures 01 through 05 to ensure no regression.
11. Run Flow Graph regression to ensure no unintended changes.
12. Generate report under `harness/reports/runs/`.

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

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/05-single-line-if-throw --entrypoint com.example.decisiontrace.singleline.PaymentGuard.validate"
```

Add the new fixture command after selecting final class/entrypoint, for example:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/06-if-else-return-branches --entrypoint com.example.decisiontrace.ifelse.FeatureToggleDecision.resolve"
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

1. A narrow direct Java `if/else` return/return branch shape is extracted as an `IF_ELSE_CONDITION` decision.
2. The extracted decision includes condition, true branch outcome, false branch outcome and source evidence.
3. The supported shape is not also reported as unresolved.
4. Unsupported if/else variants still produce deterministic unresolved records when recognized.
5. Existing fixtures 01 through 05 continue to pass exact artifact comparisons.
6. New fixture 06 passes exact artifact comparisons.
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
harness/reports/runs/PHASE_4_3_JAVA_IF_ELSE_DECISION_SHAPE_REPORT.md
```

## Branch strategy

Do not branch from `master` during the current harness/Phase 4 consolidation cycle unless Human Reviewer explicitly says the work has been consolidated.

Use the current working branch as the base.

Suggested branch:

```text
phase-4-3-java-if-else-decision-shape
```

Do not merge to `master`.

Do not push automatically.

## Phase index update

Before running `run-next-phase.sh`, ensure `harness/phases/phase-index.tsv` marks:

- Phase 4.2.2 as `implemented`;
- Phase 4.3 as `next`.

Do not mark Phase 4.3 as `implemented` automatically.

## Out of scope confirmation required in report

The final report must explicitly confirm that the phase did not add:

- broad if/else support;
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

1. If/else can easily become too broad and semantic; keep this phase narrow.
2. Branch outcomes can imply business meaning; avoid over-classification.
3. Match precedence matters: early return and throw extractors should not conflict with if/else extraction.
4. Unresolved records can become noisy if recognition is too broad.
5. Markdown and Mermaid rendering may need adjustment to avoid confusing branch direction.

## Suggested next phase after this

After this phase, choose one:

1. Phase 4.3.1 — Mixed throw/return if/else branch shape.
2. Phase 4.4 — Method-local decision calls.
3. Harness 0.5 — Phase status transition helper.

Prefer a small follow-up to if/else only if the narrow return/return shape is stable.
