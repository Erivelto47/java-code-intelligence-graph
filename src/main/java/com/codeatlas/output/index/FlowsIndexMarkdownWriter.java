package com.codeatlas.output.index;

import com.codeatlas.core.model.FlowGraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FlowsIndexMarkdownWriter {
    public Path write(FlowGraph graph, Path projectPath, Path outputDirectory) throws IOException {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Path codeAtlasDirectory = projectPath.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Path outputFile = codeAtlasDirectory.resolve("flows-index.md");
        Files.writeString(outputFile, render(graph, projectPath, outputDirectory), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static String render(FlowGraph graph, Path projectPath, Path outputDirectory) {
        String flowPath = projectRelativePath(projectPath, outputDirectory);
        String contextPackPath = artifactPath(flowPath, "context-pack.md");
        String flowJsonPath = artifactPath(flowPath, "flow.json");

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Code Atlas Flows Index\n\n");
        markdown.append("## Project\n\n");
        markdown.append("`").append(escapeInline(portablePath(projectPath.normalize()))).append("`\n\n");
        markdown.append("## Flows\n\n");
        String agentHandoffPath = artifactPath(flowPath, "agent-handoff.md");
        markdown.append("| Entrypoint | Flow path | Agent handoff | Context pack | Flow JSON |\n");
        markdown.append("| --- | --- | --- | --- | --- |\n");
        markdown.append("| `")
                .append(escapeTable(graph.entrypoint()))
                .append("` | `")
                .append(escapeTable(flowPath))
                .append("` | `")
                .append(escapeTable(agentHandoffPath))
                .append("` | `")
                .append(escapeTable(contextPackPath))
                .append("` | `")
                .append(escapeTable(flowJsonPath))
                .append("` |\n\n");
        markdown.append("## Notes\n\n");
        markdown.append("This index helps agents find generated flow artifacts without traversing the repository tree.\n");
        return markdown.toString();
    }

    private static String artifactPath(String flowPath, String fileName) {
        if (flowPath.isBlank()) {
            return fileName;
        }
        return flowPath + "/" + fileName;
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

    private static String escapeTable(String value) {
        return escapeInline(value).replace("|", "\\|");
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
