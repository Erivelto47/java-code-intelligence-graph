# Decision Trace

Scope: `com.example.decisiontrace.simplevalidation.UserRegistrationService.register`

Source:

- Flow: not available
- Project Index: not available

## Decisions

| ID | Kind | Category | Method | Location | Expression | Outcomes |
| --- | --- | --- | --- | --- | --- | --- |
| `decision:01-simple-validation:register:name-required` | IF_CONDITION | VALIDATION | `com.example.decisiontrace.simplevalidation.UserRegistrationService.register` | `src/main/java/com/example/decisiontrace/simplevalidation/UserRegistrationService.java:5` | `request.name() == null || request.name().isBlank()` | true -> THROW `InvalidUserNameException`; false -> CONTINUE |

## Unresolved

No unresolved decision items.
