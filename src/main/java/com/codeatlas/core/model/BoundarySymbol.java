package com.codeatlas.core.model;

import java.util.Map;

public record BoundarySymbol(
        String symbol,
        String nodeId,
        String fromNodeId,
        String kind,
        String reason,
        String confidence,
        Map<String, Object> attributes
) {
}
