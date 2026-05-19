package com.codeatlas.core.analyzer;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphNode;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StubFlowAnalyzer implements FlowAnalyzer {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;

    @Override
    public FlowGraph analyze(Path projectPath, String entrypoint) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        String normalizedEntrypoint = requireEntrypoint(entrypoint);

        Map<String, Object> nodeAttributes = new LinkedHashMap<>();
        nodeAttributes.put("entrypoint", true);

        GraphNode entrypointNode = new GraphNode(
                "method:" + normalizedEntrypoint,
                "METHOD",
                normalizedEntrypoint,
                displayName(normalizedEntrypoint),
                nodeAttributes
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analyzer", "stub-flow-analyzer");
        metadata.put("phase", "phase-1-mvp");
        metadata.put("deterministic", true);

        return new FlowGraph(
                SCHEMA_VERSION,
                normalizedEntrypoint,
                DETERMINISTIC_GENERATED_AT,
                List.of(entrypointNode),
                List.of(),
                metadata
        );
    }

    private static String requireEntrypoint(String entrypoint) {
        if (entrypoint == null || entrypoint.isBlank()) {
            throw new IllegalArgumentException("entrypoint must not be blank");
        }
        return entrypoint.trim();
    }

    private static String displayName(String entrypoint) {
        int separator = entrypoint.lastIndexOf('.');
        if (separator < 0 || separator == entrypoint.length() - 1) {
            return entrypoint;
        }
        return entrypoint.substring(separator + 1);
    }
}
