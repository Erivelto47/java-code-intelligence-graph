# Phase 1 Flow Graph Contract

This document defines the Phase 1 Java Flow Graph MVP contract for Code Atlas.

Phase 1 produces a deterministic graph for a Java entrypoint in the form
`<fully.qualified.ClassName>.<methodName>`. The graph describes source-level
facts found by the analyzer and conservative resolution decisions. It does not
describe runtime behavior.

## Primary Artifact

`flow.json` is the primary artifact. It is the source of truth for the generated
flow.

The other flow artifacts are derived from the same `FlowGraph` instance:

- `flow.md`
- `flow.mmd`
- `context-pack.md`
- `agent-handoff.md`

Markdown, Mermaid, context-pack, and handoff outputs may omit or reformat data
for readability, but they must not add graph facts that are absent from
`flow.json`.

AI may audit, explain, and suggest next steps from these artifacts. AI output is
not part of the deterministic graph contract and must not invent edges,
implementations, or runtime facts.

## FlowGraph Shape

The serialized `FlowGraph` uses schema version `1.0`:

```json
{
  "schemaVersion": "1.0",
  "entrypoint": "com.example.OrderController.create",
  "generatedAt": "1970-01-01T00:00:00Z",
  "nodes": [],
  "edges": [],
  "metadata": {
    "analyzer": "source-text-flow-analyzer",
    "phase": "phase-1-mvp",
    "deterministic": true
  },
  "unresolved": [],
  "boundaries": [],
  "resolutions": []
}
```

Conceptual fields:

- `entrypoint`: the requested class/method entrypoint.
- `nodes`: discovered symbols in deterministic order.
- `edges`: deterministic relationships between nodes.
- `resolutions`: conservative inferred dispatch decisions.
- `unresolved`: calls or symbols that could not be resolved safely.
- `boundaries`: calls intentionally stopped at a repository, framework, HTTP
  client, logging, or external symbol boundary.
- `metadata`: deterministic analyzer metadata.

## Node Kinds

Phase 1 node IDs use lower-case prefixes:

- `class:<qualifiedName>`
- `interface:<qualifiedName>`
- `method:<qualifiedType>.<methodName>`
- `boundary:<qualifiedType>.<methodName>`

The current JSON `kind` values are serialized in upper case:

- `CLASS`
- `INTERFACE`
- `METHOD`
- `BOUNDARY`

Conceptually these represent `class`, `interface`, `method`, and `boundary`.

## Edge Kinds

Phase 1 supports these edge kinds:

- `DECLARES`: a class or interface declares a method or boundary method.
- `CALLS`: a method body contains a deterministic call to another method or
  boundary.
- `IMPLEMENTS`: a concrete type declares that it implements an interface.
- `RESOLVES_TO`: an interface method dispatch was conservatively resolved to a
  concrete implementation.

Every edge should carry deterministic attributes such as `line`, `source`,
`evidence`, and `confidence` when available.

## Resolution

A `Resolution` records a conservative dispatch decision from an abstract or
interface method to a concrete implementation method.

Phase 1 may emit:

- `kind=INTERFACE_SINGLE_IMPLEMENTATION`
- `evidence=INFERRED`
- `confidence=HIGH`

This is allowed when the analyzer can determine that exactly one eligible local
implementation exists for the interface method. The `RESOLVES_TO` edge and the
`resolutions` entry are both derived from that deterministic source scan.

Phase 1 does not implement full Spring dispatch semantics. It does not fully
resolve `@Qualifier`, `@Profile`, `@Primary`, conditional beans, runtime
configuration, or dependency injection container state.

## UnresolvedSymbol

An `UnresolvedSymbol` records a symbol the analyzer found but cannot resolve
safely.

Common reasons include:

- `MULTIPLE_IMPLEMENTATIONS`: more than one eligible implementation exists.
- `NO_IMPLEMENTATION`: an interface call has no local implementation and is not
  classified as a boundary.
- `RECEIVER_TYPE_NOT_RESOLVED`: the receiver type is unknown.
- `METHOD_NOT_FOUND`: the receiver type is known but the method is not found.
- `MAX_DEPTH_REACHED`: traversal reached the Phase 1 safety limit.

For `MULTIPLE_IMPLEMENTATIONS`, `candidates` must list the possible concrete
targets. The analyzer must not choose one automatically when the choice is
ambiguous.

