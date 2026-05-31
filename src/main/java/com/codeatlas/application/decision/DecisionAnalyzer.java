package com.codeatlas.application.decision;

import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.output.decision.json.DecisionTraceJsonWriter;
import com.codeatlas.output.decision.markdown.DecisionTraceMarkdownWriter;
import com.codeatlas.output.decision.mermaid.DecisionTraceMermaidWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class DecisionAnalyzer {
    private final List<LanguageDecisionAdapter> languageAdapters;
    private final DecisionTraceJsonWriter jsonWriter;
    private final DecisionTraceMarkdownWriter markdownWriter;
    private final DecisionTraceMermaidWriter mermaidWriter;

    public DecisionAnalyzer(
            List<LanguageDecisionAdapter> languageAdapters,
            DecisionTraceJsonWriter jsonWriter,
            DecisionTraceMarkdownWriter markdownWriter,
            DecisionTraceMermaidWriter mermaidWriter
    ) {
        this.languageAdapters = List.copyOf(Objects.requireNonNull(
                languageAdapters,
                "languageAdapters must not be null"
        ));
        if (this.languageAdapters.isEmpty()) {
            throw new IllegalArgumentException("At least one language decision adapter is required");
        }
        this.jsonWriter = Objects.requireNonNull(jsonWriter, "jsonWriter must not be null");
        this.markdownWriter = Objects.requireNonNull(markdownWriter, "markdownWriter must not be null");
        this.mermaidWriter = Objects.requireNonNull(mermaidWriter, "mermaidWriter must not be null");
    }

    public DecisionAnalysisResult analyze(DecisionAnalysisRequest request) throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        LanguageDecisionAdapter adapter = languageAdapters.stream()
                .filter(candidate -> candidate.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No decision adapter supports language: " + request.language()
                ));

        DecisionTrace trace = adapter.analyze(request);
        Path outputDirectory = request.outputDirectory();
        Path jsonPath = jsonWriter.write(trace, outputDirectory);
        Path markdownPath = markdownWriter.write(trace, outputDirectory);
        Path mermaidPath = mermaidWriter.write(trace, outputDirectory);
        return new DecisionAnalysisResult(trace, outputDirectory, jsonPath, markdownPath, mermaidPath);
    }
}
