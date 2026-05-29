# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.ifelse.FeatureToggleDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.ifelse.FeatureToggleDecision.resolve:if-else:1

Kind: `IF_ELSE_CONDITION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.ifelse.FeatureToggleDecision.resolve`

Condition:

```java
request.enabled()
```

Outcome:

```text
true -> RETURN true
false -> RETURN false
```

Location:

```text
src/main/java/com/example/decisiontrace/ifelse/FeatureToggleDecision.java:5
```

## Unresolved

No unresolved decision items.
