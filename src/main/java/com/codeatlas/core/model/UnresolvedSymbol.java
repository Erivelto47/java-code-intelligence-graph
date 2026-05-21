package com.codeatlas.core.model;

import java.util.List;
import java.util.Map;

public record UnresolvedSymbol(
        String symbol,
        String fromNodeId,
        String reason,
        List<String> candidates,
        String confidence,
        Map<String, Object> attributes
) {
}
