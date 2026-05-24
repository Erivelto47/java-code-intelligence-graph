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
        assertTrue(errorBytes.toString(StandardCharsets.UTF_8).contains("--entrypoint or --endpoint"));
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
        assertTrue(handoff.contains(portablePath(projectDirectory.resolve("src/main/java/com/company/FooService.java"))));
        assertTrue(handoff.contains(portablePath(expectedOutputDirectory.resolve("flow.json"))));
        assertTrue(handoff.contains(portablePath(expectedOutputDirectory.resolve("flow.md"))));
        assertTrue(handoff.contains(portablePath(expectedOutputDirectory.resolve("flow.mmd"))));
        assertTrue(handoff.contains(portablePath(expectedOutputDirectory.resolve("context-pack.md"))));

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
        assertTrue(flowsIndex.contains("Agent handoff"));
        assertTrue(flowsIndex.contains("com.company.FooService.processOrder"));
        assertTrue(flowsIndex.contains(".code-atlas/flows/com/company/FooService/processOrder"));
        assertTrue(flowsIndex.contains(".code-atlas/flows/com/company/FooService/processOrder/agent-handoff.md"));
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

    @Test
    void explicitAnalyzeFlowSubcommandKeepsEntrypointSupport() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path outputDirectory = tempDir.resolve("explicit-output");
        writeJavaFile(projectDirectory.resolve("src/main/java/com/company/FooService.java"));

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.FooService.processOrder",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        JsonNode flow = OBJECT_MAPPER.readTree(outputDirectory.resolve("flow.json").toFile());
        assertEquals("com.company.FooService.processOrder", flow.get("entrypoint").asText());
    }

    @Test
    void listEntrypointsGeneratesEntrypointsJson() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeSpringController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "list-entrypoints",
                        "--project", projectDirectory.toString()
                },
                outputStream,
                errorStream
        );

        assertEquals(0, exitCode);
        Path entrypointsJson = projectDirectory.resolve(".code-atlas/entrypoints.json");
        assertTrue(Files.isRegularFile(entrypointsJson));
        assertTrue(outputBytes.toString(StandardCharsets.UTF_8)
                .contains("POST /auth/register -> com.company.AuthController.register"));
        JsonNode entrypoint = OBJECT_MAPPER.readTree(entrypointsJson.toFile()).get("entrypoints").get(0);
        assertEquals("HTTP_ENDPOINT", entrypoint.get("kind").asText());
        assertEquals("POST", entrypoint.get("httpMethod").asText());
        assertEquals("/auth/register", entrypoint.get("path").asText());
        assertEquals("com.company.AuthController.register", entrypoint.get("javaEntrypoint").asText());
    }

    @Test
    void analyzeFlowCanResolveSpringEndpointToJavaEntrypoint() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeSpringController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));
        Path expectedOutputDirectory = projectDirectory.resolve(
                ".code-atlas/flows/com/company/AuthController/register"
        );

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--endpoint", "POST /auth/register"
                }
        );

        assertEquals(0, exitCode);
        assertTrue(Files.isRegularFile(projectDirectory.resolve(".code-atlas/entrypoints.json")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("flow.json")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("flow.md")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("flow.mmd")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("context-pack.md")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("agent-handoff.md")));
        assertTrue(Files.isRegularFile(projectDirectory.resolve(".code-atlas/project-index.json")));
        assertTrue(Files.isRegularFile(projectDirectory.resolve(".code-atlas/flows-index.md")));

        JsonNode flow = OBJECT_MAPPER.readTree(expectedOutputDirectory.resolve("flow.json").toFile());
        assertEquals("com.company.AuthController.register", flow.get("entrypoint").asText());
    }

    @Test
    void analyzeFlowByEndpointNormalizesMethodAndPath() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path outputDirectory = tempDir.resolve("endpoint-output");
        writeSpringController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--endpoint", "post auth/register",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        JsonNode flow = OBJECT_MAPPER.readTree(outputDirectory.resolve("flow.json").toFile());
        assertEquals("com.company.AuthController.register", flow.get("entrypoint").asText());
    }

    @Test
    void analyzeFlowRejectsEntrypointAndEndpointTogether() {
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", tempDir.toString(),
                        "--entrypoint", "com.company.FooService.processOrder",
                        "--endpoint", "POST /auth/register"
                },
                errorStream
        );

        assertEquals(2, exitCode);
        assertTrue(errorBytes.toString(StandardCharsets.UTF_8)
                .contains("Use either --entrypoint or --endpoint, not both"));
    }

    @Test
    void analyzeFlowRejectsMalformedEndpoint() {
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", tempDir.toString(),
                        "--endpoint", "POST"
                },
                errorStream
        );

        assertEquals(2, exitCode);
        assertTrue(errorBytes.toString(StandardCharsets.UTF_8)
                .contains("Missing or invalid argument: --endpoint"));
    }

    @Test
    void analyzeFlowByEndpointFailsWhenEndpointIsUnknown() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeSpringController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--endpoint", "GET /auth/register"
                },
                errorStream
        );

        String errorOutput = errorBytes.toString(StandardCharsets.UTF_8);
        assertEquals(2, exitCode);
        assertTrue(errorOutput.contains("Endpoint not found: GET /auth/register"));
        assertTrue(errorOutput.contains("POST /auth/register -> com.company.AuthController.register"));
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

    private static void writeSpringController(Path sourceFile) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
                sourceFile,
                """
                        package com.company;

                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/auth")
                        public class AuthController {
                            @PostMapping("/register")
                            public void register() {
                            }
                        }
                        """
        );
    }

    private static String portablePath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
