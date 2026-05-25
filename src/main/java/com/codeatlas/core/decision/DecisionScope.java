package com.codeatlas.core.decision;

public record DecisionScope(
        String kind,
        String entrypoint,
        String endpoint
) {
}
