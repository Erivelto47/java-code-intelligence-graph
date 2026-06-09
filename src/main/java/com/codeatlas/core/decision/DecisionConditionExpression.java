package com.codeatlas.core.decision;

import java.util.List;

public record DecisionConditionExpression(
        String kind,
        String text,
        String operator,
        String left,
        String right,
        DecisionConditionExpression operand,
        List<DecisionConditionExpression> operands
) {
    public DecisionConditionExpression {
        operands = operands == null ? null : List.copyOf(operands);
    }
}
