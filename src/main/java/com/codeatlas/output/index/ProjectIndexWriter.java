package com.codeatlas.output.index;

import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.EntrypointKind;
import com.codeatlas.core.entrypoint.SourceLocation;
import com.codeatlas.core.project.ImplementationDescriptor;
import com.codeatlas.core.project.JavaTypeDescriptor;
import com.codeatlas.core.project.ProjectDescriptor;
import com.codeatlas.core.project.ProjectIndex;
import com.codeatlas.core.project.SpringBeanDescriptor;
import com.codeatlas.core.project.UnresolvedProjectSymbol;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProjectIndexWriter {
    private final ObjectMapper objectMapper;

    public ProjectIndexWriter() {
        this(defaultObjectMapper());
    }

    ProjectIndexWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Path write(ProjectIndex index, Path projectPath) throws IOException {
        Objects.requireNonNull(index, "index must not be null");
        Objects.requireNonNull(projectPath, "projectPath must not be null");

        Path codeAtlasDirectory = projectPath.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Path outputFile = codeAtlasDirectory.resolve("project-index.json");
        objectMapper.writeValue(outputFile.toFile(), ProjectIndexDocument.from(index));
        return outputFile;
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private record ProjectIndexDocument(
            String schemaVersion,
            Instant generatedAt,
            ProjectDescriptor project,
            String language,
            List<String> frameworks,
            List<String> sourceRoots,
            List<JavaTypeDescriptor> classes,
            List<JavaTypeDescriptor> interfaces,
            List<ImplementationDocument> implementations,
            List<SpringBeanDescriptor> springBeans,
            List<String> controllers,
            List<String> repositories,
            List<String> clients,
            List<EntrypointDocument> entrypoints,
            List<UnresolvedProjectSymbol> unresolved,
            Map<String, Object> metadata
    ) {
        private static ProjectIndexDocument from(ProjectIndex index) {
            return new ProjectIndexDocument(
                    index.schemaVersion(),
                    index.generatedAt(),
                    index.project(),
                    index.language(),
                    index.frameworks(),
                    index.sourceRoots(),
                    index.classes(),
                    index.interfaces(),
                    index.implementations().stream().map(ImplementationDocument::from).toList(),
                    index.springBeans(),
                    index.controllers(),
                    index.repositories(),
                    index.clients(),
                    index.entrypoints().stream().map(EntrypointDocument::from).toList(),
                    index.unresolved(),
                    index.metadata()
            );
        }
    }

    private record ImplementationDocument(
            @JsonProperty("interface")
            String interfaceName,
            List<String> implementations
    ) {
        private static ImplementationDocument from(ImplementationDescriptor implementation) {
            return new ImplementationDocument(implementation.interfaceName(), implementation.implementations());
        }
    }

    private record EntrypointDocument(
            String id,
            EntrypointKind kind,
            String httpMethod,
            String path,
            String javaEntrypoint,
            String controllerClass,
            String className,
            String methodName,
            String sourceFile,
            SourceLocation sourceLocation,
            List<String> annotations
    ) {
        private static EntrypointDocument from(EntrypointDescriptor entrypoint) {
            SourceLocation sourceLocation = entrypoint.sourceLocation();
            return new EntrypointDocument(
                    entrypoint.id(),
                    entrypoint.kind(),
                    entrypoint.httpMethod(),
                    entrypoint.path(),
                    entrypoint.javaEntrypoint(),
                    entrypoint.className(),
                    entrypoint.className(),
                    entrypoint.methodName(),
                    sourceLocation == null ? null : sourceLocation.file(),
                    sourceLocation,
                    flattenedAnnotations(entrypoint)
            );
        }

        private static List<String> flattenedAnnotations(EntrypointDescriptor entrypoint) {
            if (entrypoint.annotations() == null) {
                return List.of();
            }
            return java.util.stream.Stream.concat(
                            entrypoint.annotations().classLevel().stream(),
                            entrypoint.annotations().methodLevel().stream()
                    )
                    .map(EntrypointDocument::annotationName)
                    .distinct()
                    .toList();
        }

        private static String annotationName(String annotationSource) {
            String value = annotationSource == null ? "" : annotationSource.trim();
            if (value.startsWith("@")) {
                value = value.substring(1);
            }
            int end = 0;
            while (end < value.length()
                    && (Character.isJavaIdentifierPart(value.charAt(end)) || value.charAt(end) == '.')) {
                end++;
            }
            String qualifiedName = end == 0 ? value : value.substring(0, end);
            int separator = qualifiedName.lastIndexOf('.');
            return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
        }
    }
}
