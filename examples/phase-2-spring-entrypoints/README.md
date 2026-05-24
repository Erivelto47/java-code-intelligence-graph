# Phase 2 Spring Entrypoint Fixtures

## Objective

This folder contains small, versioned, controlled fixtures used to validate the
Phase 2 Spring Entrypoints MVPs.

The objective of Phase 2 is to discover Spring MVC HTTP endpoints from Java
source code, generate `entrypoints.json`, and resolve an HTTP endpoint to the
Phase 1 `javaEntrypoint` when running `analyze-flow --endpoint`.

## Relationship with Phase 2

- `entrypoints.json` is the primary deterministic artifact of Phase 2.
- Discovery must not depend on AI.
- The core must not depend on Spring, IntelliJ PSI, or adapters.
- The current adapter is source-text based.
- The discovered `javaEntrypoint` must be compatible with the Phase 1
  `analyze-flow` command.
- `list-endpoints` is the canonical listing command.
- `list-entrypoints` is retained for compatibility.

## Scope Covered by These Fixtures

- `@RestController`
- `@Controller` when applicable
- class-level `@RequestMapping`
- method-level `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- combination of class and method paths
- slash normalization
- `javaEntrypoint` generation
- `sourceLocation` generation
- multiple endpoints in the same controller

## Structure

Each example contains:

- `src/main/java/...`
- `README.md`
- `code-atlas.expected/expected-entrypoints.json`

## Examples

| Example | Entrypoint discovery | Validates | Expected result |
| --- | --- | --- | --- |
| `01-simple-rest-controller` | Discovers a POST endpoint from a REST controller with class and method mappings. | `@RestController`, `@RequestMapping("/auth")`, `@PostMapping("/register")` | `POST /auth/register -> com.example.spring.simple.AuthController.register` |
| `02-request-mapping-method` | Discovers a GET endpoint from method-level `@RequestMapping` with explicit `RequestMethod.GET`. | `@RestController`, `@RequestMapping("/users")`, `@RequestMapping(value = "/{id}", method = RequestMethod.GET)` | `GET /users/{id} -> com.example.spring.requestmapping.UserController.getById` |
| `03-multiple-http-methods` | Discovers multiple endpoints declared in the same controller through shortcut mappings. | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, multiple endpoints in the same controller | `GET /users -> com.example.spring.multiple.UserController.list`<br>`POST /users -> com.example.spring.multiple.UserController.create`<br>`PUT /users/{id} -> com.example.spring.multiple.UserController.update`<br>`DELETE /users/{id} -> com.example.spring.multiple.UserController.delete` |

## Manual Execution

```bash
./gradlew run --args="list-endpoints --project examples/phase-2-spring-entrypoints/01-simple-rest-controller"
```

Legacy compatibility command with explicit output:

```bash
./gradlew run --args="list-entrypoints --project examples/phase-2-spring-entrypoints/01-simple-rest-controller --output build/code-atlas-entrypoint-examples/01"
```

## Analyze Flow by Endpoint

The discovered Spring endpoint can also be used directly with the Phase 1 flow
analyzer:

```bash
./gradlew run --args="analyze-flow --project examples/phase-2-spring-entrypoints/01-simple-rest-controller --endpoint 'POST /auth/register'"
```

These fixtures are intentionally small. They validate endpoint discovery and
endpoint-to-`javaEntrypoint` resolution, but the generated flow is minimal
because the controller methods do not call richer application services.
