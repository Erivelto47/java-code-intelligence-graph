# Decision Trace

Schema version: `1.0`

Entrypoint: `com.example.decisiontrace.nestedif.RoutingDecision.resolve`

Generated at: `1970-01-01T00:00:00Z`

## Decisions

### decision:com.example.decisiontrace.nestedif.RoutingDecision.resolve:nested-if-else:1

Kind: `IF_ELSE_CONDITION`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.nestedif.RoutingDecision.resolve`

Condition:

```java
request.internal()
```

Outcome:

```text
1. IF (request.internal()) -> CONTINUE  | child decisions: decision:com.example.decisiontrace.nestedif.RoutingDecision.resolve:if-return:2
2. ELSE -> CONTINUE  | child decisions: decision:com.example.decisiontrace.nestedif.RoutingDecision.resolve:if-throw:3
```

Location:

```text
src/main/java/com/example/decisiontrace/nestedif/RoutingDecision.java:5
```

### decision:com.example.decisiontrace.nestedif.RoutingDecision.resolve:if-return:2

Kind: `EARLY_RETURN`

Category: `UNKNOWN`

Source: `com.example.decisiontrace.nestedif.RoutingDecision.resolve`

Condition:

```java
request.priority()
```

Outcome:

```text
RETURN "priority"
```

Location:

```text
src/main/java/com/example/decisiontrace/nestedif/RoutingDecision.java:6
```

### decision:com.example.decisiontrace.nestedif.RoutingDecision.resolve:if-throw:3

Kind: `CONDITIONAL_THROW`

Category: `VALIDATION`

Source: `com.example.decisiontrace.nestedif.RoutingDecision.resolve`

Condition:

```java
request.blocked()
```

Outcome:

```text
throws IllegalStateException("Route blocked")
```

Location:

```text
src/main/java/com/example/decisiontrace/nestedif/RoutingDecision.java:11
```

## Unresolved

No unresolved decision items.
