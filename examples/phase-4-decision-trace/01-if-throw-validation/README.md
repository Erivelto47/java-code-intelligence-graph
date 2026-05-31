# 01 If Throw Validation

## Scenario

`UserService.create` rejects a request when `request.name()` is null or blank by
throwing `IllegalArgumentException`.

## Scope

```text
com.example.UserService.create
```

## Manual Execution

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/01-if-throw-validation --entrypoint com.example.UserService.create"
```

Expected artifacts:

```text
.code-atlas/decisions/com/example/UserService/create/decisions.json
.code-atlas/decisions/com/example/UserService/create/decisions.md
.code-atlas/decisions/com/example/UserService/create/decisions.mmd
```

## Expected Decision

- `kind`: `CONDITIONAL_THROW`
- `category`: `VALIDATION`
- `expression`: `request.name() == null || request.name().isBlank()`
- `true` outcome: `THROW IllegalArgumentException`
- `message`: `Name is required`
- `false` outcome: `CONTINUE`

## Acceptance Criteria

- The extractor captures only the direct `if` + `throw new` shape.
- Decision artifacts are generated separately from flow artifacts.
- The fixture keeps JSON as the primary contract.
