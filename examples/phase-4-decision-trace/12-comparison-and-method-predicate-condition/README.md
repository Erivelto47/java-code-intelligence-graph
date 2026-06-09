# Phase 4.6 Fixture: Comparison And Method Predicate Condition

This fixture validates Java Decision Trace condition-expression extraction for
comparison operators and method-call predicates inside a composed condition.

## Command

```bash
./gradlew run --args="analyze-decisions --project examples/phase-4-decision-trace/12-comparison-and-method-predicate-condition --entrypoint com.example.decisiontrace.comparisonpredicate.EligibilityDecision.resolve"
```

## Expected

- one `EARLY_RETURN` decision;
- comparison leaves preserve operator, left text and right text;
- method predicate calls are represented as leaf expression nodes;
- negation wraps the method-call predicate operand;
- original condition text is preserved.
