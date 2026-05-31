# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.unresolved.RegistrationGuard.validate`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

No decisions detected.

## Unresolved

| ID | Kind | Method | Location | Message | Expression |
| --- | --- | --- | --- | --- | --- |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:1 | UNSUPPORTED_INLINE_THROW | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:5 | Inline conditional throw is recognized but not a supported direct literal throw in Phase 4.3 | if (request.email() == null) throw createInlineException(request.email()); |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:2 | UNSUPPORTED_NESTED_IF | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:7 | Nested if decision shapes are recognized but not supported in Phase 4.3 | if (request.blocked()) { if (request.email().isBlank()) { throw new IllegalStateException("Blocked registration"); } throw new IllegalStateException("Blocked registration"); } |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:3 | UNSUPPORTED_THROW_EXPRESSION | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:14 | Conditional throw expression is recognized but not a direct supported throw | if (request.legacy()) { throw createLegacyException(request.email()); } |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:4 | UNSUPPORTED_INLINE_THROW | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:18 | Inline conditional throw is recognized but not a supported direct literal throw in Phase 4.3 | if (request.name() == null) throw new IllegalArgumentException(buildMessage()); |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:5 | UNSUPPORTED_INLINE_THROW | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:20 | Inline conditional throw is recognized but not a supported direct literal throw in Phase 4.3 | if (request.missingCode()) throw new IllegalStateException(); |
| unresolved:com.example.decisiontrace.unresolved.RegistrationGuard.validate:if:6 | UNSUPPORTED_IF_ELSE | com.example.decisiontrace.unresolved.RegistrationGuard.validate | src/main/java/com/example/decisiontrace/unresolved/RegistrationGuard.java:22 | if/else decision shape is recognized but outside the narrow Phase 4.3 return/return support | if (request.disabled()) throw new IllegalStateException("Disabled registration"); else allowDisabled(); |
