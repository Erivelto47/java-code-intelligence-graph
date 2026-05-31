# 02 Early Return

## Scenario

`ImportService.process` returns immediately when the input request has already
been processed.

## Scope

```text
com.example.decisiontrace.earlyreturn.ImportService.process
```

## Expected Decision

- `kind`: `EARLY_RETURN`
- `category`: `BUSINESS_RULE`
- `expression`: `request.processed()`
- `true` outcome: `RETURN ProcessingResult.alreadyProcessed`
- `false` outcome: `CONTINUE`

## Acceptance Criteria

- The decision captures the early return as the outcome of the true branch.
- The false branch continues to the normal accepted result.
- No Flow Graph or Project Index artifact is required for this fixture.
