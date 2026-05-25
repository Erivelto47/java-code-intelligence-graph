package com.codeatlas.core.project;

import java.util.List;

public record ProjectIndexUsage(
        boolean assisted,
        String source,
        String status,
        int implementations,
        List<String> diagnostics,
        boolean staleSuspected,
        List<String> staleReasons
) {
    public static final String SOURCE_JSON = "json";
    public static final String SOURCE_MEMORY = "memory";
    public static final String SOURCE_NONE = "none";
    public static final String STATUS_LOADED_FROM_JSON = "LOADED_FROM_JSON";
    public static final String STATUS_FALLBACK_MEMORY_MISSING_JSON = "FALLBACK_MEMORY_MISSING_JSON";
    public static final String STATUS_FALLBACK_MEMORY_INVALID_JSON = "FALLBACK_MEMORY_INVALID_JSON";
    public static final String STATUS_NOT_USED = "NOT_USED";

    public ProjectIndexUsage {
        source = source == null || source.isBlank() ? SOURCE_NONE : source;
        status = status == null || status.isBlank() ? STATUS_NOT_USED : status;
        implementations = Math.max(0, implementations);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        staleReasons = staleReasons == null ? List.of() : List.copyOf(staleReasons);
    }

    public static ProjectIndexUsage notUsed() {
        return new ProjectIndexUsage(false, SOURCE_NONE, STATUS_NOT_USED, 0, List.of(), false, List.of());
    }

    public static ProjectIndexUsage loadedFromJson(
            int implementations,
            List<String> diagnostics,
            boolean staleSuspected,
            List<String> staleReasons
    ) {
        return new ProjectIndexUsage(
                true,
                SOURCE_JSON,
                STATUS_LOADED_FROM_JSON,
                implementations,
                diagnostics,
                staleSuspected,
                staleReasons
        );
    }

    public static ProjectIndexUsage fallbackMemoryMissingJson(int implementations, List<String> diagnostics) {
        return new ProjectIndexUsage(
                true,
                SOURCE_MEMORY,
                STATUS_FALLBACK_MEMORY_MISSING_JSON,
                implementations,
                diagnostics,
                false,
                List.of()
        );
    }

    public static ProjectIndexUsage fallbackMemoryInvalidJson(
            int implementations,
            List<String> diagnostics,
            boolean staleSuspected,
            List<String> staleReasons
    ) {
        return new ProjectIndexUsage(
                true,
                SOURCE_MEMORY,
                STATUS_FALLBACK_MEMORY_INVALID_JSON,
                implementations,
                diagnostics,
                staleSuspected,
                staleReasons
        );
    }

    public static ProjectIndexUsage legacy(String source, int implementations) {
        if (source == null || source.isBlank() || SOURCE_NONE.equals(source)) {
            return notUsed();
        }
        String status = SOURCE_MEMORY.equals(source)
                ? STATUS_FALLBACK_MEMORY_MISSING_JSON
                : STATUS_LOADED_FROM_JSON;
        return new ProjectIndexUsage(true, source, status, implementations, List.of(), false, List.of());
    }
}
