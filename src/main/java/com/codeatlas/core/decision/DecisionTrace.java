package com.codeatlas.core.decision;

import java.time.Instant;
import java.util.List;

public record DecisionTrace(
        String schemaVersion,
        Instant generatedAt,
        String project,
        DecisionScope scope,
        DecisionArtifactSource source,
        List<DecisionNode> decisions,
        List<UnresolvedDecision> unresolved,
        DecisionTraceMetadata metadata
) {
}
