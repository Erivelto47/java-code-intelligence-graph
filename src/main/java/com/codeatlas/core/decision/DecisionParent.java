package com.codeatlas.core.decision;

public record DecisionParent(
        String decisionId,
        int branchOrder,
        String branchKind
) {
}
