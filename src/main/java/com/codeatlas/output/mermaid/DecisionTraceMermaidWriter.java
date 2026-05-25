package com.codeatlas.output.mermaid;

import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.DecisionOutcomeAction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DecisionTraceMermaidWriter {
    public Path write(DecisionTrace trace, Path outputDirectory) throws IOException {
        Objects.requireNonNull(trace, "trace must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("decisions.mmd");
        Files.writeString(outputFile, render(trace), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static String render(DecisionTrace trace) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("flowchart TD\n");
        mermaid.append("  entry[\"").append(escapeLabel(trace.scope().entrypoint())).append("\"]\n");

        for (int i = 0; i < trace.decisions().size(); i++) {
            DecisionNode decision = trace.decisions().get(i);
            String decisionAlias = "d" + (i + 1);
            String outcomeAlias = "o" + (i + 1);
            mermaid.append("  ")
                    .append(decisionAlias)
                    .append("{\"")
                    .append(escapeLabel(decision.expression().text()))
                    .append("\"}\n");
            mermaid.append("  ")
                    .append(outcomeAlias)
                    .append("[\"")
                    .append(escapeLabel(primaryOutcomeLabel(decision)))
                    .append("\"]\n");
            mermaid.append("  entry --> ").append(decisionAlias).append("\n");
            mermaid.append("  ").append(decisionAlias).append(" --> ").append(outcomeAlias).append("\n");
        }

        return mermaid.toString();
    }

    private static String primaryOutcomeLabel(DecisionNode decision) {
        for (DecisionOutcome outcome : decision.outcomes()) {
            if ("true".equals(outcome.when()) && outcome.action() == DecisionOutcomeAction.THROW) {
                String label = "throws " + outcome.exceptionType();
                if (outcome.message() != null && !outcome.message().isBlank()) {
                    label += ": " + outcome.message();
                }
                return label;
            }
        }
        return "UNKNOWN";
    }

    private static String escapeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
