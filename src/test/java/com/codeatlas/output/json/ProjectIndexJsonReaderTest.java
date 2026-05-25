package com.codeatlas.output.json;

import com.codeatlas.core.project.ProjectIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectIndexJsonReaderTest {
    @TempDir
    Path tempDir;

    @Test
    void readsProjectIndexResolutionFields() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
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
                          "frameworks": ["Spring"],
                          "sourceRoots": ["src/main/java"],
                          "classes": [],
                          "interfaces": [],
                          "implementations": [
                            {
                              "interface": "com.company.UserRegistrationInternal",
                              "implementations": ["com.company.UserServiceImpl"]
                            }
                          ],
                          "springBeans": [
                            {
                              "id": "userServiceImpl",
                              "kind": "SERVICE",
                              "beanType": "com.company.UserServiceImpl",
                              "annotations": ["Service"],
                              "sourceFile": "src/main/java/com/company/UserServiceImpl.java",
                              "sourceLocation": {
                                "file": "src/main/java/com/company/UserServiceImpl.java",
                                "line": 5
                              }
                            }
                          ],
                          "controllers": [],
                          "repositories": ["com.company.UserRepository"],
                          "clients": ["com.company.NotificationClient"],
                          "entrypoints": [],
                          "unresolved": [],
                          "metadata": {}
                        }
                        """.formatted(projectDirectory.toAbsolutePath().normalize().toString().replace('\\', '/'))
        );

        Optional<ProjectIndex> index = new ProjectIndexJsonReader().read(projectDirectory);
        ProjectIndexJsonReader.ReadResult result = new ProjectIndexJsonReader().readResult(projectDirectory);

        assertTrue(index.isPresent());
        assertEquals(ProjectIndexJsonReader.ReadStatus.LOADED, result.status());
        assertTrue(result.index().isPresent());
        assertFalse(result.staleSuspected());
        assertEquals("com.company.UserRegistrationInternal", index.get().implementations().get(0).interfaceName());
        assertEquals("com.company.UserServiceImpl", index.get().implementations().get(0).implementations().get(0));
        assertEquals("com.company.UserServiceImpl", index.get().springBeans().get(0).beanType());
        assertEquals("com.company.UserRepository", index.get().repositories().get(0));
        assertEquals("com.company.NotificationClient", index.get().clients().get(0));
    }

    @Test
    void returnsEmptyWhenProjectIndexJsonDoesNotExist() {
        Path projectDirectory = tempDir.resolve("project");

        Optional<ProjectIndex> index = new ProjectIndexJsonReader().read(projectDirectory);
        ProjectIndexJsonReader.ReadResult result = new ProjectIndexJsonReader().readResult(projectDirectory);

        assertTrue(index.isEmpty());
        assertEquals(ProjectIndexJsonReader.ReadStatus.MISSING, result.status());
        assertTrue(result.index().isEmpty());
        assertTrue(result.diagnostics().contains("project-index.json not found"));
    }

    @Test
    void returnsEmptyWhenProjectIndexJsonIsInvalid() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path codeAtlasDirectory = projectDirectory.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Files.writeString(codeAtlasDirectory.resolve("project-index.json"), "{ invalid-json");

        Optional<ProjectIndex> index = new ProjectIndexJsonReader().read(projectDirectory);
        ProjectIndexJsonReader.ReadResult result = new ProjectIndexJsonReader().readResult(projectDirectory);

        assertTrue(index.isEmpty());
        assertEquals(ProjectIndexJsonReader.ReadStatus.INVALID, result.status());
        assertTrue(result.index().isEmpty());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.startsWith("Failed to read project-index.json:")));
    }

    @Test
    void detectsStaleProjectIndexWhenJavaSourceIsNewer() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path sourceFile = projectDirectory.resolve("src/main/java/com/company/Foo.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
                sourceFile,
                """
                        package com.company;

                        public class Foo {
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
                          "implementations": [],
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
        Files.setLastModifiedTime(sourceFile, FileTime.from(Instant.parse("2020-01-01T00:00:10Z")));

        ProjectIndexJsonReader.ReadResult result = new ProjectIndexJsonReader().readResult(projectDirectory);

        assertEquals(ProjectIndexJsonReader.ReadStatus.LOADED, result.status());
        assertTrue(result.staleSuspected());
        assertEquals(
                "project-index.json is older than at least one Java source file",
                result.staleReasons().get(0)
        );
        assertTrue(result.diagnostics().contains("project-index.json is older than at least one Java source file"));
    }
}
