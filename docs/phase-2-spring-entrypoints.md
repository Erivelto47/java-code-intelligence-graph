# Phase 2 - Spring Entrypoints

## Status

Status: CLOSED FOR PHASE 2.1 + PHASE 2.2 MVP

Phase 2 Spring Entrypoints is the source-text based Spring MVC entrypoint layer
on top of the Phase 1 Java Flow Graph MVP.

- Phase 2.1: `analyze-flow --endpoint` resolves an HTTP endpoint to a Java
  entrypoint and runs the existing Phase 1 flow analyzer.
- Phase 2.2: `list-endpoints` lists discovered Spring MVC HTTP endpoints and
  writes `.code-atlas/entrypoints.json`.

No Phase 1 flow graph contract is changed by this phase.

## Goal

Allow Code Atlas users to discover Spring MVC HTTP entrypoints from Java source
and run flow analysis from either a Java method entrypoint or a Spring endpoint.

The canonical workflow is:

```bash
./gradlew run --args="list-endpoints --project ./repo"
./gradlew run --args="analyze-flow --project ./repo --endpoint 'POST /auth/register'"
```

The endpoint is resolved deterministically to a `javaEntrypoint`; the existing
Phase 1 analyzer then handles the flow graph generation.

```text
HTTP endpoint -> entrypoint discovery -> javaEntrypoint -> FlowGraph
```

## Commands

### Analyze Flow by Java Entrypoint

The Phase 1 command remains supported:

```bash
./gradlew run --args="analyze-flow --project ./repo --entrypoint com.company.FooService.method"
```

The legacy no-subcommand form also remains supported:

```bash
./gradlew run --args="--project ./repo --entrypoint com.company.FooService.method"
```

Both forms generate the Phase 1 flow artifacts:

- `flow.json`
- `flow.md`
- `flow.mmd`
- `context-pack.md`
- `agent-handoff.md`
- `project-index.json` and `flows-index.md` when default project output is used

### Analyze Flow by Spring Endpoint

Phase 2.1 adds endpoint-based analysis:

```bash
./gradlew run --args="analyze-flow --project ./repo --endpoint 'POST /auth/register'"
```

Resolution rules:

- Endpoint input format is `METHOD /path`.
- HTTP methods are normalized to uppercase before matching.
- Paths are normalized with a leading slash, duplicate slashes collapsed, and a
  trailing slash removed except for `/`.
- Matching is exact on normalized HTTP method plus normalized path.
- `ANY` only matches `ANY /path`.
- If no endpoint matches, the CLI exits with a not-found error and prints the
  available endpoints.
- If multiple endpoints match the same method and path, the CLI exits with an
  ambiguous-endpoint error and prints the candidates.

The endpoint form writes `.code-atlas/entrypoints.json` as part of resolution
and then writes the same flow artifacts as `analyze-flow --entrypoint`.

`--entrypoint` and `--endpoint` are mutually exclusive.

### List Spring Endpoints

Phase 2.2 adds the canonical endpoint listing command:

```bash
./gradlew run --args="list-endpoints --project ./repo"
```

Default output:

```text
<project>/.code-atlas/entrypoints.json
```

Console output is a readable list:

```text
Discovered endpoints: 1
POST /auth/register -> com.example.AuthController.register
```

If no endpoints are found, the command still writes a valid
`entrypoints.json` with an empty `entrypoints` array.

### Legacy Compatibility: list-entrypoints

`list-entrypoints` is retained for compatibility with earlier Phase 2 docs and
scripts:

```bash
./gradlew run --args="list-entrypoints --project ./repo"
```

It uses the same discovery engine and writes the same JSON contract. The
compatibility command also supports an explicit output directory:

```bash
./gradlew run --args="list-entrypoints --project ./repo --output build/code-atlas-entrypoints"
```

With `--output`, the artifact is written to:

```text
<output>/entrypoints.json
```

Current CLI behavior keeps `--output` on `list-entrypoints`; `list-endpoints`
uses the default project output path.

## Entrypoint JSON Contract

The formal contract for `.code-atlas/entrypoints.json` is documented in:

```text
docs/phase-2-entrypoints-contract.md
```

Summary:

- `schemaVersion` is `1.0`.
- `generatedAt` is deterministic in the current source-text implementation.
- `project` is the absolute normalized project path in CLI-generated output.
- `entrypoints` contains deterministic HTTP endpoint descriptors.
- `metadata.analyzer` is `source-text-spring-entrypoint-discoverer`.
- `metadata.phase` is `phase-2-spring-entrypoints`.
- `metadata.source` is `source-text`.
- `metadata.deterministic` is `true`.
- `metadata.requestMappingWithoutMethod` is `ANY`.

## Discovery Scope

Included in this MVP:

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
- Multiple string literal paths in supported mapping annotations.
- `RequestMethod.*` method attributes on `@RequestMapping`.
- Generation of `.code-atlas/entrypoints.json`.
- Endpoint-to-`javaEntrypoint` resolution for `analyze-flow --endpoint`.

## Out of Scope

This phase does not implement:

- Kotlin support.
- IntelliJ PSI integration.
- Complete Java AST parsing.
- Runtime Spring route resolution.
- Custom composed annotations.
- Flow analysis for all endpoints at once.
- Interactive endpoint selection.
- Neo4j integration.
- QA or harness generation.
- Advanced data flow.
- Automatic decomposition.
- Listeners, scheduled jobs, consumers, workers, or non-HTTP entrypoints.
- Spring Security flow.
- Actuator or OpenAPI generation.
- Complete profile, qualifier, or conditional bean resolution.
- Advanced Spring path matching.
- Complex controller inheritance.
- Request parameter or request body analysis.

## Examples

Fixtures live in:

```text
examples/phase-2-spring-entrypoints/
```

Validated fixtures:

- `01-simple-rest-controller`
- `02-request-mapping-method`
- `03-multiple-http-methods`

Manual validation commands:

```bash
./gradlew run --args="list-endpoints --project examples/phase-2-spring-entrypoints/01-simple-rest-controller"
./gradlew run --args="analyze-flow --project examples/phase-2-spring-entrypoints/01-simple-rest-controller --endpoint 'POST /auth/register'"
```

## Acceptance Status

- `list-endpoints --project <path>` works.
- `list-entrypoints --project <path>` remains available for compatibility.
- `analyze-flow --project <path> --entrypoint <qualified.class.method>` remains
  available.
- `analyze-flow --project <path> --endpoint 'METHOD /path'` works for exact
  normalized endpoint matches.
- `.code-atlas/entrypoints.json` is the deterministic endpoint discovery
  artifact.
- Generated `javaEntrypoint` values are compatible with the Phase 1 flow
  analyzer.
- Phase 1 contracts remain unchanged.

## Known Limitations

- Discovery is source-text based and conservative.
- Only directly declared controller annotations are considered.
- Custom composed annotations are not expanded.
- Complex inherited controller mappings are not resolved.
- Runtime Spring path matching is not modeled.
- Path variables are matched literally, for example `/users/{id}`.
- `@RequestMapping` without a method attribute is represented as `ANY`.
- Endpoint disambiguation is exact method plus exact normalized path only.
