# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.blockthrow.RegistrationGuard.validate`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.blockthrow.RegistrationGuard.validate:if-throw:1

Kind: `CONDITIONAL_THROW`

Category: `VALIDATION`

Source: `com.example.decisiontrace.blockthrow.RegistrationGuard.validate`

Condition:

```java
request.name() == null || request.name().isBlank()
```

Outcome:

```text
throws IllegalArgumentException("Name is required")
```

Location:

```text
src/main/java/com/example/decisiontrace/blockthrow/RegistrationGuard.java:5
```

## Unresolved

No unresolved decision items.
