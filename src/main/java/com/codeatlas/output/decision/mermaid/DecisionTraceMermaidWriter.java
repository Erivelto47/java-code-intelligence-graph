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
import java.util.HashMap;
import java.util.Map;
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

        Map<String, String> aliasesByDecisionId = new HashMap<>();
        for (int i = 0; i < trace.decisions().size(); i++) {
            aliasesByDecisionId.put(trace.decisions().get(i).id(), "d" + (i + 1));
        }

        for (int i = 0; i < trace.decisions().size(); i++) {
            DecisionNode decision = trace.decisions().get(i);
            String decisionAlias = aliasesByDecisionId.get(decision.id());
            String outcomeAlias = "o" + (i + 1);
            boolean ifElseDecision = decision.kind() == DecisionKind.IF_ELSE_CONDITION;
            mermaid.append("  ")
                    .append(decisionAlias)
                    .append("{\"")
                    .append(escapeLabel(decision.expression().text()))
                    .append("\"}\n");
            if (!decision.branches().isEmpty()) {
                appendEntryEdge(mermaid, decision, decisionAlias);
                appendBranchOutcomes(mermaid, decision, decisionAlias, outcomeAlias, aliasesByDecisionId);
            } else if (ifElseDecision) {
                appendEntryEdge(mermaid, decision, decisionAlias);
                appendIfElseOutcomes(mermaid, decision, decisionAlias, outcomeAlias);
            } else {
                mermaid.append("  ")
                        .append(outcomeAlias)
                        .append("[\"")
                        .append(escapeLabel(primaryOutcomeLabel(decision)))
                        .append("\"]\n");
                appendEntryEdge(mermaid, decision, decisionAlias);
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

    private static void appendEntryEdge(StringBuilder mermaid, DecisionNode decision, String decisionAlias) {
        if (decision.parent() == null) {
            mermaid.append("  entry --> ").append(decisionAlias).append("\n");
        }
    }

    private static void appendBranchOutcomes(
            StringBuilder mermaid,
            DecisionNode decision,
            String decisionAlias,
            String outcomeAlias,
            Map<String, String> aliasesByDecisionId
    ) {
        decision.branches().forEach(branch -> {
            String branchAlias = outcomeAlias + "b" + branch.order();
            String label = branch.kind();
            if (branch.condition() != null && !branch.condition().isBlank()) {
                label += ": " + branch.condition();
            }
            if (!branch.outcomes().isEmpty()) {
                label += "<br/>" + returnOutcomeLabel(branch.outcomes().get(0));
            }
            mermaid.append("  ")
                    .append(branchAlias)
                    .append("[\"")
                    .append(escapeLabel(label))
                    .append("\"]\n");
            mermaid.append("  ")
                    .append(decisionAlias)
                    .append(" -- ")
                    .append(branch.order())
                    .append(" --> ")
                    .append(branchAlias)
                    .append("\n");
            decision.children().stream()
                    .filter(child -> child.parentBranchOrder() == branch.order())
                    .map(child -> aliasesByDecisionId.get(child.decisionId()))
                    .filter(Objects::nonNull)
                    .forEach(childAlias -> mermaid.append("  ")
                            .append(branchAlias)
                            .append(" --> ")
                            .append(childAlias)
                            .append("\n"));
        });
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
        if (outcome.action() == DecisionOutcomeAction.THROW) {
            String label = "throws " + outcome.exceptionType();
            if (outcome.message() != null && !outcome.message().isBlank()) {
                label += ": " + outcome.message();
            }
            return label;
        }
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
