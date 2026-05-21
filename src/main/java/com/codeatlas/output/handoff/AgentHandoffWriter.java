package com.codeatlas.output.handoff;

import com.codeatlas.core.model.BoundarySymbol;
import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;
import com.codeatlas.core.model.Resolution;
import com.codeatlas.core.model.UnresolvedSymbol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AgentHandoffWriter {
    public Path write(
            FlowGraph graph,
            Path projectPath,
            Path outputDirectory,
            String repository,
            boolean outputExplicit,
            boolean useStub
    ) throws IOException {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(repository, "repository must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("agent-handoff.md");
        Files.writeString(
                outputFile,
                render(graph, projectPath, outputDirectory, repository, outputExplicit, useStub),
                StandardCharsets.UTF_8
        );
        return outputFile;
    }

    private static String render(
            FlowGraph graph,
            Path projectPath,
            Path outputDirectory,
            String repository,
            boolean outputExplicit,
            boolean useStub
    ) {
        String projectDisplayPath = portablePath(projectPath.normalize());
        String flowPath = flowPath(projectPath, outputDirectory, outputExplicit);
        List<String> sourceFiles = sourceFiles(graph);
        String analyzer = metadataValue(graph, "analyzer");
        String deterministic = metadataValue(graph, "deterministic");

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Agent Handoff\n\n");
        markdown.append("## Repository\n\n");
        markdown.append(escapeText(repository)).append("\n\n");
        markdown.append("## Project Path\n\n");
        markdown.append("`").append(escapeInline(projectDisplayPath)).append("`\n\n");
        markdown.append("## Entrypoint\n\n");
        markdown.append("`").append(escapeInline(graph.entrypoint())).append("`\n\n");
        markdown.append("## Flow Path\n\n");
        markdown.append("`").append(escapeInline(flowPath)).append("`\n\n");
        markdown.append("## Source Files\n\n");
        appendSourceFiles(markdown, sourceFiles);
        markdown.append("\n## Generated Artifacts\n\n");
        appendArtifact(markdown, flowPath, "flow.json");
        appendArtifact(markdown, flowPath, "flow.md");
        appendArtifact(markdown, flowPath, "flow.mmd");
        appendArtifact(markdown, flowPath, "context-pack.md");
        appendArtifact(markdown, flowPath, "agent-handoff.md");
        markdown.append("\n## Exact Paths\n\n");
        appendExactSourcePaths(markdown, projectPath, sourceFiles);
        appendExactArtifact(markdown, outputDirectory, "flow.json");
        appendExactArtifact(markdown, outputDirectory, "flow.md");
        appendExactArtifact(markdown, outputDirectory, "flow.mmd");
        appendExactArtifact(markdown, outputDirectory, "context-pack.md");
        appendExactArtifact(markdown, outputDirectory, "agent-handoff.md");
        markdown.append("\n## Graph Summary\n\n");
        markdown.append("- Schema version: `").append(escapeInline(graph.schemaVersion())).append("`\n");
        markdown.append("- Node count: `").append(graph.nodes().size()).append("`\n");
        markdown.append("- Edge count: `").append(graph.edges().size()).append("`\n");
        markdown.append("- Resolution count: `").append(graph.resolutions().size()).append("`\n");
        markdown.append("- Boundary count: `").append(graph.boundaries().size()).append("`\n");
        markdown.append("- Unresolved count: `").append(graph.unresolved().size()).append("`\n");
        markdown.append("- Analyzer: `").append(escapeInline(analyzer)).append("`\n");
        markdown.append("- Deterministic: `").append(escapeInline(deterministic)).append("`\n\n");
        markdown.append("## Detected Nodes\n\n");
        appendDetectedNodes(markdown, graph);
        markdown.append("\n## Detected Edges\n\n");
        appendDetectedEdges(markdown, graph);
        markdown.append("\n## Inferred Resolutions\n\n");
        appendInferredResolutions(markdown, graph);
        markdown.append("\n## Boundaries\n\n");
        appendBoundaries(markdown, graph);
        markdown.append("\n## Unresolved\n\n");
        appendUnresolved(markdown, graph);
        markdown.append("\n## Commands\n\n");
        markdown.append("```bash\n");
        markdown.append(command(projectDisplayPath, graph.entrypoint(), outputDirectory, outputExplicit, useStub));
        markdown.append("\n```\n\n");
        markdown.append("## Notes\n\n");
        markdown.append("This handoff is intended for agents that can read files by exact path but cannot freely traverse repository directories.\n");
        return markdown.toString();
    }

    private static void appendSourceFiles(StringBuilder markdown, List<String> sourceFiles) {
        if (sourceFiles.isEmpty()) {
            markdown.append("- `No source files detected`\n");
            return;
        }
        for (String sourceFile : sourceFiles) {
            markdown.append("- `").append(escapeInline(sourceFile)).append("`\n");
        }
    }

    private static void appendArtifact(StringBuilder markdown, String flowPath, String artifactName) {
        markdown.append("- `")
                .append(escapeInline(artifactName))
                .append("` - `")
                .append(escapeInline(artifactPath(flowPath, artifactName)))
                .append("`\n");
    }

    private static void appendExactSourcePaths(StringBuilder markdown, Path projectPath, List<String> sourceFiles) {
        if (sourceFiles.isEmpty()) {
            markdown.append("- `source` - `No source files detected`\n");
            return;
        }
        for (String sourceFile : sourceFiles) {
            markdown.append("- `source` - `")
                    .append(escapeInline(absolutePath(projectPath.resolve(sourceFile))))
                    .append("`\n");
        }
    }

    private static void appendExactArtifact(StringBuilder markdown, Path outputDirectory, String artifactName) {
        markdown.append("- `")
                .append(escapeInline(artifactName))
                .append("` - `")
                .append(escapeInline(absolutePath(outputDirectory.resolve(artifactName))))
                .append("`\n");
    }

    private static void appendDetectedNodes(StringBuilder markdown, FlowGraph graph) {
        if (graph.nodes().isEmpty()) {
            markdown.append("- `No nodes detected`\n");
            return;
        }
        for (GraphNode node : graph.nodes()) {
            markdown.append("- `")
                    .append(escapeInline(node.id()))
                    .append("` ")
                    .append(escapeInline(node.kind()))
                    .append(" `")
                    .append(escapeInline(node.qualifiedName()))
                    .append("`\n");
        }
    }

    private static void appendDetectedEdges(StringBuilder markdown, FlowGraph graph) {
        if (graph.edges().isEmpty()) {
            markdown.append("- `No edges detected`\n");
            return;
        }
        for (GraphEdge edge : graph.edges()) {
            markdown.append("- `")
                    .append(escapeInline(edge.kind()))
                    .append("` `")
                    .append(escapeInline(edge.sourceNodeId()))
                    .append("` -> `")
                    .append(escapeInline(edge.targetNodeId()))
                    .append("`\n");
        }
    }

    private static void appendInferredResolutions(StringBuilder markdown, FlowGraph graph) {
        if (graph.resolutions().isEmpty()) {
            markdown.append("- `No inferred resolutions`\n");
            return;
        }
        for (Resolution resolution : graph.resolutions()) {
            markdown.append("- `")
                    .append(escapeInline(resolution.kind()))
                    .append("` `")
                    .append(escapeInline(resolution.sourceNodeId()))
                    .append("` -> `")
                    .append(escapeInline(resolution.targetNodeId()))
                    .append("` evidence=`")
                    .append(escapeInline(resolution.evidence()))
                    .append("` confidence=`")
                    .append(escapeInline(resolution.confidence()))
                    .append("`\n");
        }
    }

    private static void appendBoundaries(StringBuilder markdown, FlowGraph graph) {
        if (graph.boundaries().isEmpty()) {
            markdown.append("- `No boundaries`\n");
            return;
        }
        for (BoundarySymbol boundary : graph.boundaries()) {
            markdown.append("- `")
                    .append(escapeInline(boundary.symbol()))
                    .append("` kind=`")
                    .append(escapeInline(boundary.kind()))
                    .append("` from=`")
                    .append(escapeInline(boundary.fromNodeId()))
                    .append("` reason=`")
                    .append(escapeInline(boundary.reason()))
                    .append("` confidence=`")
                    .append(escapeInline(boundary.confidence()))
                    .append("`\n");
        }
    }

    private static void appendUnresolved(StringBuilder markdown, FlowGraph graph) {
        if (graph.unresolved().isEmpty()) {
            markdown.append("- `No unresolved symbols`\n");
            return;
        }
        for (UnresolvedSymbol unresolved : graph.unresolved()) {
            markdown.append("- `")
                    .append(escapeInline(unresolved.symbol()))
                    .append("` from=`")
                    .append(escapeInline(unresolved.fromNodeId()))
                    .append("` reason=`")
                    .append(escapeInline(unresolved.reason()))
                    .append("` confidence=`")
                    .append(escapeInline(unresolved.confidence()))
                    .append("` candidates=`")
                    .append(escapeInline(String.join(", ", unresolved.candidates())))
                    .append("`\n");
        }
    }

    private static List<String> sourceFiles(FlowGraph graph) {
        Set<String> sourceFiles = new LinkedHashSet<>();
        for (GraphNode node : graph.nodes()) {
            Object sourceFile = node.attributes().get("sourceFile");
            if (sourceFile != null && !sourceFile.toString().isBlank()) {
                sourceFiles.add(portablePath(Path.of(sourceFile.toString())));
            }
        }
        return List.copyOf(sourceFiles);
    }

    private static String command(
            String projectDisplayPath,
            String entrypoint,
            Path outputDirectory,
            boolean outputExplicit,
            boolean useStub
    ) {
        StringBuilder command = new StringBuilder();
        command.append("./gradlew run --args=\"--project ")
                .append(escapeCommandArgument(projectDisplayPath))
                .append(" --entrypoint ")
                .append(escapeCommandArgument(entrypoint));
        if (outputExplicit) {
            command.append(" --output ")
                    .append(escapeCommandArgument(portablePath(outputDirectory.normalize())));
        }
        if (useStub) {
            command.append(" --stub");
        }
        command.append("\"");
        return command.toString();
    }

    private static String flowPath(Path projectPath, Path outputDirectory, boolean outputExplicit) {
        if (outputExplicit) {
            return portablePath(outputDirectory.normalize());
        }
        return projectRelativePath(projectPath, outputDirectory);
    }

    private static String artifactPath(String flowPath, String fileName) {
        if (flowPath.isBlank()) {
            return fileName;
        }
        return flowPath + "/" + fileName;
    }

    private static String absolutePath(Path path) {
        return portablePath(path.toAbsolutePath().normalize());
    }

    private static String projectRelativePath(Path projectPath, Path path) {
        Path absoluteProjectPath = projectPath.toAbsolutePath().normalize();
        Path absolutePath = path.toAbsolutePath().normalize();
        try {
            return portablePath(absoluteProjectPath.relativize(absolutePath));
        } catch (IllegalArgumentException exception) {
            return portablePath(path.normalize());
        }
    }

    private static String metadataValue(FlowGraph graph, String key) {
        Object value = graph.metadata().get(key);
        return value == null ? "" : value.toString();
    }

    private static String escapeCommandArgument(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeText(String value) {
        return value.replace("\\", "\\\\");
    }

    private static String escapeInline(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("`", "\\`");
    }

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }
}
