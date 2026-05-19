package com.codeatlas.output.json;

import com.codeatlas.core.analyzer.StubFlowAnalyzer;
import com.codeatlas.core.model.FlowGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFlowWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesFlowJson() throws IOException {
        FlowGraph graph = new StubFlowAnalyzer().analyze(Path.of("."), "com.company.FooService.method");

        Path outputFile = new JsonFlowWriter().write(graph, tempDir);

        assertEquals(tempDir.resolve("flow.json"), outputFile);
        assertTrue(Files.isRegularFile(outputFile));
        String json = Files.readString(outputFile);
        assertTrue(json.contains("\"entrypoint\" : \"com.company.FooService.method\""));
        assertTrue(json.contains("\"id\" : \"method:com.company.FooService.method\""));
    }
}
