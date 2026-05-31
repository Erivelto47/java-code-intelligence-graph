# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.ifreturn.ImportService.process`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.ifreturn.ImportService.process:if-return:1

Kind: `EARLY_RETURN`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.ifreturn.ImportService.process`

Condition:

```java
request.processed()
```

Outcome:

```text
RETURN false
```

Location:

```text
src/main/java/com/example/decisiontrace/ifreturn/ImportService.java:5
```

## Unresolved

No unresolved decision items.
