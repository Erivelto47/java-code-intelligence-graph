package com.codeatlas.output.json;

import com.codeatlas.core.entrypoint.EntrypointIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        objectMapper.writeValue(outputFile.toFile(), index);
        return outputFile;
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
