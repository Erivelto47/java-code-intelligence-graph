# Phase 2 Entrypoints Contract

## Artifact

Phase 2 writes the Spring MVC endpoint discovery artifact to:

```text
<project>/.code-atlas/entrypoints.json
```

The legacy `list-entrypoints --output <path>` compatibility command writes:

```text
<output>/entrypoints.json
```

This contract describes the JSON emitted by the current source-text Spring
entrypoint discoverer. It does not change the Phase 1 flow graph contract.

## Top-Level Object

```json
{
  "schemaVersion": "1.0",
  "generatedAt": "1970-01-01T00:00:00Z",
  "project": "/absolute/path/to/project",
  "entrypoints": [],
  "metadata": {}
}
```

Fields:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `schemaVersion` | string | yes | Contract version. Current value is `1.0`. |
| `generatedAt` | string | yes | ISO-8601 timestamp. The source-text implementation currently emits a deterministic timestamp. |
| `project` | string | yes | Absolute normalized project path in CLI output. Path separators are `/`. |
| `entrypoints` | array | yes | Deterministic entrypoint descriptors. Can be empty. |
| `metadata` | object | yes | Analyzer metadata. |

## Entrypoint Descriptor

```json
{
  "id": "http:POST:/auth/register -> com.example.AuthController.register",
  "kind": "HTTP_ENDPOINT",
  "httpMethod": "POST",
  "path": "/auth/register",
  "javaEntrypoint": "com.example.AuthController.register",
  "controllerClass": "com.example.AuthController",
  "className": "com.example.AuthController",
  "methodName": "register",
  "sourceFile": "src/main/java/com/example/AuthController.java",
  "sourceLocation": {
    "file": "src/main/java/com/example/AuthController.java",
    "line": 123
  },
  "annotations": {
    "classLevel": [
      "@RestController",
      "@RequestMapping(\"/auth\")"
    ],
    "methodLevel": [
      "@PostMapping(\"/register\")"
    ]
  }
}
```

Fields:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `id` | string | yes | Stable descriptor id in the form `http:<METHOD>:<path> -> <javaEntrypoint>`. |
| `kind` | string | yes | Current value is `HTTP_ENDPOINT`. |
| `httpMethod` | string | yes | HTTP method. Shortcut mappings emit their method; method-less `@RequestMapping` emits `ANY`. |
| `path` | string | yes | Normalized endpoint path with a leading slash. |
| `javaEntrypoint` | string | yes | Qualified Java class and method passed to Phase 1 flow analysis. |
| `controllerClass` | string | yes | Fully qualified controller class. Kept as endpoint-facing naming. |
| `className` | string | yes | Fully qualified controller class. Kept for compatibility with core descriptor naming. |
| `methodName` | string | yes | Java method name. |
| `sourceFile` | string | yes when known | Project-relative source file path. |
| `sourceLocation` | object | yes when known | Project-relative source file and 1-based method declaration line. |
| `annotations` | object | yes | Raw class-level and method-level annotation snippets used for discovery. |

The JSON writer omits `null` fields. In the current source-text implementation,
`sourceFile` and `sourceLocation` are present for discovered Java source
methods.

## Source Location

```json
{
  "file": "src/main/java/com/example/AuthController.java",
  "line": 123
}
```

Fields:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `file` | string | yes | Project-relative source file path with `/` separators. |
| `line` | number | yes | 1-based line number for the Java method declaration. |

## Annotations

```json
{
  "classLevel": [
    "@RestController",
    "@RequestMapping(\"/auth\")"
  ],
  "methodLevel": [
    "@PostMapping(\"/register\")"
  ]
}
```

Fields:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `classLevel` | array of strings | yes | Raw annotation snippets leading the controller declaration. |
| `methodLevel` | array of strings | yes | Raw annotation snippets leading the Java method declaration. |

## Metadata

```json
{
  "analyzer": "source-text-spring-entrypoint-discoverer",
  "deterministic": true,
  "indexedJavaSourceFiles": 1,
  "phase": "phase-2-spring-entrypoints",
  "requestMappingWithoutMethod": "ANY",
  "source": "source-text"
}
```

Fields:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `analyzer` | string | yes | Current value is `source-text-spring-entrypoint-discoverer`. |
| `deterministic` | boolean | yes | Current value is `true`. |
| `indexedJavaSourceFiles` | number | yes | Count of Java source files scanned under the project. |
| `phase` | string | yes | Current value is `phase-2-spring-entrypoints`. |
| `requestMappingWithoutMethod` | string | yes | Current value is `ANY`. |
| `source` | string | yes | Current value is `source-text`. |

## Path and Method Normalization

Discovery normalizes endpoint paths by joining class-level and method-level
mapping paths, adding a leading slash, collapsing duplicate slashes, and
removing a trailing slash except for `/`.

Endpoint analysis input applies the same matching shape:

- `post auth/register` resolves as `POST /auth/register`.
- `POST //auth/register/` resolves as `POST /auth/register`.
- Matching remains exact after normalization.
- `ANY` endpoint descriptors match only `ANY /path` input.

## Compatibility Notes

- `list-endpoints` is the canonical listing command.
- `list-entrypoints` is retained for compatibility and uses the same JSON
  contract.
- `analyze-flow --entrypoint` remains the Phase 1 Java method entrypoint form.
- `analyze-flow --endpoint` resolves through this artifact model, then runs the
  existing Phase 1 flow analyzer.
- This contract is limited to Spring MVC HTTP endpoints discovered from source
  text.
