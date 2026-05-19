# Phase 1: Java Flow Graph MVP

## Goal

Create a deterministic Java Flow Graph from a Java class/method entrypoint.

The current implementation uses a small source-text analyzer. It establishes the CLI, output layout, graph model, serialization contract, and a conservative deterministic analysis path before introducing richer source-code adapters.

## CLI

```bash
./gradlew run --args="--project ./repo --entrypoint com.company.FooService.method"
```

Supported arguments:

- `--project <path>`: required project directory.
- `--entrypoint <qualified.class.method>`: required Java class/method entrypoint.
- `--output <path>`: optional output directory override.
- `--stub`: optional flag that uses `StubFlowAnalyzer` instead of the source-text analyzer.

When `--output` is omitted, artifacts are written inside the analyzed project:

```text
<projectPath>/.code-atlas/flows/<package-path>/<ClassName>/<methodName>/
```

For `--project examples/java-simple` and `--entrypoint com.company.FooService.processOrder`, the default output directory is:

```text
examples/java-simple/.code-atlas/flows/com/company/FooService/processOrder/
```

Run the real MVP against the included example:

```bash
./gradlew run --args="--project examples/java-simple --entrypoint com.company.FooService.processOrder"
```

Run the same analysis with an explicit output override:

```bash
./gradlew run --args="--project examples/java-simple --entrypoint com.company.FooService.processOrder --output build/code-atlas-output"
```

Run the stub pipeline:

```bash
./gradlew run --args="--project ./ --entrypoint com.company.FooService.method --stub"
```

## Outputs

- `flow.json`: primary deterministic graph artifact.
- `flow.md`: Markdown rendering derived from `flow.json` data.
- `flow.mmd`: Mermaid rendering derived from `flow.json` data.
- `context-pack.md`: deterministic context pack derived from `flow.json` data.

## In Scope

- Java 21 Gradle project setup.
- Core model independent from IntelliJ PSI.
- Source-text analyzer that reads `.java` files without PSI or external parser libraries.
- Stub analyzer that remains available behind `--stub`.
- CLI validation and deterministic file generation.
- Unit tests for the analyzers, JSON writer, CLI, and dependency boundary.

## Out of Scope

- QA generation.
- Kotlin support.
- Neo4j persistence.
- Runtime tracing.
- Advanced data flow.
- Automatic decomposition.
- IntelliJ PSI implementation.
- External parser integration such as JavaParser or Spoon.

## SourceTextFlowAnalyzer

`SourceTextFlowAnalyzer` lives in `com.codeatlas.adapter.source`. The core model and `FlowAnalyzer` contract do not depend on this adapter.

For an entrypoint such as `com.company.FooService.processOrder`, it:

- splits package, class, and method from the entrypoint;
- searches recursively for `FooService.java`;
- prefers a file declaring `package com.company`;
- finds the class and method body with simple textual scanning and brace matching;
- removes line comments, block comments, string literals, and char literals before scanning for calls;
- emits deterministic `CLASS`, `METHOD`, `DECLARES`, and `CALLS` facts.

Example source:

```java
package com.company;

public class FooService {
    public OrderDto processOrder(Order order) {
        validate(order);
        repository.save(order);
        paymentClient.charge(order);
        return mapper(order);
    }
}
```

Example graph facts:

```text
class:com.company.FooService
method:com.company.FooService.processOrder
method:com.company.FooService.validate
method:repository.save
method:paymentClient.charge
method:com.company.FooService.mapper
```

Known limitations:

- Detects direct calls such as `validate(order)`, `repository.save(order)`, `this.calculateTotal(order)`, `paymentClient.charge(order)`, and `mapper.toDto(order)`.
- Does not detect complex chained calls such as `a.b().c()`.
- Does not process lambdas, method references, constructors, reflection, polymorphic dispatch, overload resolution, inherited calls, or complex imports.
- Does not implement data flow, runtime tracing, QA generation, Kotlin, Neo4j, or microservice decomposition.
- Member-access calls are intentionally represented with inferred names such as `method:repository.save` when the receiver type is unknown.

## Determinism

Phase 1 uses stable node IDs, a fixed generation timestamp, and deterministic writer behavior. AI interpretations are not generated in this phase.
