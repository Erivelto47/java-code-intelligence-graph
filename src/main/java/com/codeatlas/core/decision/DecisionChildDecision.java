package com.codeatlas.core.decision;

public record DecisionChildDecision(
        String decisionId,
        DecisionKind kind,
        String condition,
        int parentBranchOrder,
        String parentBranchKind
) {
}
