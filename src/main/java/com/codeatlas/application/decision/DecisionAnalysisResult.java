package com.codeatlas.application.decision;

import com.codeatlas.core.decision.DecisionTrace;

import java.nio.file.Path;

public record DecisionAnalysisResult(
        DecisionTrace trace,
        Path outputDirectory,
        Path jsonPath,
        Path markdownPath,
        Path mermaidPath
) {
}
