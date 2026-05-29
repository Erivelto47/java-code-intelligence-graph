# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.unresolved.RegistrationGuard.validate`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

No decisions detected.

## Unresolved

| ID | Kind | Method | Location | Message | Expression |
| --- | --- | --- | --- | --- | --- |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:1 | UNSUPPORTED_INLINE_THROW | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:5 | Inline conditional throw without a block is recognized but not extracted in Phase 4.2.1 | if (request.email() == null) throw new IllegalArgumentException("Email is required"); |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:2 | UNSUPPORTED_NESTED_IF | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:7 | Nested if decision shapes are recognized but not supported in Phase 4.2.1 | if (request.blocked()) { if (request.email().isBlank()) { throw new IllegalStateException("Blocked registration"); } throw new IllegalStateException("Blocked registration"); } |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:3 | UNSUPPORTED_THROW_EXPRESSION | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:14 | Conditional throw expression is recognized but not a direct supported throw | if (request.legacy()) { throw createLegacyException(request.email()); } |
