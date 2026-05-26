package com.codeatlas.application.decision;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public record DecisionAnalysisRequest(
        Path projectPath,
        String entrypoint,
        String language,
        Path outputDirectory
) {
    public DecisionAnalysisRequest {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        if (entrypoint == null || entrypoint.isBlank()) {
            throw new IllegalArgumentException("entrypoint must not be blank");
        }
        entrypoint = entrypoint.trim();
        language = language == null || language.isBlank()
                ? "java"
                : language.trim().toLowerCase(Locale.ROOT);
    }
}
