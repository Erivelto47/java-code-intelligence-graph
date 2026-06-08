# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.switchexpression.FeeDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.switchexpression.FeeDecision.resolve:switch-expression:1

Kind: `SWITCH_EXPRESSION_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.switchexpression.FeeDecision.resolve`

Condition:

```java
request.type()
```

Outcome:

```text
1. CASE (STANDARD) -> ASSIGN "standard"
2. CASE (PREMIUM) -> ASSIGN "premium"
3. DEFAULT -> ASSIGN "unknown"
```

Location:

```text
src/main/java/com/example/decisiontrace/switchexpression/FeeDecision.java:5
```

## Unresolved

No unresolved decision items.
