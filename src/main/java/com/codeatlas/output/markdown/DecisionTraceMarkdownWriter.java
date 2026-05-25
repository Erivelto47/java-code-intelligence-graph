package com.codeatlas.output.markdown;

import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.UnresolvedDecision;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DecisionTraceMarkdownWriter {
    public Path write(DecisionTrace trace, Path outputDirectory) throws IOException {
        Objects.requireNonNull(trace, "trace must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("decisions.md");
        Files.writeString(outputFile, render(trace), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static String render(DecisionTrace trace) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Decision Trace\n\n");
        markdown.append("Schema version: `").append(escapeInline(trace.schemaVersion())).append("`\n\n");
        markdown.append("Entrypoint: `").append(escapeInline(trace.scope().entrypoint())).append("`\n\n");
        markdown.append("Generated at: `").append(trace.generatedAt()).append("`\n\n");

        markdown.append("## Decisions\n\n");
        if (trace.decisions().isEmpty()) {
            markdown.append("No decisions detected.\n");
        } else {
            for (DecisionNode decision : trace.decisions()) {
                markdown.append("### ").append(escapeInline(decision.id())).append("\n\n");
                markdown.append("Kind: `").append(decision.kind()).append("`\n\n");
                markdown.append("Category: `").append(decision.category()).append("`\n\n");
                markdown.append("Source: `").append(escapeInline(decision.method())).append("`\n\n");
                markdown.append("Condition:\n\n");
                markdown.append("```java\n");
                markdown.append(decision.expression().text()).append("\n");
                markdown.append("```\n\n");
                markdown.append("Outcome:\n\n");
                markdown.append("```text\n");
                markdown.append(primaryOutcome(decision)).append("\n");
                markdown.append("```\n\n");
                markdown.append("Location:\n\n");
                markdown.append("```text\n");
                markdown.append(decision.sourceLocation().file())
                        .append(":")
                        .append(decision.sourceLocation().line())
                        .append("\n");
                markdown.append("```\n\n");
            }
        }

        markdown.append("## Unresolved\n\n");
        if (trace.unresolved().isEmpty()) {
            markdown.append("No unresolved decision items.\n");
        } else {
            markdown.append("| ID | Kind | Method | Location | Message |\n");
            markdown.append("| --- | --- | --- | --- | --- |\n");
            for (UnresolvedDecision unresolved : trace.unresolved()) {
                String location = unresolved.sourceLocation() == null
                        ? ""
                        : unresolved.sourceLocation().file() + ":" + unresolved.sourceLocation().line();
                markdown.append("| ")
                        .append(escapeTable(unresolved.id()))
                        .append(" | ")
                        .append(escapeTable(unresolved.kind()))
                        .append(" | ")
                        .append(escapeTable(unresolved.method()))
                        .append(" | ")
                        .append(escapeTable(location))
                        .append(" | ")
                        .append(escapeTable(unresolved.message()))
                        .append(" |\n");
            }
        }

        return markdown.toString();
    }

    private static String primaryOutcome(DecisionNode decision) {
        for (DecisionOutcome outcome : decision.outcomes()) {
            if ("true".equals(outcome.when()) && outcome.action() != null) {
                if (outcome.exceptionType() != null) {
                    return "throws " + outcome.exceptionType() + "(\"" + escapeText(outcome.message()) + "\")";
                }
                return outcome.action() + " " + nullToEmpty(outcome.target());
            }
        }
        return "UNKNOWN";
    }

    private static String escapeTable(String value) {
        return escapeInline(value).replace("|", "\\|");
    }

    private static String escapeInline(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "\\`");
    }

    private static String escapeText(String value) {
        return nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
