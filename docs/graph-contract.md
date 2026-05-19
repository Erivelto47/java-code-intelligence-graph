# Graph Contract

## Primary Artifact

`flow.json` is the primary artifact. Markdown, Mermaid, and context-pack outputs are derived from the same `FlowGraph` data.

## FlowGraph

```json
{
  "schemaVersion": "1.0",
  "entrypoint": "com.company.FooService.method",
  "generatedAt": "1970-01-01T00:00:00Z",
  "nodes": [],
  "edges": [],
  "metadata": {}
}
```

Fields:

- `schemaVersion`: graph schema version.
- `entrypoint`: requested entrypoint.
- `generatedAt`: deterministic timestamp for the current stub analyzer.
- `nodes`: ordered list of graph nodes.
- `edges`: ordered list of graph edges.
- `metadata`: deterministic analyzer metadata.

## GraphNode

```json
{
  "id": "method:com.company.FooService.method",
  "kind": "METHOD",
  "qualifiedName": "com.company.FooService.method",
  "displayName": "method",
  "attributes": {
    "entrypoint": true
  }
}
```

Fields:

- `id`: stable node ID.
- `kind`: deterministic node kind.
- `qualifiedName`: fully qualified Java symbol name when known.
- `displayName`: short display label.
- `attributes`: deterministic node attributes.

## GraphEdge

```json
{
  "id": "edge-id",
  "kind": "CALLS",
  "sourceNodeId": "source-node-id",
  "targetNodeId": "target-node-id",
  "attributes": {}
}
```

Fields:

- `id`: stable edge ID.
- `kind`: deterministic edge kind.
- `sourceNodeId`: source node ID.
- `targetNodeId`: target node ID.
- `attributes`: deterministic edge attributes.

## Phase 1 Rules

- Core packages must not import IntelliJ PSI.
- PSI belongs only in adapter code.
- AI interpretations must not be mixed into deterministic graph facts.
- The adapter PSI package is a placeholder until source-code analysis is introduced.
