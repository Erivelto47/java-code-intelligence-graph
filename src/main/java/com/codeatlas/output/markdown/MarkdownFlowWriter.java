package com.codeatlas.output.markdown;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MarkdownFlowWriter {
    public Path write(FlowGraph graph, Path outputDirectory) throws IOException {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("flow.md");
        Files.writeString(outputFile, render(graph), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static String render(FlowGraph graph) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Java Flow Graph\n\n");
        markdown.append("Schema version: `").append(escapeInline(graph.schemaVersion())).append("`\n\n");
        markdown.append("Entrypoint: `").append(escapeInline(graph.entrypoint())).append("`\n\n");
        markdown.append("Generated at: `").append(graph.generatedAt()).append("`\n\n");

        markdown.append("## Nodes\n\n");
        markdown.append("| ID | Kind | Qualified name | Display name |\n");
        markdown.append("| --- | --- | --- | --- |\n");
        for (GraphNode node : graph.nodes()) {
            markdown.append("| ")
                    .append(escapeTable(node.id()))
                    .append(" | ")
                    .append(escapeTable(node.kind()))
                    .append(" | ")
                    .append(escapeTable(node.qualifiedName()))
                    .append(" | ")
                    .append(escapeTable(node.displayName()))
                    .append(" |\n");
        }

        markdown.append("\n## Edges\n\n");
        if (graph.edges().isEmpty()) {
            markdown.append("No edges.\n");
        } else {
            markdown.append("| ID | Kind | Source | Target |\n");
            markdown.append("| --- | --- | --- | --- |\n");
            for (GraphEdge edge : graph.edges()) {
                markdown.append("| ")
                        .append(escapeTable(edge.id()))
                        .append(" | ")
                        .append(escapeTable(edge.kind()))
                        .append(" | ")
                        .append(escapeTable(edge.sourceNodeId()))
                        .append(" | ")
                        .append(escapeTable(edge.targetNodeId()))
                        .append(" |\n");
            }
        }

        return markdown.toString();
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
}
