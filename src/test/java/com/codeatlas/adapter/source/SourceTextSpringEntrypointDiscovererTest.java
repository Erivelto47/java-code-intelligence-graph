package com.codeatlas.adapter.source;

import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.EntrypointIndex;
import com.codeatlas.core.entrypoint.EntrypointKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceTextSpringEntrypointDiscovererTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void detectsPostMappingInRestControllerWithClassRequestMapping() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/spring/simple/AuthController.java"),
                """
                        package com.example.spring.simple;

                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/auth")
                        public class AuthController {
                            @PostMapping("/register")
                            public void register() {
                            }

                            public void helper() {
                            }
                        }
                        """
        );

        EntrypointIndex index = new SourceTextSpringEntrypointDiscoverer().discover(tempDir);

        assertEquals("1.0", index.schemaVersion());
        assertEquals(1, index.entrypoints().size());
        EntrypointDescriptor entrypoint = index.entrypoints().get(0);
        assertEquals(EntrypointKind.HTTP_ENDPOINT, entrypoint.kind());
        assertEquals("POST", entrypoint.httpMethod());
        assertEquals("/auth/register", entrypoint.path());
        assertEquals("com.example.spring.simple.AuthController", entrypoint.className());
        assertEquals("register", entrypoint.methodName());
        assertEquals("com.example.spring.simple.AuthController.register", entrypoint.javaEntrypoint());
        assertEquals(
                "http:POST:/auth/register -> com.example.spring.simple.AuthController.register",
                entrypoint.id()
        );
        assertTrue(entrypoint.annotations().classLevel().contains("@RestController"));
        assertTrue(entrypoint.annotations().classLevel().contains("@RequestMapping(\"/auth\")"));
        assertEquals(List.of("@PostMapping(\"/register\")"), entrypoint.annotations().methodLevel());
        assertEquals("src/main/java/com/example/spring/simple/AuthController.java", entrypoint.sourceLocation().file());
    }

    @Test
    void combinesPathAndDetectsRequestMappingMethodAttribute() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/spring/requestmapping/UserController.java"),
                """
                        package com.example.spring.requestmapping;

                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RequestMethod;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/users/")
                        public class UserController {
                            @RequestMapping(value = "/{id}", method = RequestMethod.GET)
                            public String getById() {
                                return "ok";
                            }
                        }
                        """
        );

        EntrypointDescriptor entrypoint = new SourceTextSpringEntrypointDiscoverer()
                .discover(tempDir)
                .entrypoints()
                .get(0);

        assertEquals("GET", entrypoint.httpMethod());
        assertEquals("/users/{id}", entrypoint.path());
        assertEquals("com.example.spring.requestmapping.UserController.getById", entrypoint.javaEntrypoint());
    }

    @Test
    void detectsMultipleHttpMethodsInSameController() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/spring/multiple/UserController.java"),
                """
                        package com.example.spring.multiple;

                        import org.springframework.web.bind.annotation.DeleteMapping;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.PutMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping(path = "/users")
                        public class UserController {
                            @GetMapping
                            public String list() {
                                return "list";
                            }

                            @PostMapping(path = "")
                            public String create() {
                                return "create";
                            }

                            @PutMapping(value = "/{id}")
                            public String update() {
                                return "update";
                            }

                            @DeleteMapping("/{id}")
                            public void delete() {
                            }

                            public void helper() {
                            }
                        }
                        """
        );

        List<EntrypointDescriptor> entrypoints = new SourceTextSpringEntrypointDiscoverer()
                .discover(tempDir)
                .entrypoints();

        assertEquals(4, entrypoints.size());
        assertEndpoint(entrypoints, "GET", "/users", "com.example.spring.multiple.UserController.list");
        assertEndpoint(entrypoints, "POST", "/users", "com.example.spring.multiple.UserController.create");
        assertEndpoint(entrypoints, "PUT", "/users/{id}", "com.example.spring.multiple.UserController.update");
        assertEndpoint(entrypoints, "DELETE", "/users/{id}", "com.example.spring.multiple.UserController.delete");
        assertFalse(entrypoints.stream().anyMatch(entrypoint -> entrypoint.methodName().equals("helper")));
    }

    @Test
    void doesNotDetectMappingsOutsideControllerClasses() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/spring/notcontroller/NotAController.java"),
                """
                        package com.example.spring.notcontroller;

                        import org.springframework.web.bind.annotation.GetMapping;

                        public class NotAController {
                            @GetMapping("/internal")
                            public void internal() {
                            }
                        }
                        """
        );

        EntrypointIndex index = new SourceTextSpringEntrypointDiscoverer().discover(tempDir);

        assertTrue(index.entrypoints().isEmpty());
    }

    @Test
    void usesAnyForRequestMappingWithoutHttpMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/spring/any/AnyController.java"),
                """
                        package com.example.spring.any;

                        import org.springframework.stereotype.Controller;
                        import org.springframework.web.bind.annotation.RequestMapping;

                        @Controller
                        @RequestMapping("/root")
                        public class AnyController {
                            @RequestMapping(path = "/any")
                            public void any() {
                            }
                        }
                        """
        );

        EntrypointDescriptor entrypoint = new SourceTextSpringEntrypointDiscoverer()
                .discover(tempDir)
                .entrypoints()
                .get(0);

        assertEquals("ANY", entrypoint.httpMethod());
        assertEquals("/root/any", entrypoint.path());
        assertEquals("com.example.spring.any.AnyController.any", entrypoint.javaEntrypoint());
    }

    @Test
    void phase2ExampleFixturesStayAlignedWithEntrypointDiscovery() throws Exception {
        List<String> examples = List.of(
                "01-simple-rest-controller",
                "02-request-mapping-method",
                "03-multiple-http-methods"
        );

        SourceTextSpringEntrypointDiscoverer discoverer = new SourceTextSpringEntrypointDiscoverer();
        for (String example : examples) {
            Path examplePath = Path.of("examples/phase-2-spring-entrypoints").resolve(example);
            JsonNode expected = OBJECT_MAPPER.readTree(
                    examplePath.resolve("code-atlas.expected/expected-entrypoints.json").toFile()
            ).get("entrypoints");
            List<EntrypointDescriptor> actual = discoverer.discover(examplePath).entrypoints();

            assertEquals(expected.size(), actual.size(), example);
            for (int i = 0; i < actual.size(); i++) {
                JsonNode expectedEntrypoint = expected.get(i);
                EntrypointDescriptor actualEntrypoint = actual.get(i);
                assertEquals(expectedEntrypoint.get("id").asText(), actualEntrypoint.id(), example);
                assertEquals(expectedEntrypoint.get("kind").asText(), actualEntrypoint.kind().name(), example);
                assertEquals(expectedEntrypoint.get("httpMethod").asText(), actualEntrypoint.httpMethod(), example);
                assertEquals(expectedEntrypoint.get("path").asText(), actualEntrypoint.path(), example);
                assertEquals(expectedEntrypoint.get("className").asText(), actualEntrypoint.className(), example);
                assertEquals(expectedEntrypoint.get("methodName").asText(), actualEntrypoint.methodName(), example);
                assertEquals(expectedEntrypoint.get("javaEntrypoint").asText(), actualEntrypoint.javaEntrypoint(), example);
                assertEquals(
                        expectedEntrypoint.get("sourceLocation").get("file").asText(),
                        actualEntrypoint.sourceLocation().file(),
                        example
                );
                assertEquals(
                        expectedEntrypoint.get("sourceLocation").get("line").asInt(),
                        actualEntrypoint.sourceLocation().line(),
                        example
                );
            }
        }
    }

    private static void assertEndpoint(
            List<EntrypointDescriptor> entrypoints,
            String httpMethod,
            String path,
            String javaEntrypoint
    ) {
        assertTrue(entrypoints.stream().anyMatch(entrypoint ->
                entrypoint.httpMethod().equals(httpMethod)
                        && entrypoint.path().equals(path)
                        && entrypoint.javaEntrypoint().equals(javaEntrypoint)
        ), httpMethod + " " + path + " -> " + javaEntrypoint);
    }

    private static void writeJavaFile(Path file, String source) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
