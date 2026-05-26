package com.codeatlas.output.decision.json;

import com.codeatlas.core.decision.DecisionTrace;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DecisionTraceJsonWriter {
    private final ObjectMapper objectMapper;

    public DecisionTraceJsonWriter() {
        this(defaultObjectMapper());
    }

    DecisionTraceJsonWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Path write(DecisionTrace trace, Path outputDirectory) throws IOException {
        Objects.requireNonNull(trace, "trace must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");

        Files.createDirectories(outputDirectory);
        Path outputFile = outputDirectory.resolve("decisions.json");
        objectMapper.writeValue(outputFile.toFile(), trace);
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
