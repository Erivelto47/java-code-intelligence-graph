# Phase 4.4 Method-Local Decision Call

This fixture validates a narrow same-class helper call resolved from the
entrypoint method.

## Entrypoint

`com.example.decisiontrace.localcall.UserRegistration.create`

## Expected Decision

The entrypoint calls `validateName(request)`. The trace includes the supported
`if (...) { throw ...; }` decision from `validateName` and preserves the helper
method source location and evidence.

## Acceptance Criteria

- one decision is emitted;
- no unresolved items are emitted;
- the decision source is the helper method;
- the evidence snippet points to the helper `if` statement;
- no cross-class, overload, recursive or data-flow resolution is required.
