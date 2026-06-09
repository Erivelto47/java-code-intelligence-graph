# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.streamallnone.EligibilityDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.streamallnone.EligibilityDecision.resolve:stream-match:1

Kind: `STREAM_MATCH_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.streamallnone.EligibilityDecision.resolve`

Condition:

```java
accounts.stream().allMatch(account -> account.active())
```

Outcome:

```text
ASSIGN accounts.stream().allMatch(account -> account.active())
```

Location:

```text
src/main/java/com/example/decisiontrace/streamallnone/EligibilityDecision.java:7
```

### decision:com.example.decisiontrace.streamallnone.EligibilityDecision.resolve:stream-match:2

Kind: `STREAM_MATCH_DECISION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.streamallnone.EligibilityDecision.resolve`

Condition:

```java
accounts.stream().noneMatch(account -> account.blocked())
```

Outcome:

```text
ASSIGN accounts.stream().noneMatch(account -> account.blocked())
```

Location:

```text
src/main/java/com/example/decisiontrace/streamallnone/EligibilityDecision.java:8
```

## Unresolved

No unresolved decision items.
