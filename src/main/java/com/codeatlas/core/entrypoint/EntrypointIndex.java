package com.codeatlas.core.entrypoint;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EntrypointIndex(
        String schemaVersion,
        String project,
        Instant generatedAt,
        List<EntrypointDescriptor> entrypoints,
        Map<String, Object> metadata
) {
}
