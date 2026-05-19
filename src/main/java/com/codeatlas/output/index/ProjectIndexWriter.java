package com.codeatlas.output.index;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ProjectIndexWriter {
    private static final String INDEX_SCHEMA_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    public ProjectIndexWriter() {
        this(defaultObjectMapper());
    }

    ProjectIndexWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Path write(FlowGraph graph, Path projectPath, Path outputDirectory) throws IOException {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Path codeAtlasDirectory = projectPath.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Path outputFile = codeAtlasDirectory.resolve("project-index.json");
        objectMapper.writeValue(outputFile.toFile(), projectIndex(graph, projectPath, outputDirectory));
        return outputFile;
    }

    private static ProjectIndex projectIndex(FlowGraph graph, Path projectPath, Path outputDirectory) {
        String flowPath = projectRelativePath(projectPath, outputDirectory);
        Artifacts artifacts = new Artifacts(
                artifactPath(flowPath, "flow.json"),
                artifactPath(flowPath, "flow.md"),
                artifactPath(flowPath, "flow.mmd"),
                artifactPath(flowPath, "context-pack.md"),
                artifactPath(flowPath, "agent-handoff.md")
        );
        Flow flow = new Flow(
                graph.entrypoint(),
                flowPath,
                sourceFiles(graph),
                artifacts
        );
        return new ProjectIndex(
                INDEX_SCHEMA_VERSION,
                new Project(portablePath(projectPath.normalize())),
                graph.generatedAt(),
                List.of(flow)
        );
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

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private record ProjectIndex(
            String schemaVersion,
            Project project,
            Instant generatedAt,
            List<Flow> flows
    ) {
    }

    private record Project(String root) {
    }

    private record Flow(
            String entrypoint,
            String flowPath,
            List<String> sourceFiles,
            Artifacts artifacts
    ) {
    }

    private record Artifacts(
            String flowJson,
            String flowMarkdown,
            String flowMermaid,
            String contextPack,
            String agentHandoff
    ) {
    }
}
