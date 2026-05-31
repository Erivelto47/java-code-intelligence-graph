# 01 Simple Validation

## Scenario

`UserRegistrationService.register` rejects a request when `request.name()` is
null or blank.

## Scope

```text
com.example.decisiontrace.simplevalidation.UserRegistrationService.register
```

## Expected Decision

- `kind`: `IF_CONDITION`
- `category`: `VALIDATION`
- `expression`: `request.name() == null || request.name().isBlank()`
- `true` outcome: `THROW InvalidUserNameException`
- `false` outcome: `CONTINUE`

## Acceptance Criteria

- The decision is stored in `expected/decisions.json`.
- The source location points to the `if` statement.
- The fixture does not require Flow Graph or Project Index.
- Markdown and Mermaid files are derived from the JSON contract.
