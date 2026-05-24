# Phase 2 - Spring Entrypoints

## Status

Status: STARTED / MVP DESIGN

## Goal

Phase 2 allows Code Atlas to discover Spring entrypoints from the analyzed
project. The initial MVP focuses on Spring MVC HTTP controllers and endpoints,
so a user can list available HTTP entrypoints before running the Phase 1 flow
analyzer on a concrete Java class/method entrypoint.

## Why

In Phase 1, the user must already know the exact Java method:

```bash
analyze-flow --project ./repo --entrypoint com.company.Foo.method
```

In Phase 2, the user should be able to discover available entrypoints:

```bash
list-entrypoints --project ./repo
```

The discovered `javaEntrypoint` can then be passed back to the Phase 1 analyzer:

```bash
analyze-flow --project ./repo --entrypoint com.company.Foo.method
```

## Scope

Initial scope:

- Spring MVC HTTP endpoints.
- `@RestController`.
- `@Controller`.
- Class-level `@RequestMapping`.
- Method-level `@RequestMapping`.
- Shortcut mappings:
  - `@GetMapping`
  - `@PostMapping`
  - `@PutMapping`
  - `@PatchMapping`
  - `@DeleteMapping`
- Generation of `entrypoints.json`.
- `list-entrypoints` CLI command.
- Real benchmark using `AuthController.register` from
  `aws-fintech-transfer-lab/onboarding`.

## Out of Scope

The initial MVP does not cover:

- Listeners.
- Scheduled jobs.
- Consumers.
- Workers.
- Spring Security flow.
- Runtime route resolution.
- Actuator.
- OpenAPI generation.
- Kotlin.
- Complete profile, qualifier, or conditional bean resolution.
- Advanced path matching.
- Complex controller inheritance.
- Custom composed annotations.
- Request parameters or request body analysis.
- Data flow.
- QA or harness generation.

## Entrypoint JSON Contract Draft

`entrypoints.json` is the primary deterministic artifact for entrypoint
discovery. The initial schema is:

```json
{
  "schemaVersion": "1.0",
  "project": "/path/to/project",
  "generatedAt": "1970-01-01T00:00:00Z",
  "entrypoints": [
    {
      "id": "http:POST:/auth/register -> com.example.AuthController.register",
      "kind": "HTTP_ENDPOINT",
      "httpMethod": "POST",
      "path": "/auth/register",
      "className": "com.example.AuthController",
      "methodName": "register",
      "javaEntrypoint": "com.example.AuthController.register",
      "annotations": {
        "classLevel": ["@RequestMapping(\"/auth\")"],
        "methodLevel": ["@PostMapping(\"/register\")"]
      },
      "sourceLocation": {
        "file": "src/main/java/com/example/AuthController.java",
        "line": 123
      }
    }
  ],
  "metadata": {
    "analyzer": "source-text-spring-entrypoint-discoverer",
    "deterministic": true,
    "phase": "phase-2-spring-entrypoints",
    "source": "source-text"
  }
}
```

`sourceLocation` is included when the source-text adapter can determine the
file and method declaration line.

For method-level `@RequestMapping` without an explicit `method` attribute, the
MVP writes `httpMethod: "ANY"`. This keeps the fact deterministic without
inferring runtime route constraints.

## CLI

Target command:

```bash
./gradlew run --args="list-entrypoints --project /path/to/project"
```

Default output:

```text
<project>/.code-atlas/entrypoints.json
```

The command also supports an explicit output directory:

```bash
./gradlew run --args="list-entrypoints --project /path/to/project --output /path/to/output"
```

With `--output`, the artifact is written to:

```text
<output>/entrypoints.json
```

The existing Phase 1 invocation remains supported:

```bash
./gradlew run --args="--project ./repo --entrypoint com.company.Foo.method"
```

The explicit subcommand form is also supported:

```bash
./gradlew run --args="analyze-flow --project ./repo --entrypoint com.company.Foo.method"
```

## Analyze Flow by Endpoint

In addition to listing Spring entrypoints, Phase 2 allows the flow analyzer to
run from a discovered HTTP endpoint. The endpoint is resolved deterministically
from the Spring MVC entrypoint index to its `javaEntrypoint`, and that Java
entrypoint is then passed to the existing Phase 1 flow analyzer.

Conceptual flow:

```text
HTTP endpoint -> entrypoint discovery -> javaEntrypoint -> FlowGraph
```

```bash
./gradlew run --args="analyze-flow --project ./repo --endpoint 'POST /auth/register'"
```

The endpoint form generates the same flow artifacts as the Phase 1 analyzer:

- `flow.json`
- `flow.md`
- `flow.mmd`
- `context-pack.md`
- `agent-handoff.md`
- `project-index.json` and `flows-index.md` when the default project output is
  used

The legacy Java entrypoint forms remain supported:

```bash
./gradlew run --args="analyze-flow --project ./repo --entrypoint com.company.Foo.method"
./gradlew run --args="--project ./repo --entrypoint com.company.Foo.method"
```

Endpoint resolution rules:

- The accepted format is `METHOD /path`, for example `POST /auth/register`.
- HTTP methods are normalized to uppercase before matching.
- Paths are normalized with a leading slash, duplicate slashes collapsed, and a
  trailing slash removed except for `/`.
- Matching is exact on normalized HTTP method plus normalized path.
- `ANY` only matches `ANY /path`.
- If no endpoint matches, the CLI returns a clear not-found error and prints
  available endpoints.
- If multiple endpoints match the same method and path, the CLI returns a clear
  ambiguous-endpoint error and prints the candidates.

Current limitations:

- No Spring runtime path matching is modeled.
- Path variables are not expanded or interpreted beyond the literal registered
  path, for example `/users/{id}`.
- There is no complex disambiguation beyond exact method and path matching.

## Acceptance Criteria

- `docs/phase-2-spring-entrypoints.md` exists.
- `list-entrypoints` works.
- `entrypoints.json` is generated.
- `AuthController.register` appears as an HTTP endpoint in the real benchmark.
- Generated `javaEntrypoint` values are compatible with the Phase 1
  `analyze-flow` command.
- Phase 1 tests and contracts keep passing.
- `./gradlew test` passes.
- `./gradlew build` passes.

## Initial Limitations

- Discovery is source-text based and conservative.
- Only directly declared controller annotations are considered.
- Custom composed annotations are not expanded.
- Complex inherited controller mappings are not resolved.
- Advanced Spring path matching is not modeled.
- `@RequestMapping` with no `method` is represented as `ANY`.
