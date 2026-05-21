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
        assertNodeExists(graph, "method:com.company.OrderRepository.save");
        assertNodeExists(graph, "method:com.company.PaymentClient.charge");
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
                            void create(UserCreateInternalRequest request);
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
        assertNodeExists(graph, "interface:com.study.onboarding.modules.auth.api.UserRegistrationInternal");
        assertNodeExists(graph, "method:com.study.onboarding.modules.auth.api.UserRegistrationInternal.create");
        assertFalse(hasNode(graph, "method:PostMapping"));

        GraphNode callNode = nodeById(graph, "method:com.study.onboarding.modules.auth.api.UserRegistrationInternal.create");
        assertEquals("field", callNode.attributes().get("resolution"));
        assertEquals("userRegistration", callNode.attributes().get("receiverName"));
        assertTrue(graph.unresolved().stream().anyMatch(unresolved ->
                unresolved.reason().equals("NO_IMPLEMENTATION")
                        && unresolved.symbol().equals("com.study.onboarding.modules.auth.api.UserRegistrationInternal.create")
        ));
    }

    @Test
    void resolvesInterfaceReceiverToSingleImplementationAndTraversesImplementation() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/RegistrationController.java"),
                """
                        package com.example;

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

                        @Service
                        class RegistrationService implements RegistrationUseCase {
                            public void create() {
                                validate();
                            }

                            private void validate() {
                            }
                        }

                        @interface Service {
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.example.RegistrationController.register"
        );

        assertNodeExists(graph, "method:com.example.RegistrationUseCase.create");
        assertNodeExists(graph, "method:com.example.RegistrationService.create");
        assertNodeExists(graph, "method:com.example.RegistrationService.validate");
        assertTrue(graph.edges().stream().anyMatch(edge ->
                edge.kind().equals("RESOLVES_TO")
                        && edge.sourceNodeId().equals("method:com.example.RegistrationUseCase.create")
                        && edge.targetNodeId().equals("method:com.example.RegistrationService.create")
        ));
        assertTrue(graph.resolutions().stream().anyMatch(resolution ->
                resolution.sourceNodeId().equals("method:com.example.RegistrationUseCase.create")
                        && resolution.targetNodeId().equals("method:com.example.RegistrationService.create")
                        && resolution.evidence().equals("INFERRED")
        ));
    }

    @Test
    void recordsMultipleInterfaceImplementationsAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/NotificationController.java"),
                """
                        package com.example;

                        public class NotificationController {
                            private final NotificationSender notificationSender;

                            public NotificationController(NotificationSender notificationSender) {
                                this.notificationSender = notificationSender;
                            }

                            public void send() {
                                notificationSender.send();
                            }
                        }

                        interface NotificationSender {
                            void send();
                        }

                        class EmailNotificationSender implements NotificationSender {
                            public void send() {
                            }
                        }

                        class SmsNotificationSender implements NotificationSender {
                            public void send() {
                            }
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.example.NotificationController.send"
        );

        assertTrue(graph.unresolved().stream().anyMatch(unresolved ->
                unresolved.reason().equals("MULTIPLE_IMPLEMENTATIONS")
                        && unresolved.symbol().equals("com.example.NotificationSender.send")
                        && unresolved.candidates().contains("com.example.EmailNotificationSender.send")
                        && unresolved.candidates().contains("com.example.SmsNotificationSender.send")
        ));
    }

    @Test
    void recordsRepositoryCallsAsBoundaries() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/AccountService.java"),
                """
                        package com.example;

                        import org.springframework.data.jpa.repository.JpaRepository;

                        public class AccountService {
                            private final AccountRepository accountRepository;

                            public AccountService(AccountRepository accountRepository) {
                                this.accountRepository = accountRepository;
                            }

                            public void openAccount() {
                                accountRepository.findByDocument("123");
                                accountRepository.save(new Account());
                            }
                        }

                        interface AccountRepository extends JpaRepository<Account, Long> {
                            Account findByDocument(String document);
                        }

                        class Account {
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.example.AccountService.openAccount"
        );

        assertNodeExists(graph, "boundary:com.example.AccountRepository.findByDocument");
        assertNodeExists(graph, "boundary:com.example.AccountRepository.save");
        assertTrue(graph.boundaries().stream().anyMatch(boundary ->
                boundary.kind().equals("REPOSITORY")
                        && boundary.symbol().equals("com.example.AccountRepository.findByDocument")
        ));
        assertTrue(graph.boundaries().stream().anyMatch(boundary ->
                boundary.kind().equals("REPOSITORY")
                        && boundary.symbol().equals("com.example.AccountRepository.save")
        ));
    }

    @Test
    void recordsExternalClientFrameworkCallAsBoundaryAfterInterfaceResolution() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/PaymentService.java"),
                """
                        package com.example;

                        import org.springframework.web.client.RestClient;

                        public class PaymentService {
                            private final PaymentGatewayClient paymentGatewayClient;

                            public PaymentService(PaymentGatewayClient paymentGatewayClient) {
                                this.paymentGatewayClient = paymentGatewayClient;
                            }

                            public void pay() {
                                paymentGatewayClient.authorize();
                            }
                        }

                        interface PaymentGatewayClient {
                            void authorize();
                        }

                        class HttpPaymentGatewayClient implements PaymentGatewayClient {
                            private final RestClient restClient;

                            HttpPaymentGatewayClient(RestClient restClient) {
                                this.restClient = restClient;
                            }

                            public void authorize() {
                                restClient.post().retrieve();
                            }
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.example.PaymentService.pay"
        );

        assertNodeExists(graph, "method:com.example.PaymentGatewayClient.authorize");
        assertNodeExists(graph, "method:com.example.HttpPaymentGatewayClient.authorize");
        assertNodeExists(graph, "boundary:org.springframework.web.client.RestClient.post");
        assertTrue(graph.boundaries().stream().anyMatch(boundary ->
                boundary.kind().equals("HTTP_CLIENT")
                        && boundary.symbol().equals("org.springframework.web.client.RestClient.post")
        ));
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
