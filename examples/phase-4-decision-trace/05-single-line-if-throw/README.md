# Phase 4.2.2 Single-Line If Throw Fixture

This fixture validates Java Decision Trace extraction for a direct single-line
conditional throw:

```java
if (request.amount() == null) throw new IllegalArgumentException("Amount is required");
```

Run:

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/05-single-line-if-throw --entrypoint com.example.decisiontrace.singleline.PaymentGuard.validate"
```

Expected output:

- one `CONDITIONAL_THROW` decision;
- no unresolved decision items;
- a `THROW` outcome for `IllegalArgumentException`;
- literal message `Amount is required`.
