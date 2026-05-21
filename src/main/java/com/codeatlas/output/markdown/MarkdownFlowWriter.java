package com.codeatlas.output.markdown;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;
import com.codeatlas.core.model.BoundarySymbol;
import com.codeatlas.core.model.Resolution;
import com.codeatlas.core.model.UnresolvedSymbol;

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

        markdown.append("\n## Resolutions\n\n");
        if (graph.resolutions().isEmpty()) {
            markdown.append("No inferred resolutions.\n");
        } else {
            markdown.append("| Kind | Source | Target | Evidence | Confidence |\n");
            markdown.append("| --- | --- | --- | --- | --- |\n");
            for (Resolution resolution : graph.resolutions()) {
                markdown.append("| ")
                        .append(escapeTable(resolution.kind()))
                        .append(" | ")
                        .append(escapeTable(resolution.sourceNodeId()))
                        .append(" | ")
                        .append(escapeTable(resolution.targetNodeId()))
                        .append(" | ")
                        .append(escapeTable(resolution.evidence()))
                        .append(" | ")
                        .append(escapeTable(resolution.confidence()))
                        .append(" |\n");
            }
        }

        markdown.append("\n## Boundaries\n\n");
        if (graph.boundaries().isEmpty()) {
            markdown.append("No boundaries.\n");
        } else {
            markdown.append("| Symbol | Kind | From | Reason | Confidence |\n");
            markdown.append("| --- | --- | --- | --- | --- |\n");
            for (BoundarySymbol boundary : graph.boundaries()) {
                markdown.append("| ")
                        .append(escapeTable(boundary.symbol()))
                        .append(" | ")
                        .append(escapeTable(boundary.kind()))
                        .append(" | ")
                        .append(escapeTable(boundary.fromNodeId()))
                        .append(" | ")
                        .append(escapeTable(boundary.reason()))
                        .append(" | ")
                        .append(escapeTable(boundary.confidence()))
                        .append(" |\n");
            }
        }

        markdown.append("\n## Unresolved\n\n");
        if (graph.unresolved().isEmpty()) {
            markdown.append("No unresolved symbols.\n");
        } else {
            markdown.append("| Symbol | From | Reason | Confidence | Candidates |\n");
            markdown.append("| --- | --- | --- | --- | --- |\n");
            for (UnresolvedSymbol unresolved : graph.unresolved()) {
                markdown.append("| ")
                        .append(escapeTable(unresolved.symbol()))
                        .append(" | ")
                        .append(escapeTable(unresolved.fromNodeId()))
                        .append(" | ")
                        .append(escapeTable(unresolved.reason()))
                        .append(" | ")
                        .append(escapeTable(unresolved.confidence()))
                        .append(" | ")
                        .append(escapeTable(String.join(", ", unresolved.candidates())))
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
