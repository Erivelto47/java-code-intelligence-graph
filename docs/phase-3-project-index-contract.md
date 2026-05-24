# Phase 3 - Project Index Contract

## Objective

Phase 3 adds a deterministic Project Index for Java/Spring projects. The index is
generated from source text under `src/main/java` and does not depend on IntelliJ
PSI. JSON remains the primary output. Markdown is a derived human-readable view.

The index captures facts that can be extracted deterministically:

- Java types and methods.
- Interfaces and local concrete implementations.
- Spring beans.
- Spring HTTP entrypoints.
- Unresolved project index items, when the indexer can report them.

## Main Command

```bash
./gradlew run --args="index-project --project ./repo"
```

Example:

```bash
./gradlew run --args="index-project --project /Users/eriveltomuller/Documents/GitHub/aws-fintech-transfer-lab/onboarding"
```

## Generated Files

The command writes files inside the analyzed project:

```text
<project>/.code-atlas/project-index.json
<project>/.code-atlas/entrypoints.json
<project>/.code-atlas/flows-index.md
```

`project-index.md` is optional and is not required for the Phase 3 MVP.

## `project-index.json` Schema

Top-level required fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `schemaVersion` | string | yes | Project index schema version. Current value: `1.0`. |
| `generatedAt` | string | yes | ISO-8601 timestamp. The MVP uses deterministic `1970-01-01T00:00:00Z`. |
| `project` | object | yes | Project descriptor. |
| `language` | string | yes | Current value: `Java`. |
| `frameworks` | array | yes | Detected frameworks, for example `Spring`. Can be empty. |
| `sourceRoots` | array | yes | Indexed source roots. Current MVP indexes `src/main/java`. |
| `classes` | array | yes | Java `CLASS`, `ENUM`, and `RECORD` descriptors. |
| `interfaces` | array | yes | Java `INTERFACE` descriptors. |
| `implementations` | array | yes | Local implementation mapping for interfaces. |
| `springBeans` | array | yes | Detected Spring bean classes. |
| `controllers` | array | yes | Fully qualified controller class names. |
| `repositories` | array | yes | Fully qualified repository type names. |
| `clients` | array | yes | Fully qualified client type names. |
| `entrypoints` | array | yes | Spring HTTP entrypoints. |
| `unresolved` | array | yes | Deterministic unresolved items. Can be empty. |
| `metadata` | object | yes | Indexer metadata. |

`project` fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `root` | string | yes | Absolute normalized project root. |

Java type descriptor fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `fullyQualifiedName` | string | yes | Fully qualified Java type name. |
| `packageName` | string | yes | Declared package, or empty string. |
| `simpleName` | string | yes | Simple type name. |
| `kind` | string | yes | `CLASS`, `INTERFACE`, `ENUM`, or `RECORD`. |
| `sourceFile` | string | yes | Project-relative Java source file. |
| `annotations` | array | yes | Simple annotation names. |
| `methods` | array | yes | Method descriptors. |
| `sourceLocation` | object | yes | Project-relative file and line. |

Method descriptor fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `name` | string | yes | Method name. |
| `signature` | string | yes | Simple signature, for example `register(RegisterRequest)`. |
| `visibility` | string | yes | `PUBLIC`, `PROTECTED`, `PRIVATE`, or `PACKAGE_PRIVATE`. |
| `returnType` | string | yes | Simple return type text from source. |
| `annotations` | array | yes | Simple annotation names. |
| `sourceLocation` | object | yes | Project-relative file and line. |

Implementation descriptor fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `interface` | string | yes | Fully qualified local interface name. |
| `implementations` | array | yes | Fully qualified local concrete implementation names. |

Spring bean descriptor fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | string | yes | Deterministic bean id from decapitalized simple class name. |
| `kind` | string | yes | `COMPONENT`, `SERVICE`, `REPOSITORY`, `CONTROLLER`, `REST_CONTROLLER`, or `CONFIGURATION`. |
| `beanType` | string | yes | Fully qualified bean type. |
| `annotations` | array | yes | Simple annotation names. |
| `sourceFile` | string | yes | Project-relative Java source file. |
| `sourceLocation` | object | yes | Project-relative file and line. |

Optional fields:

- `metadata` can add analyzer details without changing the core schema.
- Future versions may add more deterministic entrypoint kinds.
- Future versions may add richer unresolved entries.

Example:

