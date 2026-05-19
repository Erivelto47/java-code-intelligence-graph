package com.codeatlas.output.context;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ContextPackWriter {
    public Path write(FlowGraph graph, Path outputDirectory) throws IOException {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("context-pack.md");
        Files.writeString(outputFile, render(graph), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static String render(FlowGraph graph) {
        StringBuilder context = new StringBuilder();
        context.append("# Context Pack\n\n");
        context.append("## Deterministic Facts\n\n");
        context.append("- Schema version: `").append(escapeInline(graph.schemaVersion())).append("`\n");
        context.append("- Entrypoint: `").append(escapeInline(graph.entrypoint())).append("`\n");
        context.append("- Node count: ").append(graph.nodes().size()).append("\n");
        context.append("- Edge count: ").append(graph.edges().size()).append("\n\n");

        context.append("## Nodes\n\n");
        for (GraphNode node : graph.nodes()) {
            context.append("- `")
                    .append(escapeInline(node.id()))
                    .append("` ")
                    .append(escapeInline(node.kind()))
                    .append(" ")
                    .append(escapeInline(node.qualifiedName()))
                    .append("\n");
        }

        context.append("\n## Edges\n\n");
        if (graph.edges().isEmpty()) {
            context.append("No edges.\n");
        } else {
            for (GraphEdge edge : graph.edges()) {
                context.append("- `")
                        .append(escapeInline(edge.id()))
                        .append("` ")
                        .append(escapeInline(edge.sourceNodeId()))
                        .append(" -> ")
                        .append(escapeInline(edge.targetNodeId()))
                        .append(" (")
                        .append(escapeInline(edge.kind()))
                        .append(")\n");
            }
        }

        context.append("\n## AI Interpretations\n\n");
        context.append("None. This artifact contains deterministic facts only.\n");
        return context.toString();
    }

    private static String escapeInline(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "\\`");
    }
}
