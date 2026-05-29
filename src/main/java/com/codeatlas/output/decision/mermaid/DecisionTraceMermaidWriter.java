package com.codeatlas.output.decision.mermaid;

import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionKind;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.DecisionOutcomeAction;
import com.codeatlas.core.decision.UnresolvedDecision;

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
            boolean ifElseDecision = decision.kind() == DecisionKind.IF_ELSE_CONDITION;
            mermaid.append("  ")
                    .append(decisionAlias)
                    .append("{\"")
                    .append(escapeLabel(decision.expression().text()))
                    .append("\"}\n");
            if (ifElseDecision) {
                mermaid.append("  entry --> ").append(decisionAlias).append("\n");
                appendIfElseOutcomes(mermaid, decision, decisionAlias, outcomeAlias);
            } else {
                mermaid.append("  ")
                        .append(outcomeAlias)
                        .append("[\"")
                        .append(escapeLabel(primaryOutcomeLabel(decision)))
                        .append("\"]\n");
                mermaid.append("  entry --> ").append(decisionAlias).append("\n");
                mermaid.append("  ").append(decisionAlias).append(" --> ").append(outcomeAlias).append("\n");
            }
        }

        for (int i = 0; i < trace.unresolved().size(); i++) {
            UnresolvedDecision unresolved = trace.unresolved().get(i);
            String unresolvedAlias = "u" + (i + 1);
            mermaid.append("  ")
                    .append(unresolvedAlias)
                    .append("[\"")
                    .append(escapeLabel("unresolved: " + unresolved.kind()))
                    .append("\"]\n");
            mermaid.append("  entry -.-> ").append(unresolvedAlias).append("\n");
        }

        return mermaid.toString();
    }

    private static void appendIfElseOutcomes(
            StringBuilder mermaid,
            DecisionNode decision,
            String decisionAlias,
            String outcomeAlias
    ) {
        for (DecisionOutcome outcome : decision.outcomes()) {
            if (!"true".equals(outcome.when()) && !"false".equals(outcome.when())) {
                continue;
            }
            String branchAlias = outcomeAlias + ("true".equals(outcome.when()) ? "t" : "f");
            mermaid.append("  ")
                    .append(branchAlias)
                    .append("[\"")
                    .append(escapeLabel(returnOutcomeLabel(outcome)))
                    .append("\"]\n");
            mermaid.append("  ")
                    .append(decisionAlias)
                    .append(" -- ")
                    .append(outcome.when())
                    .append(" --> ")
                    .append(branchAlias)
                    .append("\n");
        }
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
            if ("true".equals(outcome.when()) && outcome.action() == DecisionOutcomeAction.RETURN) {
                if (outcome.target() == null || outcome.target().isBlank()) {
                    return "returns";
                }
                return "returns " + outcome.target();
            }
        }
        return "UNKNOWN";
    }

    private static String returnOutcomeLabel(DecisionOutcome outcome) {
        if (outcome.action() != DecisionOutcomeAction.RETURN) {
            return outcome.action().name();
        }
        if (outcome.target() == null || outcome.target().isBlank()) {
            return "returns";
        }
        return "returns " + outcome.target();
    }

    private static String escapeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
