# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.booleancondition.AccessPolicy.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.booleancondition.AccessPolicy.resolve:if-return:1

Kind: `EARLY_RETURN`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.booleancondition.AccessPolicy.resolve`

Condition:

```java
request.enabled() && (!request.locked() || request.admin())
```

Outcome:

```text
RETURN "allowed"
```

Location:

```text
src/main/java/com/example/decisiontrace/booleancondition/AccessPolicy.java:5
```

## Unresolved

No unresolved decision items.
