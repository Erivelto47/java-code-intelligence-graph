package com.codeatlas.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzeFlowCommandTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void failsWhenEntrypointIsMissing() {
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{"--project", tempDir.toString()},
                errorStream
        );

        assertEquals(2, exitCode);
        assertTrue(errorBytes.toString(StandardCharsets.UTF_8).contains("--entrypoint"));
    }

    @Test
    void generatesFilesUnderProjectWhenOutputIsOmitted() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeJavaFile(projectDirectory.resolve("src/main/java/com/company/FooService.java"));
        Path expectedOutputDirectory = projectDirectory.resolve(
                ".code-atlas/flows/com/company/FooService/processOrder"
        );

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.FooService.processOrder"
                }
        );

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("flow.json")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("flow.md")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("flow.mmd")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("context-pack.md")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("agent-handoff.md")));
        assertTrue(Files.isRegularFile(projectDirectory.resolve(".code-atlas/project-index.json")));
        assertTrue(Files.isRegularFile(projectDirectory.resolve(".code-atlas/flows-index.md")));

        String handoff = Files.readString(expectedOutputDirectory.resolve("agent-handoff.md"));
        assertTrue(handoff.contains("Agent Handoff"));
        assertTrue(handoff.contains("com.company.FooService.processOrder"));
        assertTrue(handoff.contains("src/main/java/com/company/FooService.java"));
        assertTrue(handoff.contains("Node count: `2`"));
        assertTrue(handoff.contains("Edge count: `1`"));
        assertTrue(handoff.contains(".code-atlas/flows/com/company/FooService/processOrder/flow.json"));
        assertTrue(handoff.contains(".code-atlas/flows/com/company/FooService/processOrder/context-pack.md"));
        assertTrue(handoff.contains(".code-atlas/flows/com/company/FooService/processOrder/agent-handoff.md"));

        JsonNode projectIndex = OBJECT_MAPPER.readTree(projectDirectory.resolve(".code-atlas/project-index.json").toFile());
        JsonNode indexedFlow = projectIndex.get("flows").get(0);
        assertEquals("1.0", projectIndex.get("schemaVersion").asText());
        assertEquals(projectDirectory.toString().replace('\\', '/'), projectIndex.get("project").get("root").asText());
        assertEquals("com.company.FooService.processOrder", indexedFlow.get("entrypoint").asText());
        assertEquals(
                ".code-atlas/flows/com/company/FooService/processOrder/flow.json",
                indexedFlow.get("artifacts").get("flowJson").asText()
        );
        assertEquals(
                ".code-atlas/flows/com/company/FooService/processOrder/context-pack.md",
                indexedFlow.get("artifacts").get("contextPack").asText()
        );
        assertEquals(
                ".code-atlas/flows/com/company/FooService/processOrder/agent-handoff.md",
                indexedFlow.get("artifacts").get("agentHandoff").asText()
        );

        String flowsIndex = Files.readString(projectDirectory.resolve(".code-atlas/flows-index.md"));
        assertTrue(flowsIndex.contains("com.company.FooService.processOrder"));
        assertTrue(flowsIndex.contains(".code-atlas/flows/com/company/FooService/processOrder"));
        assertTrue(flowsIndex.contains("context-pack.md"));
        assertTrue(flowsIndex.contains("flow.json"));
    }

    @Test
    void generatesExpectedFilesWithStubAnalyzer() throws Exception {
        Path projectDirectory = Files.createDirectory(tempDir.resolve("project"));
        Path outputDirectory = tempDir.resolve("output");

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.FooService.method",
                        "--output", outputDirectory.toString(),
                        "--stub"
                }
        );

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(outputDirectory.resolve("flow.json")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("flow.md")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("flow.mmd")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("context-pack.md")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("agent-handoff.md")));
    }

    @Test
    void generatesExpectedFilesWithSourceTextAnalyzer() {
        Path outputDirectory = tempDir.resolve("source-output");

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "--project", Path.of("examples/java-simple").toString(),
                        "--entrypoint", "com.company.FooService.processOrder",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(outputDirectory.resolve("flow.json")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("flow.md")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("flow.mmd")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("context-pack.md")));
        assertTrue(Files.isRegularFile(outputDirectory.resolve("agent-handoff.md")));
    }

    private static void writeJavaFile(Path sourceFile) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
                sourceFile,
                """
                        package com.company;

                        public class FooService {
                            public void processOrder() {
                            }
                        }
                        """
        );
    }
}
