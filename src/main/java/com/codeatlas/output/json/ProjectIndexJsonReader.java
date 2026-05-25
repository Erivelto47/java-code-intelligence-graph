package com.codeatlas.output.json;

import com.codeatlas.core.entrypoint.SourceLocation;
import com.codeatlas.core.project.ImplementationDescriptor;
import com.codeatlas.core.project.ProjectDescriptor;
import com.codeatlas.core.project.ProjectIndex;
import com.codeatlas.core.project.SpringBeanDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ProjectIndexJsonReader {
    private static final String STALE_REASON = "project-index.json is older than at least one Java source file";

    private final ObjectMapper objectMapper;

    public ProjectIndexJsonReader() {
        this(defaultObjectMapper());
    }

    ProjectIndexJsonReader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Optional<ProjectIndex> read(Path projectPath) {
        return readResult(projectPath).index();
    }

    public ReadResult readResult(Path projectPath) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");

        Path projectIndexJson = projectPath.resolve(".code-atlas/project-index.json");
        if (!Files.isRegularFile(projectIndexJson)) {
            return ReadResult.missing("project-index.json not found");
        }

        StaleCheck staleCheck = staleCheck(projectPath, projectIndexJson);
        try {
            JsonNode root = objectMapper.readTree(projectIndexJson.toFile());
            String projectRoot = projectPath.toAbsolutePath().normalize().toString().replace('\\', '/');
            JsonNode projectNode = root.get("project");
            if (projectNode != null && projectNode.isObject()) {
                projectRoot = text(projectNode.get("root"), projectRoot);
            }

            ProjectIndex index = new ProjectIndex(
                    text(root.get("schemaVersion"), "1.0"),
                    instant(root.get("generatedAt")),
                    new ProjectDescriptor(projectRoot),
                    text(root.get("language"), "Java"),
                    readStringArray(root.get("frameworks")),
                    readStringArray(root.get("sourceRoots")),
                    List.of(),
                    List.of(),
                    readImplementations(root.get("implementations")),
                    readSpringBeans(root.get("springBeans")),
                    readStringArray(root.get("controllers")),
                    readStringArray(root.get("repositories")),
                    readStringArray(root.get("clients")),
                    List.of(),
                    List.of(),
                    Map.of("source", "project-index-json")
            );
            return ReadResult.loaded(index, staleCheck);
        } catch (IOException | RuntimeException exception) {
            List<String> diagnostics = new ArrayList<>();
            diagnostics.add("Failed to read project-index.json: " + message(exception));
            diagnostics.addAll(staleCheck.diagnostics());
            return ReadResult.invalid(diagnostics, staleCheck.staleSuspected(), staleCheck.staleReasons());
        }
    }

    public enum ReadStatus {
        LOADED,
        MISSING,
        INVALID
    }

    public record ReadResult(
            Optional<ProjectIndex> index,
            ReadStatus status,
            List<String> diagnostics,
            boolean staleSuspected,
            List<String> staleReasons
    ) {
        public ReadResult {
            index = index == null ? Optional.empty() : index;
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
            staleReasons = staleReasons == null ? List.of() : List.copyOf(staleReasons);
        }

        private static ReadResult loaded(ProjectIndex index, StaleCheck staleCheck) {
            return new ReadResult(
                    Optional.of(index),
                    ReadStatus.LOADED,
                    staleCheck.diagnostics(),
                    staleCheck.staleSuspected(),
                    staleCheck.staleReasons()
            );
        }

        private static ReadResult missing(String diagnostic) {
            return new ReadResult(Optional.empty(), ReadStatus.MISSING, List.of(diagnostic), false, List.of());
        }

        private static ReadResult invalid(
                List<String> diagnostics,
                boolean staleSuspected,
                List<String> staleReasons
        ) {
            return new ReadResult(Optional.empty(), ReadStatus.INVALID, diagnostics, staleSuspected, staleReasons);
        }
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private static List<ImplementationDescriptor> readImplementations(JsonNode implementationsNode) {
        if (implementationsNode == null || !implementationsNode.isArray()) {
            return List.of();
        }
        List<ImplementationDescriptor> implementations = new ArrayList<>();
        for (JsonNode implementationNode : implementationsNode) {
            implementations.add(new ImplementationDescriptor(
                    text(implementationNode.get("interface"), ""),
                    readStringArray(implementationNode.get("implementations"))
            ));
        }
        return List.copyOf(implementations);
    }

    private static List<SpringBeanDescriptor> readSpringBeans(JsonNode springBeansNode) {
        if (springBeansNode == null || !springBeansNode.isArray()) {
            return List.of();
        }
        List<SpringBeanDescriptor> springBeans = new ArrayList<>();
        for (JsonNode springBeanNode : springBeansNode) {
            springBeans.add(new SpringBeanDescriptor(
                    text(springBeanNode.get("id"), ""),
                    text(springBeanNode.get("kind"), ""),
                    text(springBeanNode.get("beanType"), ""),
                    readStringArray(springBeanNode.get("annotations")),
                    text(springBeanNode.get("sourceFile"), ""),
                    readSourceLocation(springBeanNode)
            ));
        }
        return List.copyOf(springBeans);
    }

    private static List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            values.add(value.asText());
        }
        return List.copyOf(values);
    }

    private static SourceLocation readSourceLocation(JsonNode node) {
        JsonNode sourceLocation = node.get("sourceLocation");
        if (sourceLocation != null && sourceLocation.isObject()) {
            return new SourceLocation(
                    text(sourceLocation.get("file"), text(node.get("sourceFile"), "")),
                    sourceLocation.has("line") ? sourceLocation.get("line").asInt() : 0
            );
        }
        String sourceFile = text(node.get("sourceFile"), "");
        return sourceFile.isBlank() ? null : new SourceLocation(sourceFile, 0);
    }

    private static String text(JsonNode node, String fallback) {
        return node == null || node.isNull() ? fallback : node.asText();
    }

    private static Instant instant(JsonNode node) {
        if (node == null || node.isNull()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(node.asText());
        } catch (DateTimeParseException exception) {
            return Instant.EPOCH;
        }
    }

    private static StaleCheck staleCheck(Path projectPath, Path projectIndexJson) {
        Path sourceRoot = projectPath.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) {
            return StaleCheck.current();
        }

        try {
            FileTime projectIndexModifiedAt = Files.getLastModifiedTime(projectIndexJson);
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                for (Path sourceFile : paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .toList()) {
                    if (Files.getLastModifiedTime(sourceFile).compareTo(projectIndexModifiedAt) > 0) {
                        return StaleCheck.stale();
                    }
                }
            }
            return StaleCheck.current();
        } catch (IOException | RuntimeException exception) {
            return StaleCheck.unknown("Failed to evaluate project-index staleness: " + message(exception));
        }
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record StaleCheck(
            boolean staleSuspected,
            List<String> staleReasons,
            List<String> diagnostics
    ) {
        private static StaleCheck current() {
            return new StaleCheck(false, List.of(), List.of());
        }

        private static StaleCheck stale() {
            return new StaleCheck(true, List.of(STALE_REASON), List.of(STALE_REASON));
        }

        private static StaleCheck unknown(String diagnostic) {
            return new StaleCheck(false, List.of(), List.of(diagnostic));
        }
    }
}
