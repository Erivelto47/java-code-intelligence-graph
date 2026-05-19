package com.codeatlas.output.mermaid;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MermaidFlowWriter {
    public Path write(FlowGraph graph, Path outputDirectory) throws IOException {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("flow.mmd");
        Files.writeString(outputFile, render(graph), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static String render(FlowGraph graph) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("flowchart TD\n");

        Map<String, String> nodeAliases = new LinkedHashMap<>();
        for (int i = 0; i < graph.nodes().size(); i++) {
            GraphNode node = graph.nodes().get(i);
            String alias = "n" + i;
            nodeAliases.put(node.id(), alias);
            mermaid.append("  ")
                    .append(alias)
                    .append("[\"")
                    .append(escapeLabel(node.qualifiedName()))
                    .append("\"]\n");
        }

        for (GraphEdge edge : graph.edges()) {
            String sourceAlias = nodeAliases.get(edge.sourceNodeId());
            String targetAlias = nodeAliases.get(edge.targetNodeId());
            if (sourceAlias != null && targetAlias != null) {
                mermaid.append("  ")
                        .append(sourceAlias)
                        .append(" -->|")
                        .append(escapeLabel(edge.kind()))
                        .append("| ")
                        .append(targetAlias)
                        .append("\n");
            }
        }

        return mermaid.toString();
    }

    private static String escapeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
