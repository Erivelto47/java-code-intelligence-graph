package com.codeatlas.output.decision.json;

import com.codeatlas.core.decision.DecisionTrace;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DecisionTraceJsonWriter {
    private static final String LINE_FEED = "\n";
    private static final DefaultIndenter CANONICAL_INDENTER = new DefaultIndenter("  ", LINE_FEED);

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
        Files.writeString(outputFile, canonicalJson(trace), StandardCharsets.UTF_8);
        return outputFile;
    }

    private String canonicalJson(DecisionTrace trace) throws IOException {
        String json = objectMapper
                .writer(canonicalPrettyPrinter())
                .writeValueAsString(trace);
        if (json.endsWith(LINE_FEED)) {
            return json;
        }
        return json + LINE_FEED;
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private static DefaultPrettyPrinter canonicalPrettyPrinter() {
        Separators separators = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator("");
        return new DefaultPrettyPrinter()
                .withObjectIndenter(CANONICAL_INDENTER)
                .withArrayIndenter(CANONICAL_INDENTER)
                .withSeparators(separators);
    }
}
