# 04 If Throw With Pre Statements

## Scenario

`RegistrationGuard.validate` records a simple pre-statement before throwing for
an invalid request name.

## Scope

```text
com.example.decisiontrace.blockthrow.RegistrationGuard.validate
```

## Expected Decision

- `kind`: `CONDITIONAL_THROW`
- `expression`: `request.name() == null || request.name().isBlank()`
- `true` outcome: `THROW IllegalArgumentException`
- `false` outcome: `CONTINUE`

## Acceptance Criteria

- The fixture emits one Decision Trace decision.
- The true branch outcome includes exception type `IllegalArgumentException`.
- The true branch outcome includes message `Name is required`.
- The supported block shape does not emit an unresolved record.
- Phase 4.2.1 only treats simple method-call expression statements as allowed
  pre-statements before the final direct throw.
- No Flow Graph or Project Index artifact is required for this fixture.
