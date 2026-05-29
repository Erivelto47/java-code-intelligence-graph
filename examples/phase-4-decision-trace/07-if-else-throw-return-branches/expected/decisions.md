# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.ifelsemixed.AccessDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.ifelsemixed.AccessDecision.resolve:if-else:1

Kind: `IF_ELSE_CONDITION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.ifelsemixed.AccessDecision.resolve`

Condition:

```java
!request.allowed()
```

Outcome:

```text
true -> throws IllegalStateException("Access denied")
false -> RETURN true
```

Location:

```text
src/main/java/com/example/decisiontrace/ifelsemixed/AccessDecision.java:5
```

## Unresolved

No unresolved decision items.
