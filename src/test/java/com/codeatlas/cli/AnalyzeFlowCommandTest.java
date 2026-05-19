package com.codeatlas.cli;

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
