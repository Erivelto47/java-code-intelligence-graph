# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.singleline.PaymentGuard.validate`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.singleline.PaymentGuard.validate:if-throw:1

Kind: `CONDITIONAL_THROW`

Category: `VALIDATION`

Source: `com.example.decisiontrace.singleline.PaymentGuard.validate`

Condition:

```java
request.amount() == null
```

Outcome:

```text
throws IllegalArgumentException("Amount is required")
```

Location:

```text
src/main/java/com/example/decisiontrace/singleline/PaymentGuard.java:5
```

## Unresolved

No unresolved decision items.
