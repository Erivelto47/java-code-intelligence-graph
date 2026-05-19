package com.codeatlas.core.model;

import java.util.Map;

public record GraphNode(
        String id,
        String kind,
        String qualifiedName,
        String displayName,
        Map<String, Object> attributes
) {
}
