# 02 If Return Early Return

## Scenario

`ImportService.process` returns early when the input request was already
processed.

## Scope

```text
com.example.decisiontrace.ifreturn.ImportService.process
```

## Expected Decision

- `kind`: `EARLY_RETURN`
- `expression`: `request.processed()`
- `true` outcome: `RETURN false`
- `false` outcome: `CONTINUE`

## Acceptance Criteria

- The fixture emits one Decision Trace decision.
- The true branch outcome action is `RETURN`.
- No unresolved item is required for this simple direct form.
- No Flow Graph or Project Index artifact is required for this fixture.
