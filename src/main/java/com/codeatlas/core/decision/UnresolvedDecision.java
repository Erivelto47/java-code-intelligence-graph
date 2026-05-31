package com.codeatlas.core.decision;

public record UnresolvedDecision(
        String id,
        String kind,
        String method,
        DecisionSourceLocation sourceLocation,
        String message,
        String expression
) {
}
