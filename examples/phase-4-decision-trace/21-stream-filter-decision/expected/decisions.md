# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.streamfilter.ActiveUserDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.streamfilter.ActiveUserDecision.resolve:stream-filter:1

Kind: `STREAM_FILTER_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.streamfilter.ActiveUserDecision.resolve`

Condition:

```java
users.stream() .filter(user -> user.active())
```

Outcome:

```text
FILTER user.active()
```

Location:

```text
src/main/java/com/example/decisiontrace/streamfilter/ActiveUserDecision.java:8
```

## Unresolved

No unresolved decision items.
