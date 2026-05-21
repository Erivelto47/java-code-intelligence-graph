package com.codeatlas.core.model;

import java.util.Map;

public record Resolution(
        String sourceNodeId,
        String targetNodeId,
        String kind,
        String evidence,
        String confidence,
        Map<String, Object> attributes
) {
}
