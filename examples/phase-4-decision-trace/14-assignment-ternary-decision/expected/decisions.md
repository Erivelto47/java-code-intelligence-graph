# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.ternaryassignment.StatusDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.ternaryassignment.StatusDecision.resolve:ternary:1

Kind: `TERNARY_CONDITION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.ternaryassignment.StatusDecision.resolve`

Condition:

```java
request.active()
```

Outcome:

```text
1. WHEN_TRUE (true) -> ASSIGN "active"
2. WHEN_FALSE (false) -> ASSIGN "inactive"
```

Location:

```text
src/main/java/com/example/decisiontrace/ternaryassignment/StatusDecision.java:5
```

## Unresolved

No unresolved decision items.
