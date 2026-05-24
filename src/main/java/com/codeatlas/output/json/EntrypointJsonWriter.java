package com.codeatlas.output.json;

import com.codeatlas.core.entrypoint.EntrypointAnnotations;
import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.EntrypointIndex;
import com.codeatlas.core.entrypoint.EntrypointKind;
import com.codeatlas.core.entrypoint.SourceLocation;
import com.fasterxml.jackson.annotation.JsonInclude;
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

public final class EntrypointJsonWriter {
    private final ObjectMapper objectMapper;

    public EntrypointJsonWriter() {
        this(defaultObjectMapper());
    }

    EntrypointJsonWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Path write(EntrypointIndex index, Path outputDirectory) throws IOException {
        Objects.requireNonNull(index, "index must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("entrypoints.json");
        objectMapper.writeValue(outputFile.toFile(), EntrypointIndexDocument.from(index));
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

    private record EntrypointIndexDocument(
            String schemaVersion,
            Instant generatedAt,
            String project,
            List<EntrypointDocument> entrypoints,
            Map<String, Object> metadata
    ) {
        private static EntrypointIndexDocument from(EntrypointIndex index) {
            return new EntrypointIndexDocument(
                    index.schemaVersion(),
                    index.generatedAt(),
                    index.project(),
                    index.entrypoints().stream()
                            .map(EntrypointDocument::from)
                            .toList(),
                    index.metadata()
            );
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
            EntrypointAnnotations annotations
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
                    entrypoint.annotations()
            );
        }
    }
}
