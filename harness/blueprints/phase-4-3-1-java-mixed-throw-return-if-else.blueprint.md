# Phase 4.3.1 — Java Mixed Throw/Return If/Else Blueprint

## Primary input status

This blueprint is the primary input and source of truth for this phase.

Derived artifacts such as handoff, validation and completion should be generated or updated from this blueprint by the harness runner and reviewed before execution.

## Phase

Phase 4.3.1 — Java Mixed Throw/Return If/Else

## Context

Current Decision Trace baseline:

- Phase 4.1 supports direct Java block if + throw.
- Phase 4.2 supports simple early return and deterministic unresolved records.
- Phase 4.2.1 supports block if + allowed pre-statements + final direct throw.
- Phase 4.2.2 supports single-line if + direct throw.
- Phase 4.3 supports narrow block if/else return/return extraction as `IF_ELSE_CONDITION`.

Harness baseline:

- Harness 0.4.2 should derive phase paths from blueprint ids and synchronize `harness/phases/phase-index.tsv`.
- This blueprint should be discoverable from its filename:
  `phase-4-3-1-java-mixed-throw-return-if-else.blueprint.md`.

## Motivation

Phase 4.3 intentionally kept if/else support narrow by focusing on return/return branches.

A common next branch shape is a guarded failure branch followed by a successful return branch:

```java
if (condition) {
    throw new SomeException("literal message");
} else {
    return simpleValue;
}
```

This phase should add only this narrow mixed branch family if it can be represented deterministically without implying broader business semantics.

## Objective

Add conservative Java Decision Trace support for direct block if/else where one branch is a direct literal throw and the other branch is a simple direct return.

## Goals

1. Extract a deterministic `IF_ELSE_CONDITION` decision for narrow mixed throw/return if/else blocks.
2. Preserve existing if/throw, single-line if/throw and early-return behavior.
3. Keep unsupported mixed branch variants as unresolved.
4. Keep Java-specific logic inside `com.codeatlas.adapter.java.source.decision`.
5. Keep core Decision Trace language-agnostic.
6. Preserve exact fixture comparisons for `decisions.json`, `decisions.md` and `decisions.mmd`.
7. Avoid Flow Graph and Project Index changes.

## Non-goals

Do not implement:

- broad if/else support;
- nested if/else propagation;
- cross-method propagation;
- data flow analysis;
- Optional, stream, lambda or method reference analysis;
- try/catch or switch analysis;
- Kotlin or JS/TS support;
- AI interpretation;
- runtime analysis;
- Flow Graph changes;
- Project Index changes.

## Supported shape

Primary supported shape:

```java
if (condition) {
    throw new SomeException("literal message");
} else {
    return simpleValue;
}
```

Also acceptable if symmetrical and simple:

```java
if (condition) {
    return simpleValue;
} else {
    throw new SomeException("literal message");
}
```

Only implement the symmetrical case if it does not complicate extraction. Prefer a smaller safe subset.

## Required extraction

Extract:

- condition text;
- decision kind `IF_ELSE_CONDITION`;
- branch outcome for throw branch;
- branch outcome for return branch;
- exception type;
- literal exception message;
- return value/expression text;
- source class/method/location when available;
- evidence/source snippet.

## Unsupported shapes should remain unresolved

Keep unresolved for:

```java
if (condition) {
    throw createException();
} else {
    return true;
}
```

```java
if (condition) {
    throw new SomeException(buildMessage());
} else {
    return true;
}
```

```java
if (condition) {
    log.warn("x");
    throw new SomeException("message");
} else {
    return true;
}
```

unless pre-statement support is explicitly and safely reused.

```java
if (condition) {
    throw new SomeException("message");
} else if (otherCondition) {
    return false;
} else {
    return true;
}
```

## Suggested fixture

Add:

```text
examples/phase-4-decision-trace/07-if-else-throw-return-branches
```

Suggested class:

```java
package com.example.decisiontrace.ifelsemixed;

public class AccessDecision {
    public boolean resolve(AccessRequest request) {
        if (!request.allowed()) {
            throw new IllegalStateException("Access denied");
        } else {
            return true;
        }
    }
}
```

Suggested record:

```java
package com.example.decisiontrace.ifelsemixed;

public record AccessRequest(boolean allowed) {
}
```

Expected result:

- 1 decision;
- 0 unresolved for supported shape;
- decision kind `IF_ELSE_CONDITION`;
- one branch outcome `THROW`;
- one branch outcome `RETURN`;
- exception type `IllegalStateException`;
- message `Access denied`;
- condition text preserved.

## Validation requirements

Required commands:

```bash
git status
git branch --show-current
./gradlew test
./gradlew build
git diff --check
```

Run analyze-decisions for fixtures 01 through 06, then add fixture 07.

New fixture command example:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/07-if-else-throw-return-branches --entrypoint com.example.decisiontrace.ifelsemixed.AccessDecision.resolve"
```

Required Flow Graph regression:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

## Completion criteria

This phase is complete only when:

1. Narrow mixed throw/return if/else shape is extracted deterministically.
2. Unsupported mixed variants remain unresolved.
3. Fixtures 01 through 06 continue passing.
4. New fixture 07 passes exact comparisons.
5. Core remains language-agnostic.
6. Java-specific logic remains in Java adapter package.
7. No Flow Graph or Project Index changes are made.
8. Report is generated under `harness/reports/runs/`.
9. Human Reviewer approves before push or merge.

## Runtime report path

The harness should derive this path from the phase id:

```text
harness/reports/runs/PHASE_4_3_1_JAVA_MIXED_THROW_RETURN_IF_ELSE_REPORT.md
```

## Branch strategy

Use the current working branch as the base.

Suggested branch:

```text
phase-4-3-1-java-mixed-throw-return-if-else
```

Do not branch from `master`, merge to `master`, or push automatically.

## Out of scope confirmation required in report

The report must confirm no broad if/else support, nested propagation, cross-method propagation, Optional/stream/lambda analysis, Kotlin, JS/TS, AI interpretation, runtime analysis, Flow Graph changes, Project Index changes, merge or push.

## Suggested next phase

Phase 4.4 — Java Method-Local Decision Calls.
