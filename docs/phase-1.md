# Phase 1: Java Flow Graph MVP

## Goal

Create a deterministic Java Flow Graph from a Java class/method entrypoint.

The current implementation is intentionally a stub. It establishes the CLI, output layout, graph model, and serialization contract before introducing source-code analysis.

## CLI

```bash
./gradlew run --args="--project ./repo --entrypoint com.company.FooService.method"
```

Supported arguments:

- `--project <path>`: required project directory.
- `--entrypoint <qualified.class.method>`: required Java class/method entrypoint.
- `--output <path>`: optional output directory. Defaults to `./build/code-atlas-output`.

## Outputs

- `flow.json`: primary deterministic graph artifact.
- `flow.md`: Markdown rendering derived from `flow.json` data.
- `flow.mmd`: Mermaid rendering derived from `flow.json` data.
- `context-pack.md`: deterministic context pack derived from `flow.json` data.

## In Scope

- Java 21 Gradle project setup.
- Core model independent from IntelliJ PSI.
- Stub analyzer that creates a graph with the entrypoint node.
- CLI validation and deterministic file generation.
- Unit tests for the analyzer, JSON writer, and CLI.

## Out of Scope

- QA generation.
- Kotlin support.
- Neo4j persistence.
- Runtime tracing.
- Advanced data flow.
- Automatic decomposition.
- IntelliJ PSI implementation.

## Determinism

Phase 1 uses stable node IDs, a fixed generation timestamp, and deterministic writer behavior. AI interpretations are not generated in this phase.
