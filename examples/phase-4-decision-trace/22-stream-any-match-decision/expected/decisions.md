# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.streamanymatch.InvalidItemDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.streamanymatch.InvalidItemDecision.resolve:if-return:1

Kind: `EARLY_RETURN`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.streamanymatch.InvalidItemDecision.resolve`

Condition:

```java
items.stream().anyMatch(item -> item.invalid())
```

Outcome:

```text
RETURN false
```

Location:

```text
src/main/java/com/example/decisiontrace/streamanymatch/InvalidItemDecision.java:7
```

### decision:com.example.decisiontrace.streamanymatch.InvalidItemDecision.resolve:stream-match:2

Kind: `STREAM_MATCH_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.streamanymatch.InvalidItemDecision.resolve`

Condition:

```java
items.stream().anyMatch(item -> item.invalid())
```

Outcome:

```text
UNKNOWN item.invalid()
```

Location:

```text
src/main/java/com/example/decisiontrace/streamanymatch/InvalidItemDecision.java:7
```

## Unresolved

No unresolved decision items.
