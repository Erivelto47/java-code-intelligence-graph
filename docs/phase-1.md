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

When `--output` is omitted, the CLI writes the flow artifacts under:

```text
<projectPath>/.code-atlas/flows/<package-path>/<ClassName>/<methodName>/
```

It also writes project-level navigation files:

- `.code-atlas/project-index.json`: structured index for the analyzed project and current flow.
- `.code-atlas/flows-index.md`: human-readable index with direct paths to the generated flow artifacts.

The flow directory contains:

- `flow.json`: primary deterministic graph artifact.
- `flow.md`: Markdown rendering derived from `flow.json` data.
- `flow.mmd`: Mermaid rendering derived from `flow.json` data.
- `context-pack.md`: deterministic context pack derived from `flow.json` data.
- `agent-handoff.md`: operational handoff for agents that need exact artifact paths.

For the included fixture, the generated structure is:

```text
examples/java-simple/.code-atlas/
  project-index.json
  flows-index.md
  flows/com/company/FooService/processOrder/
    flow.json
    flow.md
    flow.mmd
    context-pack.md
    agent-handoff.md
```

`project-index.json` keeps JSON as the primary machine-readable navigation surface for generated flows. It includes the project root, current entrypoint, source files, flow path, and artifact paths. In this phase it may be overwritten with the current flow instead of accumulating multiple flows.

`flows-index.md` exists for humans and agents that can read a known file path but cannot traverse repository directories. It provides a compact table pointing to the flow directory, `context-pack.md`, and `flow.json`.

`agent-handoff.md` sits next to each flow and summarizes the repository, project path, entrypoint, source files, generated artifacts, graph counts, analyzer name, detected nodes, detected edges, and the command used to regenerate the flow. It may contain operational guidance for another agent, but it does not add AI interpretation.

When `--output <path>` is provided, the CLI keeps the current behavior of writing flow artifacts to that explicit directory. It also writes `agent-handoff.md` there. Project-level `.code-atlas/project-index.json` and `.code-atlas/flows-index.md` are not required in that mode.

## In Scope

- Java 21 Gradle project setup.
- Core model independent from IntelliJ PSI.
- Source-text analyzer that reads `.java` files without PSI or external parser libraries.
- Stub analyzer that remains available behind `--stub`.
- CLI validation and deterministic file generation, including agent navigation artifacts.
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