## BoundarySymbol

A `BoundarySymbol` records a deterministic stopping point. Boundaries are graph
facts, but they are not internal implementation edges.

Phase 1 boundary kinds include:

- `REPOSITORY`
- `HTTP_CLIENT`
- `FRAMEWORK`
- `EXTERNAL_CLIENT`
- `EXTERNAL_SYMBOL`

Repository interfaces without a local implementation are boundaries. Examples:

- `UserRepository.findByEmail`
- `UserRepository.save`

Framework, logging, HTTP client, and external symbols must not receive invented
implementations. They should be marked as `boundary` when they match a known
boundary category, or `unresolved` when the analyzer cannot classify them
safely.

Examples:

- `PasswordEncoder.encode` as framework/external boundary.
- `RestClient.post` as HTTP client boundary.
- `Logger.info` and `Logger.warn` as framework/logging boundaries.

## Interface Dispatch Rules

Interface with one eligible local implementation:

- add a factual `CALLS` edge to the interface method;
- add an `IMPLEMENTS` edge from the implementation type to the interface;
- add `RESOLVES_TO` from the interface method to the implementation method;
- add a `Resolution` with `evidence=INFERRED` and `confidence=HIGH`;
- continue traversal into the concrete implementation method.

Interface with multiple eligible local implementations:

- add the factual interface method call;
- add implementation candidates as facts when discovered;
- add `unresolved` with `reason=MULTIPLE_IMPLEMENTATIONS`;
- include the candidate method names;
- do not add a `RESOLVES_TO` edge to any candidate.

Interface repository without local implementation:

- classify the call as `REPOSITORY` boundary;
- do not invent an implementation.

Interface external client without local implementation:

- classify the call as `HTTP_CLIENT`, `EXTERNAL_CLIENT`, or
  `EXTERNAL_SYMBOL` boundary when the naming/import rules support it;
- otherwise use `unresolved`.

## Example

Conceptual example for a controller calling an interface with one
implementation:

```json
{
  "entrypoint": "com.example.RegistrationController.register",
  "nodes": [
    {
      "id": "method:com.example.RegistrationController.register",
      "kind": "METHOD"
    },
    {
      "id": "method:com.example.RegistrationUseCase.create",
      "kind": "METHOD"
    },
    {
      "id": "method:com.example.RegistrationService.create",
      "kind": "METHOD"
    },
    {
      "id": "boundary:com.example.UserRepository.save",
      "kind": "BOUNDARY"
    }
  ],
  "edges": [
    {
      "kind": "CALLS",
      "sourceNodeId": "method:com.example.RegistrationController.register",
      "targetNodeId": "method:com.example.RegistrationUseCase.create"
    },
    {
      "kind": "RESOLVES_TO",
      "sourceNodeId": "method:com.example.RegistrationUseCase.create",
      "targetNodeId": "method:com.example.RegistrationService.create",
      "attributes": {
        "evidence": "INFERRED",
        "confidence": "HIGH"
      }
    },
    {
      "kind": "CALLS",
      "sourceNodeId": "method:com.example.RegistrationService.create",
      "targetNodeId": "boundary:com.example.UserRepository.save"
    }
  ],
  "resolutions": [
    {
      "kind": "INTERFACE_SINGLE_IMPLEMENTATION",
      "evidence": "INFERRED",
      "confidence": "HIGH"
    }
  ],
  "boundaries": [
    {
      "symbol": "com.example.UserRepository.save",
      "kind": "REPOSITORY"
    }
  ],
  "unresolved": []
}
```

This example is intentionally small. The actual `flow.json` includes additional
deterministic fields such as display names, source paths, line numbers, and
attributes.

## Phase 1 Limitations

Phase 1 intentionally excludes:

- advanced data flow;
- runtime behavior;
- complete Spring `@Qualifier`, `@Profile`, `@Primary`, or conditional bean
  resolution;
- complete chained-call analysis;
- complete Lombok support;
- overload resolution;
- inherited method resolution;
- reflection;
- method references and lambda body traversal;
- Kotlin;
- QA generation;
- runtime tracing;
- Neo4j persistence;
- IntelliJ PSI integration in the core model.

The core model must remain independent from IntelliJ PSI. PSI can be introduced
later as an adapter, but not as a dependency of `com.codeatlas.core`.
