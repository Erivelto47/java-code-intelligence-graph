# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.switchfallthrough.StatusDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.switchfallthrough.StatusDecision.resolve:switch:1

Kind: `SWITCH_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.switchfallthrough.StatusDecision.resolve`

Condition:

```java
request.status()
```

Outcome:

```text
1. CASE ("PENDING", "QUEUED") -> RETURN "waiting"
2. CASE ("DONE") -> RETURN "complete"
3. DEFAULT -> RETURN "unknown"
```

Location:

```text
src/main/java/com/example/decisiontrace/switchfallthrough/StatusDecision.java:5
```

## Unresolved

No unresolved decision items.
