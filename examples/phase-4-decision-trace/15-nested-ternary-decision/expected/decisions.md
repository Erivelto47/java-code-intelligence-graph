# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.ternarynested.StatusDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.ternarynested.StatusDecision.resolve:ternary:1

Kind: `TERNARY_CONDITION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.ternarynested.StatusDecision.resolve`

Condition:

```java
request.active()
```

Outcome:

```text
1. WHEN_TRUE (true) -> RETURN (request.admin() ? "admin" : "active") | child decisions: decision:com.example.decisiontrace.ternarynested.StatusDecision.resolve:ternary:2
2. WHEN_FALSE (false) -> RETURN "inactive"
```

Location:

```text
src/main/java/com/example/decisiontrace/ternarynested/StatusDecision.java:5
```

### decision:com.example.decisiontrace.ternarynested.StatusDecision.resolve:ternary:2

Kind: `TERNARY_CONDITION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.ternarynested.StatusDecision.resolve`

Condition:

```java
request.admin()
```

Outcome:

```text
1. WHEN_TRUE (true) -> RETURN "admin"
2. WHEN_FALSE (false) -> RETURN "active"
```

Location:

```text
src/main/java/com/example/decisiontrace/ternarynested/StatusDecision.java:5
```

## Unresolved

No unresolved decision items.
