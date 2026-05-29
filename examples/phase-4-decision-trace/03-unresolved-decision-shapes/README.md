# 03 Unresolved Decision Shapes

## Scenario

`RegistrationGuard.validate` contains recognized Java decision shapes that are
not safe to extract as precise decisions in Phase 4.2.

## Scope

```text
com.example.decisiontrace.unresolved.RegistrationGuard.validate
```

## Expected Unresolved Items

- Inline `if` + `throw` without braces.
- Block `if` with another statement before `throw`.
- Block `if` with a factory-created exception.

## Acceptance Criteria

- The fixture emits unresolved records instead of precise decisions.
- Each unresolved record preserves the source snippet.
- No Flow Graph or Project Index artifact is required for this fixture.
