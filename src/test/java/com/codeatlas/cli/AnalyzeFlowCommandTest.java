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
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals("1.0", projectIndex.get("schemaVersion").asText());
        assertEquals(projectDirectory.toString().replace('\\', '/'), projectIndex.get("project").get("root").asText());
        assertEquals("Java", projectIndex.get("language").asText());
        assertEquals("src/main/java", projectIndex.get("sourceRoots").get(0).asText());
        JsonNode indexedClass = projectIndex.get("classes").get(0);
        assertEquals("com.company.FooService", indexedClass.get("fullyQualifiedName").asText());
        assertEquals("processOrder", indexedClass.get("methods").get(0).get("name").asText());
        assertEquals("processOrder()", indexedClass.get("methods").get(0).get("signature").asText());
        assertEquals(0, projectIndex.get("entrypoints").size());
        assertTrue(Files.isRegularFile(projectDirectory.resolve(".code-atlas/entrypoints.json")));

        JsonNode flow = OBJECT_MAPPER.readTree(expectedOutputDirectory.resolve("flow.json").toFile());
        assertTrue(flow.get("metadata").get("projectIndexAssisted").asBoolean());
        assertEquals("memory", flow.get("metadata").get("projectIndexSource").asText());

        String flowsIndex = Files.readString(projectDirectory.resolve(".code-atlas/flows-index.md"));
        assertTrue(flowsIndex.contains("# Code Atlas Flows Index"));
        assertTrue(flowsIndex.contains("## HTTP Endpoints"));
        assertTrue(flowsIndex.contains("| Method | Path | Java Entrypoint | Source |"));
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
    void analyzeDecisionsGeneratesSeparateDecisionArtifacts() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeJavaFile(
                projectDirectory.resolve("src/main/java/com/example/UserService.java"),
                """
                        package com.example;

                        public class UserService {
                            public void create(CreateUserRequest request) {
                                if (request.name() == null || request.name().isBlank()) {
                                    throw new IllegalArgumentException("Name is required");
                                }
                            }
                        }
                        """
        );
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);
        Path expectedOutputDirectory = projectDirectory.resolve(
                ".code-atlas/decisions/com/example/UserService/create"
        );

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-decisions",
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.example.UserService.create"
                },
                outputStream,
                errorStream
        );

        assertEquals(0, exitCode);
        assertEquals("", errorBytes.toString(StandardCharsets.UTF_8));
        assertTrue(outputBytes.toString(StandardCharsets.UTF_8).contains("Analyzed decisions: 1"));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("decisions.json")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("decisions.md")));
        assertTrue(Files.isRegularFile(expectedOutputDirectory.resolve("decisions.mmd")));
        assertFalse(Files.exists(projectDirectory.resolve(".code-atlas/flows")));

        JsonNode decisions = OBJECT_MAPPER.readTree(expectedOutputDirectory.resolve("decisions.json").toFile());
        JsonNode decision = decisions.get("decisions").get(0);
        assertEquals("1.0", decisions.get("schemaVersion").asText());
        assertEquals("com.example.UserService.create", decisions.get("scope").get("entrypoint").asText());
        assertEquals("CONDITIONAL_THROW", decision.get("kind").asText());
        assertEquals("VALIDATION", decision.get("category").asText());
        assertEquals("com.example.UserService", decision.get("source").get("className").asText());
        assertEquals("create", decision.get("source").get("methodName").asText());
        assertEquals("src/main/java/com/example/UserService.java", decision.get("sourceLocation").get("file").asText());
        assertEquals(5, decision.get("sourceLocation").get("line").asInt());
        assertEquals("request.name() == null || request.name().isBlank()", decision.get("expression").get("text").asText());
        assertEquals("IllegalArgumentException", decision.get("outcomes").get(0).get("exceptionType").asText());
        assertEquals("Name is required", decision.get("outcomes").get(0).get("message").asText());

        String markdown = Files.readString(expectedOutputDirectory.resolve("decisions.md"));
        assertTrue(markdown.contains("# Decision Trace"));
        assertTrue(markdown.contains("throws IllegalArgumentException(\"Name is required\")"));
        assertTrue(markdown.contains("src/main/java/com/example/UserService.java:5"));

        String mermaid = Files.readString(expectedOutputDirectory.resolve("decisions.mmd"));
        assertTrue(mermaid.contains("flowchart TD"));
        assertTrue(mermaid.contains("request.name() == null || request.name().isBlank()"));
        assertTrue(mermaid.contains("throws IllegalArgumentException: Name is required"));
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
    void listEndpointsGeneratesEntrypointsJsonAndReadableConsoleOutput() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeEndpointCatalogController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "list-endpoints",
                        "--project", projectDirectory.toString()
                },
                outputStream,
                errorStream
        );

        assertEquals(0, exitCode);
        assertEquals("", errorBytes.toString(StandardCharsets.UTF_8));

        String output = outputBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Discovered endpoints: 3"));
        assertTrue(output.contains("POST /auth/register -> com.company.AuthController.register"));
        assertTrue(output.contains("GET /auth/status -> com.company.AuthController.status"));
        assertTrue(output.contains("POST /auth/login -> com.company.AuthController.login"));

        Path entrypointsJson = projectDirectory.resolve(".code-atlas/entrypoints.json");
        assertTrue(Files.isRegularFile(entrypointsJson));
        JsonNode root = OBJECT_MAPPER.readTree(entrypointsJson.toFile());
        assertEquals("1.0", root.get("schemaVersion").asText());
        assertTrue(root.hasNonNull("generatedAt"));
        assertEquals(
                projectDirectory.toAbsolutePath().normalize().toString().replace('\\', '/'),
                root.get("project").asText()
        );
        assertEquals(3, root.get("entrypoints").size());

        JsonNode register = endpoint(root.get("entrypoints"), "POST", "/auth/register");
        assertEquals("com.company.AuthController.register", register.get("javaEntrypoint").asText());
        assertEquals("com.company.AuthController", register.get("controllerClass").asText());
        assertEquals("com.company.AuthController", register.get("className").asText());
        assertEquals("register", register.get("methodName").asText());
        assertEquals("src/main/java/com/company/AuthController.java", register.get("sourceFile").asText());
        assertEquals("RestController", register.get("annotations").get(0).asText());
        assertEquals("RequestMapping", register.get("annotations").get(1).asText());
        assertEquals("PostMapping", register.get("annotations").get(2).asText());
    }

    @Test
    void indexProjectGeneratesPhase3Artifacts() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeSpringController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));
        writeJavaFile(
                projectDirectory.resolve("src/main/java/com/company/UserRegistrationInternal.java"),
                """
                        package com.company;

                        public interface UserRegistrationInternal {
                            void register();
                        }
                        """
        );
        writeJavaFile(
                projectDirectory.resolve("src/main/java/com/company/UserServiceImpl.java"),
                """
                        package com.company;

                        import org.springframework.stereotype.Service;

                        @Service
                        public class UserServiceImpl implements UserRegistrationInternal {
                            public void register() {
                            }
                        }
                        """
        );
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "index-project",
                        "--project", projectDirectory.toString()
                },
                outputStream,
                errorStream
        );

        assertEquals(0, exitCode);
        assertEquals("", errorBytes.toString(StandardCharsets.UTF_8));
        assertTrue(outputBytes.toString(StandardCharsets.UTF_8).contains("HTTP endpoints: 1"));

        JsonNode projectIndex = OBJECT_MAPPER.readTree(projectDirectory.resolve(".code-atlas/project-index.json").toFile());
        assertEquals("Java", projectIndex.get("language").asText());
        assertEquals("Spring", projectIndex.get("frameworks").get(0).asText());
        assertEquals("src/main/java", projectIndex.get("sourceRoots").get(0).asText());
        assertEquals("com.company.UserRegistrationInternal", projectIndex.get("interfaces").get(0).get("fullyQualifiedName").asText());
        assertEquals(
                "com.company.UserRegistrationInternal",
                projectIndex.get("implementations").get(0).get("interface").asText()
        );
        assertEquals(
                "com.company.UserServiceImpl",
                projectIndex.get("implementations").get(0).get("implementations").get(0).asText()
        );
        assertEquals("com.company.AuthController", projectIndex.get("controllers").get(0).asText());
        assertEquals("REST_CONTROLLER", projectIndex.get("springBeans").get(0).get("kind").asText());
        assertEquals("POST", projectIndex.get("entrypoints").get(0).get("httpMethod").asText());

        JsonNode entrypoints = OBJECT_MAPPER.readTree(projectDirectory.resolve(".code-atlas/entrypoints.json").toFile());
        assertEquals(1, entrypoints.get("entrypoints").size());
        assertEquals("/auth/register", entrypoints.get("entrypoints").get(0).get("path").asText());

        String flowsIndex = Files.readString(projectDirectory.resolve(".code-atlas/flows-index.md"));
        assertTrue(flowsIndex.contains("| POST | /auth/register | `com.company.AuthController.register`"));
    }

    @Test
    void listEndpointsGeneratesEmptyEntrypointsJsonForProjectWithoutEndpoints() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writeJavaFile(projectDirectory.resolve("src/main/java/com/company/FooService.java"));
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
        PrintStream errorStream = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "list-endpoints",
                        "--project", projectDirectory.toString()
                },
                outputStream,
                errorStream
        );

        assertEquals(0, exitCode);
        assertEquals("", errorBytes.toString(StandardCharsets.UTF_8));
        String output = outputBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Discovered endpoints: 0"));
        assertTrue(output.contains("No Spring HTTP endpoints were discovered."));

        JsonNode root = OBJECT_MAPPER.readTree(projectDirectory.resolve(".code-atlas/entrypoints.json").toFile());
        assertEquals(0, root.get("entrypoints").size());
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
        assertEquals(0, flow.get("unresolved").size());
    }

    @Test
    void analyzeFlowCanUseExistingProjectIndexForInterfaceResolution() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path outputDirectory = tempDir.resolve("project-index-assisted-output");
        writeJavaFile(
                projectDirectory.resolve("src/main/java/com/company/RegistrationController.java"),
                """
                        package com.company;

                        public class RegistrationController {
                            private final RegistrationUseCase registrationUseCase;

                            public RegistrationController(RegistrationUseCase registrationUseCase) {
                                this.registrationUseCase = registrationUseCase;
                            }

                            public void register() {
                                registrationUseCase.create();
                            }
                        }

                        interface RegistrationUseCase {
                            void create();
                        }

                        class RegistrationService {
                            public void create() {
                                validate();
                            }

                            private void validate() {
                            }
                        }
                        """
        );
        Path codeAtlasDirectory = projectDirectory.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Files.writeString(
                codeAtlasDirectory.resolve("project-index.json"),
                """
                        {
                          "schemaVersion": "1.0",
                          "generatedAt": "1970-01-01T00:00:00Z",
                          "project": {
                            "root": "%s"
                          },
                          "language": "Java",
                          "frameworks": [],
                          "sourceRoots": ["src/main/java"],
                          "classes": [],
                          "interfaces": [],
                          "implementations": [
                            {
                              "interface": "com.company.RegistrationUseCase",
                              "implementations": ["com.company.RegistrationService"]
                            }
                          ],
                          "springBeans": [],
                          "controllers": [],
                          "repositories": [],
                          "clients": [],
                          "entrypoints": [],
                          "unresolved": [],
                          "metadata": {}
                        }
                        """.formatted(projectDirectory.toAbsolutePath().normalize().toString().replace('\\', '/'))
        );

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.RegistrationController.register",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        JsonNode flow = OBJECT_MAPPER.readTree(outputDirectory.resolve("flow.json").toFile());
        assertEquals("com.company.RegistrationController.register", flow.get("entrypoint").asText());
        assertTrue(flow.get("metadata").get("projectIndexAssisted").asBoolean());
        assertEquals("json", flow.get("metadata").get("projectIndexSource").asText());
        assertEquals(1, flow.get("metadata").get("projectIndexImplementations").asInt());
        assertEquals("LOADED_FROM_JSON", flow.get("metadata").get("projectIndexStatus").asText());
        assertEquals(0, flow.get("metadata").get("projectIndexDiagnostics").size());
        assertFalse(flow.get("metadata").get("projectIndexStaleSuspected").asBoolean());
        assertEquals(0, flow.get("metadata").get("projectIndexStaleReasons").size());
        assertEquals(0, flow.get("unresolved").size());
        assertTrue(hasResolution(
                flow,
                "method:com.company.RegistrationUseCase.create",
                "method:com.company.RegistrationService.create",
                "INTERFACE_SINGLE_IMPLEMENTATION"
        ));
    }

    @Test
    void analyzeFlowFallsBackToMemoryProjectIndexWhenProjectIndexJsonDoesNotExist() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path outputDirectory = tempDir.resolve("missing-project-index-output");
        writeResolvableRegistrationProject(projectDirectory);

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.RegistrationController.register",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        JsonNode flow = OBJECT_MAPPER.readTree(outputDirectory.resolve("flow.json").toFile());
        assertTrue(flow.get("metadata").get("projectIndexAssisted").asBoolean());
        assertEquals("memory", flow.get("metadata").get("projectIndexSource").asText());
        assertEquals(1, flow.get("metadata").get("projectIndexImplementations").asInt());
        assertEquals("FALLBACK_MEMORY_MISSING_JSON", flow.get("metadata").get("projectIndexStatus").asText());
        assertTrue(flowDiagnosticsContain(flow, "project-index.json not found"));
        assertTrue(flowDiagnosticsContain(flow, "Using in-memory ProjectIndex fallback"));
        assertFalse(flow.get("metadata").get("projectIndexStaleSuspected").asBoolean());
        assertEquals(0, flow.get("unresolved").size());
        assertTrue(hasResolution(
                flow,
                "method:com.company.RegistrationUseCase.create",
                "method:com.company.RegistrationService.create",
                "INTERFACE_SINGLE_IMPLEMENTATION"
        ));
    }

    @Test
    void analyzeFlowFallsBackToMemoryProjectIndexWhenProjectIndexJsonIsInvalid() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path outputDirectory = tempDir.resolve("invalid-project-index-output");
        writeResolvableRegistrationProject(projectDirectory);
        Path codeAtlasDirectory = projectDirectory.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Files.writeString(codeAtlasDirectory.resolve("project-index.json"), "{ invalid-json");

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.RegistrationController.register",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        JsonNode flow = OBJECT_MAPPER.readTree(outputDirectory.resolve("flow.json").toFile());
        assertTrue(flow.get("metadata").get("projectIndexAssisted").asBoolean());
        assertEquals("memory", flow.get("metadata").get("projectIndexSource").asText());
        assertEquals(1, flow.get("metadata").get("projectIndexImplementations").asInt());
        assertEquals("FALLBACK_MEMORY_INVALID_JSON", flow.get("metadata").get("projectIndexStatus").asText());
        assertTrue(flowDiagnosticsContain(flow, "Failed to read project-index.json:"));
        assertTrue(flowDiagnosticsContain(flow, "Using in-memory ProjectIndex fallback"));
        assertFalse(flow.get("metadata").get("projectIndexStaleSuspected").asBoolean());
        assertEquals(0, flow.get("unresolved").size());
        assertTrue(hasResolution(
                flow,
                "method:com.company.RegistrationUseCase.create",
                "method:com.company.RegistrationService.create",
                "INTERFACE_SINGLE_IMPLEMENTATION"
        ));
    }

    @Test
    void analyzeFlowMarksExistingProjectIndexAsStaleWhenJavaSourceIsNewer() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path outputDirectory = tempDir.resolve("stale-project-index-output");
        writeJavaFile(
                projectDirectory.resolve("src/main/java/com/company/RegistrationController.java"),
                """
                        package com.company;

                        public class RegistrationController {
                            private final RegistrationUseCase registrationUseCase;

                            public RegistrationController(RegistrationUseCase registrationUseCase) {
                                this.registrationUseCase = registrationUseCase;
                            }

                            public void register() {
                                registrationUseCase.create();
                            }
                        }

                        interface RegistrationUseCase {
                            void create();
                        }

                        class RegistrationService {
                            public void create() {
                            }
                        }
                        """
        );
        Path codeAtlasDirectory = projectDirectory.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Path projectIndexJson = codeAtlasDirectory.resolve("project-index.json");
        Files.writeString(
                projectIndexJson,
                """
                        {
                          "schemaVersion": "1.0",
                          "generatedAt": "1970-01-01T00:00:00Z",
                          "project": {
                            "root": "%s"
                          },
                          "language": "Java",
                          "frameworks": [],
                          "sourceRoots": ["src/main/java"],
                          "classes": [],
                          "interfaces": [],
                          "implementations": [
                            {
                              "interface": "com.company.RegistrationUseCase",
                              "implementations": ["com.company.RegistrationService"]
                            }
                          ],
                          "springBeans": [],
                          "controllers": [],
                          "repositories": [],
                          "clients": [],
                          "entrypoints": [],
                          "unresolved": [],
                          "metadata": {}
                        }
                        """.formatted(projectDirectory.toAbsolutePath().normalize().toString().replace('\\', '/'))
        );
        Files.setLastModifiedTime(projectIndexJson, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
        Files.setLastModifiedTime(
                projectDirectory.resolve("src/main/java/com/company/RegistrationController.java"),
                FileTime.from(Instant.parse("2020-01-01T00:00:10Z"))
        );

        int exitCode = new AnalyzeFlowCommand().run(
                new String[]{
                        "analyze-flow",
                        "--project", projectDirectory.toString(),
                        "--entrypoint", "com.company.RegistrationController.register",
                        "--output", outputDirectory.toString()
                }
        );

        assertEquals(0, exitCode);
        JsonNode flow = OBJECT_MAPPER.readTree(outputDirectory.resolve("flow.json").toFile());
        assertEquals("json", flow.get("metadata").get("projectIndexSource").asText());
        assertEquals("LOADED_FROM_JSON", flow.get("metadata").get("projectIndexStatus").asText());
        assertTrue(flow.get("metadata").get("projectIndexStaleSuspected").asBoolean());
        assertEquals(
                "project-index.json is older than at least one Java source file",
                flow.get("metadata").get("projectIndexStaleReasons").get(0).asText()
        );
        assertTrue(flowDiagnosticsContain(flow, "project-index.json is older than at least one Java source file"));
        assertEquals(0, flow.get("unresolved").size());
    }

    @Test
    void analyzeFlowByEndpointCanUseExistingEntrypointsJson() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        writePlainAuthController(projectDirectory.resolve("src/main/java/com/company/AuthController.java"));
        Path codeAtlasDirectory = projectDirectory.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Files.writeString(
                codeAtlasDirectory.resolve("entrypoints.json"),
                """
                        {
                          "schemaVersion": "1.0",
                          "generatedAt": "1970-01-01T00:00:00Z",
                          "project": "%s",
                          "entrypoints": [
                            {
                              "id": "http:POST:/auth/register -> com.company.AuthController.register",
                              "kind": "HTTP_ENDPOINT",
                              "httpMethod": "POST",
                              "path": "/auth/register",
                              "javaEntrypoint": "com.company.AuthController.register",
                              "controllerClass": "com.company.AuthController",
                              "className": "com.company.AuthController",
                              "methodName": "register",
                              "sourceFile": "src/main/java/com/company/AuthController.java",
                              "sourceLocation": {
                                "file": "src/main/java/com/company/AuthController.java",
                                "line": 4
                              },
                              "annotations": ["PostMapping"]
                            }
                          ],
                          "metadata": {}
                        }
                        """.formatted(projectDirectory.toAbsolutePath().normalize().toString().replace('\\', '/'))
        );
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
        writeJavaFile(
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

    private static void writeJavaFile(Path sourceFile, String source) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
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

    private static void writePlainAuthController(Path sourceFile) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
                sourceFile,
                """
                        package com.company;

                        public class AuthController {
                            public void register() {
                            }
                        }
                        """
        );
    }

    private static void writeResolvableRegistrationProject(Path projectDirectory) throws Exception {
        writeJavaFile(
                projectDirectory.resolve("src/main/java/com/company/RegistrationController.java"),
                """
                        package com.company;

                        public class RegistrationController {
                            private final RegistrationUseCase registrationUseCase;

                            public RegistrationController(RegistrationUseCase registrationUseCase) {
                                this.registrationUseCase = registrationUseCase;
                            }

                            public void register() {
                                registrationUseCase.create();
                            }
                        }

                        interface RegistrationUseCase {
                            void create();
                        }

                        class RegistrationService implements RegistrationUseCase {
                            public void create() {
                            }
                        }
                        """
        );
    }

    private static void writeEndpointCatalogController(Path sourceFile) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
                sourceFile,
                """
                        package com.company;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RequestMethod;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/auth")
                        public class AuthController {
                            @PostMapping("/register")
                            public void register() {
                            }

                            @GetMapping("/status")
                            public String status() {
                                return "ok";
                            }

                            @RequestMapping(value = "/login", method = RequestMethod.POST)
                            public void login() {
                            }
                        }
                        """
        );
    }

    private static JsonNode endpoint(JsonNode entrypoints, String httpMethod, String path) {
        for (JsonNode entrypoint : entrypoints) {
            if (entrypoint.get("httpMethod").asText().equals(httpMethod)
                    && entrypoint.get("path").asText().equals(path)) {
                return entrypoint;
            }
        }
        throw new AssertionError("Missing endpoint " + httpMethod + " " + path);
    }

    private static boolean hasResolution(JsonNode flow, String sourceNodeId, String targetNodeId, String kind) {
        for (JsonNode resolution : flow.get("resolutions")) {
            if (resolution.get("sourceNodeId").asText().equals(sourceNodeId)
                    && resolution.get("targetNodeId").asText().equals(targetNodeId)
                    && resolution.get("kind").asText().equals(kind)) {
                return true;
            }
        }
        return false;
    }

    private static boolean flowDiagnosticsContain(JsonNode flow, String expected) {
        for (JsonNode diagnostic : flow.get("metadata").get("projectIndexDiagnostics")) {
            if (diagnostic.asText().contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static String portablePath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
