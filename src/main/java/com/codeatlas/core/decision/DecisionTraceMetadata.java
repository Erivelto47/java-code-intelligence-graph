package com.codeatlas.core.decision;

public record DecisionTraceMetadata(
        String analyzer,
        String phase,
        boolean deterministic,
        String source
) {
}
