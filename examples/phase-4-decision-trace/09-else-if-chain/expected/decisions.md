# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.elseif.AccessDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.elseif.AccessDecision.resolve:else-if-chain:1

Kind: `IF_ELSE_IF_CHAIN`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.elseif.AccessDecision.resolve`

Condition:

```java
request == null
```

Outcome:

```text
1. IF (request == null) -> throws IllegalArgumentException("Request is required")
2. ELSE_IF (request.blocked()) -> RETURN "blocked"
3. ELSE_IF (request.admin()) -> RETURN "admin"
4. ELSE -> RETURN "standard"
```

Location:

```text
src/main/java/com/example/decisiontrace/elseif/AccessDecision.java:5
```

## Unresolved

No unresolved decision items.
