# Decision Trace

Scope: `com.example.decisiontrace.conditionalthrow.EmailRegistrationService.register`

Source:

- Flow: not available
- Project Index: not available

## Decisions

| ID | Kind | Category | Method | Location | Expression | Outcomes |
| --- | --- | --- | --- | --- | --- | --- |
| `decision:03-conditional-throw:register:email-exists` | CONDITIONAL_THROW | VALIDATION | `com.example.decisiontrace.conditionalthrow.EmailRegistrationService.register` | `src/main/java/com/example/decisiontrace/conditionalthrow/EmailRegistrationService.java:11` | `userRepository.existsByEmail(request.email())` | true -> THROW `EmailAlreadyExistsException`; false -> CONTINUE |

## Unresolved

No unresolved decision items.
