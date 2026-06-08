# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.optionalfallback.DisplayNameDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.optionalfallback.DisplayNameDecision.resolve:optional:1

Kind: `OPTIONAL_BRANCH`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.optionalfallback.DisplayNameDecision.resolve`

Condition:

```java
Optional.ofNullable(profile.name()) .map(String::trim) .orElse("anonymous")
```

Outcome:

```text
1. PRESENT (present) -> ASSIGN Optional.ofNullable(profile.name()) .map(String::trim)
2. EMPTY (empty) -> ASSIGN "anonymous"
```

Location:

```text
src/main/java/com/example/decisiontrace/optionalfallback/DisplayNameDecision.java:9
```

## Unresolved

No unresolved decision items.
