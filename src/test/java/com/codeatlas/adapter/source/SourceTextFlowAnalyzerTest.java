package com.codeatlas.adapter.source;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceTextFlowAnalyzerTest {
    @TempDir
    Path tempDir;

    @Test
    void findsSourceFileByPackageAndClassName() throws Exception {
        writeJavaFile(
                tempDir.resolve("aaa/FooService.java"),
                """
                        package com.other;

                        public class FooService {
                            public void processOrder() {
                            }
                        }
                        """
        );
        Path expectedSource = tempDir.resolve("src/main/java/com/company/FooService.java");
        writeJavaFile(expectedSource, simpleFooServiceSource());

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.company.FooService.processOrder"
        );

        GraphNode classNode = nodeById(graph, "class:com.company.FooService");
        assertEquals(
                "src/main/java/com/company/FooService.java",
                classNode.attributes().get("sourceFile").toString().replace('\\', '/')
        );
    }

    @Test
    void createsClassAndEntrypointMethodNodes() throws Exception {
        writeJavaFile(tempDir.resolve("src/main/java/com/company/FooService.java"), simpleFooServiceSource());

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.company.FooService.processOrder"
        );

        GraphNode classNode = nodeById(graph, "class:com.company.FooService");
        assertEquals("CLASS", classNode.kind());
        assertEquals("com.company.FooService", classNode.qualifiedName());

        GraphNode methodNode = nodeById(graph, "method:com.company.FooService.processOrder");
        assertEquals("METHOD", methodNode.kind());
        assertEquals("com.company.FooService.processOrder", methodNode.qualifiedName());
        assertEquals(true, methodNode.attributes().get("entrypoint"));
        assertEquals("local", methodNode.attributes().get("resolution"));
    }

    @Test
    void createsDeclaresEdgeFromClassToEntrypointMethod() throws Exception {
        writeJavaFile(tempDir.resolve("src/main/java/com/company/FooService.java"), simpleFooServiceSource());

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.company.FooService.processOrder"
        );

        assertTrue(graph.edges().stream().anyMatch(edge ->
                edge.kind().equals("DECLARES")
                        && edge.sourceNodeId().equals("class:com.company.FooService")
                        && edge.targetNodeId().equals("method:com.company.FooService.processOrder")
        ));
    }

    @Test
    void detectsDirectCallsFromEntrypointMethod() throws Exception {
        writeJavaFile(tempDir.resolve("src/main/java/com/company/FooService.java"), simpleFooServiceSource());

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.company.FooService.processOrder"
        );

        assertNodeExists(graph, "method:com.company.FooService.validate");
        assertNodeExists(graph, "method:repository.save");
        assertNodeExists(graph, "method:paymentClient.charge");
        assertNodeExists(graph, "method:com.company.FooService.mapper");

        List<String> callExpressions = graph.edges().stream()
                .filter(edge -> edge.kind().equals("CALLS"))
                .map(edge -> edge.attributes().get("expression").toString())
                .toList();
        assertEquals(List.of(
                "validate(order)",
                "repository.save(order)",
                "paymentClient.charge(order)",
                "mapper(order)"
        ), callExpressions);

        assertTrue(graph.edges().stream()
                .filter(edge -> edge.kind().equals("CALLS"))
                .allMatch(edge -> edge.sourceNodeId().equals("method:com.company.FooService.processOrder")));
    }

    @Test
    void ignoresCallsInsideCommentsAndStrings() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/company/FooService.java"),
                """
                        package com.company;

                        public class FooService {
                            public void processOrder(Order order) {
                                // ignoredCall(order);
                                String text = "repository.save(order)";
                                /*
                                 * paymentClient.charge(order);
                                 */
                                java.util.List.of(order).forEach(item -> {
                                    ignoredLambdaCall(item);
                                });
                                java.util.List.of(order).forEach(item -> ignoredExpressionLambdaCall(item));
                                validate(order);
                            }

                            private void validate(Order order) {
                            }
                        }

                        class Order {
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.company.FooService.processOrder"
        );

        assertNodeExists(graph, "method:com.company.FooService.validate");
        assertFalse(hasNode(graph, "method:com.company.FooService.ignoredCall"));
        assertFalse(hasNode(graph, "method:com.company.FooService.ignoredLambdaCall"));
        assertFalse(hasNode(graph, "method:com.company.FooService.ignoredExpressionLambdaCall"));
        assertFalse(hasNode(graph, "method:repository.save"));
        assertFalse(hasNode(graph, "method:paymentClient.charge"));
    }

    @Test
    void findsAnnotatedControllerMethodWithGenericReturnAndMemberAccessCall() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/study/onboarding/modules/auth/api/AuthController.java"),
                """
                        package com.study.onboarding.modules.auth.api;

                        import org.springframework.http.ResponseEntity;
                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestBody;

                        public class AuthController {
                            private final UserRegistrationInternal userRegistration;

                            @PostMapping("/register")
                            public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
                                userRegistration.create(
                                        new UserCreateInternalRequest(req.name(), req.email(), req.password())
                                );
                                return ResponseEntity.status(201).build();
                            }
                        }

                        interface UserRegistrationInternal {
                        }

                        class RegisterRequest {
                        }

                        class UserCreateInternalRequest {
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.study.onboarding.modules.auth.api.AuthController.register"
        );

        assertNodeExists(graph, "class:com.study.onboarding.modules.auth.api.AuthController");
        assertNodeExists(graph, "method:com.study.onboarding.modules.auth.api.AuthController.register");
        assertNodeExists(graph, "method:userRegistration.create");
        assertFalse(hasNode(graph, "method:PostMapping"));

        GraphNode callNode = nodeById(graph, "method:userRegistration.create");
        assertEquals("member-access", callNode.attributes().get("resolution"));
        assertEquals("userRegistration", callNode.attributes().get("receiverName"));
    }

    private static String simpleFooServiceSource() {
        return """
                package com.company;

                public class FooService {
                    private final OrderRepository repository = new OrderRepository();
                    private final PaymentClient paymentClient = new PaymentClient();

                    public OrderDto processOrder(Order order) {
                        validate(order);
                        repository.save(order);
                        paymentClient.charge(order);
                        return mapper(order);
                    }

                    private void validate(Order order) {
                    }

                    private OrderDto mapper(Order order) {
                        return new OrderDto();
                    }
                }

                class Order {
                }

                class OrderDto {
                }

                class OrderRepository {
                    void save(Order order) {
                    }
                }

                class PaymentClient {
                    void charge(Order order) {
                    }
                }
                """;
    }

    private static void writeJavaFile(Path file, String source) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }

    private static GraphNode nodeById(FlowGraph graph, String nodeId) {
        return graph.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasNode(FlowGraph graph, String nodeId) {
        return graph.nodes().stream().anyMatch(node -> node.id().equals(nodeId));
    }

    private static void assertNodeExists(FlowGraph graph, String nodeId) {
        assertTrue(hasNode(graph, nodeId), "Expected node " + nodeId);
    }
}
