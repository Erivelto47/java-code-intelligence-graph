package com.codeatlas.core.decision;

public record DecisionSource(
        String className,
        String methodName,
        String signature
) {
}
