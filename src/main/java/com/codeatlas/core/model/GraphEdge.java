package com.codeatlas.core.model;

import java.util.Map;

public record GraphEdge(
        String id,
        String kind,
        String sourceNodeId,
        String targetNodeId,
        Map<String, Object> attributes
) {
}
