# 03 Conditional Throw

## Scenario

`EmailRegistrationService.register` checks whether the submitted e-mail already
exists and throws when it does.

## Scope

```text
com.example.decisiontrace.conditionalthrow.EmailRegistrationService.register
```

## Expected Decision

- `kind`: `CONDITIONAL_THROW`
- `category`: `VALIDATION`
- `expression`: `userRepository.existsByEmail(request.email())`
- `true` outcome: `THROW EmailAlreadyExistsException`
- `false` outcome: `CONTINUE`

## Acceptance Criteria

- The decision is represented separately from any repository boundary that may
  exist in a Flow Graph.
- The repository method may be listed as a lightweight link.
- The fixture does not require Project Index to classify the decision.
