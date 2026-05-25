package com.codeatlas.output.json;

import com.codeatlas.core.project.ProjectIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertTrue(index.isPresent());
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

        assertTrue(index.isEmpty());
    }

    @Test
    void returnsEmptyWhenProjectIndexJsonIsInvalid() throws Exception {
        Path projectDirectory = tempDir.resolve("project");
        Path codeAtlasDirectory = projectDirectory.resolve(".code-atlas");
        Files.createDirectories(codeAtlasDirectory);
        Files.writeString(codeAtlasDirectory.resolve("project-index.json"), "{ invalid-json");

        Optional<ProjectIndex> index = new ProjectIndexJsonReader().read(projectDirectory);

        assertTrue(index.isEmpty());
    }
}
