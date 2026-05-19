package com.codeatlas.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FlowGraph(
        String schemaVersion,
        String entrypoint,
        Instant generatedAt,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        Map<String, Object> metadata
) {
}
