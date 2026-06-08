# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.switchstatement.StatusDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.switchstatement.StatusDecision.resolve:switch:1

Kind: `SWITCH_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.switchstatement.StatusDecision.resolve`

Condition:

```java
request.status()
```

Outcome:

```text
1. CASE ("PENDING") -> RETURN "queued"
2. CASE ("APPROVED", "ACTIVE") -> RETURN "open"
3. DEFAULT -> RETURN "closed"
```

Location:

```text
src/main/java/com/example/decisiontrace/switchstatement/StatusDecision.java:5
```

## Unresolved

No unresolved decision items.
