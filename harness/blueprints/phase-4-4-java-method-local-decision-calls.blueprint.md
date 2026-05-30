# Phase 4.4 — Java Method-Local Decision Calls Blueprint

## Primary input status

This blueprint is the primary input and source of truth for this phase.

Derived artifacts such as handoff, validation and completion should be generated or updated from this blueprint by the harness runner and reviewed before execution.

## Phase

Phase 4.4 — Java Method-Local Decision Calls

## Context

Decision Trace currently extracts several direct Java decision shapes inside a target method body.

Current supported families include:

- block if + direct throw;
- early return;
- block if + allowed pre-statements + final direct throw;
- single-line if + direct throw;
- narrow if/else return/return;
- future mixed if/else branch shapes may exist depending on Phase 4.3.1.

The next useful semantic step is to detect simple method-local delegation of decisions without doing full cross-method analysis.

## Motivation

Real Java code often keeps decision logic in small private helper methods:

```java
public void create(UserRequest request) {
    validateName(request);
    save(request);
}

private void validateName(UserRequest request) {
    if (request.name() == null || request.name().isBlank()) {
        throw new IllegalArgumentException("Name is required");
    }
}
```

Full cross-method propagation is broad and should remain out of scope.

However, a narrow method-local helper inside the same class can be resolved conservatively and linked to the caller as decision evidence.

## Objective

Add conservative support for detecting and linking simple same-class private/helper method calls that contain supported Decision Trace shapes.

This phase should not become full call graph analysis.

## Goals

1. Detect direct method calls inside the entrypoint method that target methods in the same class.
2. Analyze only same-class methods with supported direct decision shapes.
3. Include linked decisions from helper methods in the entrypoint Decision Trace.
4. Preserve source location and evidence from the helper method.
5. Clearly mark that the decision source is a method-local helper.
6. Keep unresolved records for helper calls that are not safe to resolve.
7. Keep core Decision Trace language-agnostic.
8. Keep Java-specific logic in the Java adapter package.
9. Preserve exact artifact comparisons.
10. Avoid Flow Graph and Project Index changes.

## Non-goals

Do not implement:

- full call graph;
- interface implementation resolution;
- cross-class helper resolution;
- dependency injection resolution;
- polymorphic method resolution;
- recursive helper expansion;
- multi-level propagation beyond one safe local call;
- data flow analysis;
- argument binding;
- Optional/stream/lambda analysis;
- Kotlin or JS/TS support;
- AI interpretation;
- runtime analysis;
- Flow Graph or Project Index changes.

## Supported shape

Support only one-level same-class calls from the entrypoint method to a local method.

Example:

```java
public void create(UserRequest request) {
    validateName(request);
}

private void validateName(UserRequest request) {
    if (request.name() == null || request.name().isBlank()) {
        throw new IllegalArgumentException("Name is required");
    }
}
```

Accept only simple call statements:

```java
validateName(request);
```

Do not support:

```java
this.validator.validateName(request);
someService.validate(request);
validateName(transform(request));
if (validateName(request)) { ... }
return validateName(request);
```

unless already safely supported elsewhere.

## Required behavior

The generated Decision Trace for the entrypoint should include the decision found in the same-class helper method.

The decision should preserve:

- helper method source class;
- helper method name;
- source location;
- condition text;
- outcome;
- evidence snippet.

The artifact should make it clear enough that the decision originated from a local helper method, not directly from the entrypoint body.

## Unresolved behavior

If a same-class method call looks like a decision helper but cannot be safely resolved, emit unresolved rather than silently ignoring it.

Examples:

```java
validate(request);
```

where multiple overloads exist.

```java
validateName(transform(request));
```

where argument mapping would be required.

```java
other.validateName(request);
```

cross-object/cross-class call.

```java
validateName(request);
```

where `validateName` contains unsupported nested decisions.

## Suggested fixture

Add:

```text
examples/phase-4-decision-trace/08-method-local-decision-call
```

Suggested class:

```java
package com.example.decisiontrace.localcall;

public class UserRegistration {
    public void create(CreateUserRequest request) {
        validateName(request);
    }

    private void validateName(CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
    }
}
```

Suggested record:

```java
package com.example.decisiontrace.localcall;

public record CreateUserRequest(String name) {
}
```

Expected result:

- 1 decision;
- decision evidence points to helper method;
- entrypoint trace includes linked helper decision;
- 0 unresolved for supported simple case.

## Suggested implementation areas

Likely files/areas:

```text
src/main/java/com/codeatlas/adapter/java/source/decision/
src/main/java/com/codeatlas/application/decision/
src/main/java/com/codeatlas/output/decision/
src/test/java/com/codeatlas/
examples/phase-4-decision-trace/
```

Potential new class:

```text
JavaMethodLocalDecisionCallResolver
```

or equivalent.

## Implementation constraints

Keep expansion depth to one local call for this phase.

Avoid recursion.

Avoid overload ambiguity.

Avoid argument/data-flow binding.

Avoid cross-class resolution.

If any of those are required, emit unresolved or skip conservatively.

## Validation requirements

Required commands:

```bash
git status
git branch --show-current
./gradlew test
./gradlew build
git diff --check
```

Run analyze-decisions for fixtures 01 through latest existing fixture, then add fixture 08.

New fixture command example:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/08-method-local-decision-call --entrypoint com.example.decisiontrace.localcall.UserRegistration.create"
```

Required Flow Graph regression:

```bash
./gradlew run --args="analyze-flow --project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

## Completion criteria

This phase is complete only when:

1. A simple same-class helper method decision is included in the entrypoint Decision Trace.
2. The source evidence points to the helper method.
3. Unsupported local call variants do not produce false precision.
4. Existing fixtures continue passing.
5. New fixture 08 passes exact comparisons.
6. Core remains language-agnostic.
7. Java-specific logic remains in Java adapter/application layer as appropriate.
8. No Flow Graph or Project Index changes are made.
9. Report is generated under `harness/reports/runs/`.
10. Human Reviewer approves before push or merge.

## Runtime report path

The harness should derive this path from the phase id:

```text
harness/reports/runs/PHASE_4_4_JAVA_METHOD_LOCAL_DECISION_CALLS_REPORT.md
```

## Branch strategy

Use the current working branch as the base.

Suggested branch:

```text
phase-4-4-java-method-local-decision-calls
```

Do not branch from `master`, merge to `master`, or push automatically.

## Out of scope confirmation required in report

The report must confirm no full call graph, cross-class resolution, DI resolution, polymorphism, recursion, data-flow analysis, Optional/stream/lambda analysis, Kotlin, JS/TS, AI interpretation, runtime analysis, Flow Graph changes, Project Index changes, merge or push.

## Suggested next phase

Phase 4.5 — Java Optional Decision Shapes, or Harness 0.5 — Phase Status Transition Helper.
