# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.comparisonpredicate.EligibilityDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.comparisonpredicate.EligibilityDecision.resolve:if-return:1

Kind: `EARLY_RETURN`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.comparisonpredicate.EligibilityDecision.resolve`

Condition:

```java
request.age() >= 18 && request.name() != null && !request.name().isBlank() && request.email().contains("@")
```

Outcome:

```text
RETURN "eligible"
```

Location:

```text
src/main/java/com/example/decisiontrace/comparisonpredicate/EligibilityDecision.java:5
```

## Unresolved

No unresolved decision items.
