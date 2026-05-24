package com.codeatlas.core.project;

import com.codeatlas.core.entrypoint.SourceLocation;

import java.util.Map;

public record UnresolvedProjectSymbol(
        String symbol,
        String reason,
        SourceLocation sourceLocation,
        Map<String, Object> attributes
) {
}