```json
{
  "schemaVersion": "1.0",
  "generatedAt": "1970-01-01T00:00:00Z",
  "project": {
    "root": "/repo/onboarding"
  },
  "language": "Java",
  "frameworks": ["Spring"],
  "sourceRoots": ["src/main/java"],
  "classes": [
    {
      "fullyQualifiedName": "com.study.onboarding.modules.auth.api.AuthController",
      "packageName": "com.study.onboarding.modules.auth.api",
      "simpleName": "AuthController",
      "kind": "CLASS",
      "sourceFile": "src/main/java/com/study/onboarding/modules/auth/api/AuthController.java",
      "annotations": ["RestController", "RequestMapping"],
      "methods": [
        {
          "name": "register",
          "signature": "register(RegisterRequest)",
          "visibility": "PUBLIC",
          "returnType": "ResponseEntity",
          "annotations": ["PostMapping"],
          "sourceLocation": {
            "file": "src/main/java/com/study/onboarding/modules/auth/api/AuthController.java",
            "line": 34
          }
        }
      ],
      "sourceLocation": {
        "file": "src/main/java/com/study/onboarding/modules/auth/api/AuthController.java",
        "line": 28
      }
    }
  ],
  "interfaces": [],
  "implementations": [
    {
      "interface": "com.study.onboarding.modules.auth.application.UserRegistrationInternal",
      "implementations": [
        "com.study.onboarding.modules.auth.application.UserServiceImpl"
      ]
    }
  ],
  "springBeans": [],
  "controllers": [
    "com.study.onboarding.modules.auth.api.AuthController"
  ],
  "repositories": [],
  "clients": [],
  "entrypoints": [],
  "unresolved": [],
  "metadata": {
    "analyzer": "source-text-project-indexer",
    "phase": "phase-3-project-index",
    "deterministic": true,
    "source": "source-text"
  }
}
```

## `entrypoints.json` Schema

Top-level required fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `schemaVersion` | string | yes | Current value: `1.0`. |
| `generatedAt` | string | yes | ISO-8601 timestamp. |
| `project` | string | yes | Absolute normalized project root. |
| `entrypoints` | array | yes | Deterministic entrypoint descriptors. Can be empty. |
| `metadata` | object | yes | Indexer metadata. |

Entrypoint descriptor fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | string | yes | Stable entrypoint id. |
| `kind` | string | yes | Current value: `HTTP_ENDPOINT`. |
| `httpMethod` | string | yes | HTTP method, or `ANY` for method-less `@RequestMapping`. |
| `path` | string | yes | Normalized HTTP path. |
| `javaEntrypoint` | string | yes | Fully qualified class plus method. |
| `controllerClass` | string | yes | Fully qualified controller class. |
| `className` | string | yes | Alias for `controllerClass`. |
| `methodName` | string | yes | Java method name. |
| `sourceFile` | string | yes | Project-relative Java source file. |
| `sourceLocation` | object | yes | Project-relative file and line. |
| `annotations` | array | yes | Simple annotation names. |

Example:

```json
{
  "id": "http:POST:/auth/register -> com.study.onboarding.modules.auth.api.AuthController.register",
  "kind": "HTTP_ENDPOINT",
  "httpMethod": "POST",
  "path": "/auth/register",
  "javaEntrypoint": "com.study.onboarding.modules.auth.api.AuthController.register",
  "controllerClass": "com.study.onboarding.modules.auth.api.AuthController",
  "className": "com.study.onboarding.modules.auth.api.AuthController",
  "methodName": "register",
  "sourceFile": "src/main/java/com/study/onboarding/modules/auth/api/AuthController.java",
  "sourceLocation": {
    "file": "src/main/java/com/study/onboarding/modules/auth/api/AuthController.java",
    "line": 34
  },
  "annotations": ["RestController", "RequestMapping", "PostMapping"]
}
```

## `flows-index.md` Format

`flows-index.md` is derived from the JSON entrypoints and lists HTTP entrypoints
for human navigation.

Expected structure:

```markdown
# Code Atlas Flows Index

## HTTP Endpoints

| Method | Path | Java Entrypoint | Source |
| --- | --- | --- | --- |
| POST | /auth/register | `com.study.onboarding.modules.auth.api.AuthController.register` | `src/main/java/.../AuthController.java` |
```

## `analyze-flow --endpoint` Compatibility

`analyze-flow --project ./repo --endpoint "POST /auth/register"` must continue
to work.

Resolution order:

1. Try `<project>/.code-atlas/entrypoints.json` when present.
2. If the file is missing, index in memory from source text and write
   `.code-atlas/entrypoints.json`.
3. Resolve the HTTP endpoint to `javaEntrypoint`.
4. Execute the existing `analyze-flow` implementation.

## Out Of Scope

- IntelliJ PSI dependency in core.
- Kotlin.
- QA or test-generation features.
- Neo4j output.
- Runtime analysis.
- Automatic microservice or boundary decomposition.
- Listener, job, worker, or consumer entrypoints.
- AI interpretation fields in deterministic JSON artifacts.
