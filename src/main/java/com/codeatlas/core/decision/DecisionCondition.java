package com.codeatlas.core.decision;

public record DecisionCondition(
        String text,
        String normalized,
        DecisionConditionExpression conditionExpression
) {
    public DecisionCondition(String text, String normalized) {
        this(text, normalized, null);
    }
}
