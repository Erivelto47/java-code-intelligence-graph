# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.optionalifpresent.NotificationDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.optionalifpresent.NotificationDecision.resolve:optional:1

Kind: `OPTIONAL_BRANCH`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.optionalifpresent.NotificationDecision.resolve`

Condition:

```java
user.ifPresentOrElse( value -> notify(value), () -> auditMissing())
```

Outcome:

```text
1. PRESENT (present) -> CALL value -> notify(value)
2. EMPTY (empty) -> CALL () -> auditMissing()
```

Location:

```text
src/main/java/com/example/decisiontrace/optionalifpresent/NotificationDecision.java:7
```

## Unresolved

No unresolved decision items.
