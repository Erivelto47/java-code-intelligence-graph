# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.optionalthrow.UserDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.optionalthrow.UserDecision.resolve:optional:1

Kind: `OPTIONAL_BRANCH`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.optionalthrow.UserDecision.resolve`

Condition:

```java
repository.findById(id) .filter(user -> user.active()) .orElseThrow(() -> new NotFoundException("User not found"))
```

Outcome:

```text
1. PRESENT (present) -> RETURN repository.findById
2. EMPTY (empty) -> throws NotFoundException("User not found")
```

Location:

```text
src/main/java/com/example/decisiontrace/optionalthrow/UserDecision.java:7
```

## Unresolved

No unresolved decision items.
