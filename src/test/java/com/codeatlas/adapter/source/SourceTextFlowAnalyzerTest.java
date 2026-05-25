package com.codeatlas.adapter.source;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;
import com.codeatlas.core.project.ProjectIndexHints;
import com.codeatlas.output.context.ContextPackWriter;
import com.codeatlas.output.handoff.AgentHandoffWriter;
import com.codeatlas.output.markdown.MarkdownFlowWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceTextFlowAnalyzerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                        && resolution.kind().equals("INTERFACE_SINGLE_IMPLEMENTATION")
                        && resolution.evidence().equals("INFERRED")
                        && resolution.confidence().equals("HIGH")
        ));

        GraphEdge resolutionEdge = graph.edges().stream()
                .filter(edge -> edge.kind().equals("RESOLVES_TO"))
                .findFirst()
                .orElseThrow();
        assertEquals("INFERRED", resolutionEdge.attributes().get("evidence"));
        assertEquals("HIGH", resolutionEdge.attributes().get("confidence"));
    }

    @Test
    void usesProjectIndexHintsForInterfaceResolution() throws Exception {
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

                        class RegistrationService {
                            public void create() {
                            }
                        }
                        """
        );
        ProjectIndexHints hints = new ProjectIndexHints(
                Map.of("com.example.RegistrationUseCase", List.of("com.example.RegistrationService")),
                Set.of(),
                Set.of(),
                Set.of(),
                1
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.example.RegistrationController.register",
                hints,
                "test"
        );

        assertEquals(true, graph.metadata().get("projectIndexAssisted"));
        assertEquals("test", graph.metadata().get("projectIndexSource"));
        assertEquals(1, graph.metadata().get("projectIndexImplementations"));
        assertTrue(graph.resolutions().stream().anyMatch(resolution ->
                resolution.sourceNodeId().equals("method:com.example.RegistrationUseCase.create")
                        && resolution.targetNodeId().equals("method:com.example.RegistrationService.create")
                        && resolution.kind().equals("INTERFACE_SINGLE_IMPLEMENTATION")
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
        assertFalse(graph.edges().stream().anyMatch(edge ->
                edge.kind().equals("RESOLVES_TO")
                        && edge.sourceNodeId().equals("method:com.example.NotificationSender.send")
        ));
        assertFalse(graph.edges().stream().anyMatch(edge ->
                edge.kind().equals("CALLS")
                        && (edge.targetNodeId().equals("method:com.example.EmailNotificationSender.send")
                        || edge.targetNodeId().equals("method:com.example.SmsNotificationSender.send"))
        ));
        assertFalse(hasNode(graph, "method:com.example.EmailNotificationSender.formatEmail"));
        assertFalse(hasNode(graph, "method:com.example.SmsNotificationSender.formatSms"));
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

    @Test
    void recordsLocalHttpClientInterfaceWithNoImplementationAsBoundaryTarget() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/PaymentService.java"),
                """
                        package com.example;

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
                            private final HttpClient httpClient;

                            HttpPaymentGatewayClient(HttpClient httpClient) {
                                this.httpClient = httpClient;
                            }

                            public void authorize() {
                                httpClient.post();
                            }
                        }

                        interface HttpClient {
                            void post();
                        }
                        """
        );

        FlowGraph graph = new SourceTextFlowAnalyzer().analyze(
                tempDir,
                "com.example.PaymentService.pay"
        );

        assertNodeExists(graph, "boundary:com.example.HttpClient.post");
        assertFalse(hasNode(graph, "method:com.example.HttpClient.post"));
        assertTrue(graph.edges().stream().anyMatch(edge ->
                edge.kind().equals("CALLS")
                        && edge.sourceNodeId().equals("method:com.example.HttpPaymentGatewayClient.authorize")
                        && edge.targetNodeId().equals("boundary:com.example.HttpClient.post")
        ));
        assertTrue(graph.boundaries().stream().anyMatch(boundary ->
                boundary.kind().equals("HTTP_CLIENT")
                        && boundary.symbol().equals("com.example.HttpClient.post")
                        && boundary.reason().equals("NO_LOCAL_IMPLEMENTATION")
        ));
    }

    @Test
    void derivedOutputsExposeResolutionsBoundariesAndUnresolvedSections() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/PhaseController.java"),
                """
                        package com.example;

                        public class PhaseController {
                            private final UseCase useCase;

                            public PhaseController(UseCase useCase) {
                                this.useCase = useCase;
                            }

                            public void go() {
                                useCase.run();
                            }
                        }

                        interface UseCase {
                            void run();
                        }

                        class UseCaseImpl implements UseCase {
                            private final AccountRepository accountRepository;
                            private final NotificationSender notificationSender;

                            UseCaseImpl(AccountRepository accountRepository, NotificationSender notificationSender) {
                                this.accountRepository = accountRepository;
                                this.notificationSender = notificationSender;
                            }

                            public void run() {
                                accountRepository.save();
                                notificationSender.send();
                            }
                        }

                        interface AccountRepository {
                            void save();
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
                "com.example.PhaseController.go"
        );
        assertEquals(1, graph.resolutions().size());
        assertEquals(1, graph.boundaries().size());
        assertEquals(1, graph.unresolved().size());

        Path outputDirectory = tempDir.resolve("derived-output");
        new MarkdownFlowWriter().write(graph, outputDirectory);
        new ContextPackWriter().write(graph, outputDirectory);
        new AgentHandoffWriter().write(graph, tempDir, outputDirectory, "test/repo", true, false);

        String flowMarkdown = Files.readString(outputDirectory.resolve("flow.md"));
        assertTrue(flowMarkdown.contains("## Resolutions"));
        assertTrue(flowMarkdown.contains("INTERFACE_SINGLE_IMPLEMENTATION"));
        assertTrue(flowMarkdown.contains("## Boundaries"));
        assertTrue(flowMarkdown.contains("com.example.AccountRepository.save"));
        assertTrue(flowMarkdown.contains("## Unresolved"));
        assertTrue(flowMarkdown.contains("MULTIPLE_IMPLEMENTATIONS"));

        String contextPack = Files.readString(outputDirectory.resolve("context-pack.md"));
        assertTrue(contextPack.contains("Resolution count: 1"));
        assertTrue(contextPack.contains("Boundary count: 1"));
        assertTrue(contextPack.contains("Unresolved count: 1"));
        assertTrue(contextPack.contains("None. This artifact contains deterministic facts only."));

        String handoff = Files.readString(outputDirectory.resolve("agent-handoff.md"));
        assertTrue(handoff.contains("Resolution count: `1`"));
        assertTrue(handoff.contains("Boundary count: `1`"));
        assertTrue(handoff.contains("Unresolved count: `1`"));
        assertTrue(handoff.contains("## Inferred Resolutions"));
        assertTrue(handoff.contains("evidence=`INFERRED`"));
        assertTrue(handoff.contains("## Boundaries"));
        assertTrue(handoff.contains("kind=`REPOSITORY`"));
        assertTrue(handoff.contains("## Unresolved"));
        assertTrue(handoff.contains("reason=`MULTIPLE_IMPLEMENTATIONS`"));
    }

    @Test
    void phase1ExampleFixturesStayAlignedWithCurrentFlowGraphSchema() throws Exception {
        List<Phase1Example> examples = List.of(
                new Phase1Example("01-direct-method-call", "com.example.direct.OrderController.create"),
                new Phase1Example("02-controller-service", "com.example.controllerservice.CustomerController.register"),
                new Phase1Example("03-interface-single-implementation", "com.example.interfaces.single.RegistrationController.register"),
                new Phase1Example("04-interface-multiple-implementations", "com.example.interfaces.multiple.NotificationController.send"),
                new Phase1Example("05-repository-boundary", "com.example.repository.AccountService.openAccount"),
                new Phase1Example("06-external-client-boundary", "com.example.externalclient.PaymentService.pay")
        );

        SourceTextFlowAnalyzer analyzer = new SourceTextFlowAnalyzer();
        for (Phase1Example example : examples) {
            Path examplePath = Path.of("examples/phase-1-java-flow").resolve(example.directory());
            Path expectedPath = examplePath.resolve("code-atlas.expected");
            assertTrue(Files.isRegularFile(expectedPath.resolve("expected-flow.json")), example.directory());
            assertTrue(Files.isRegularFile(expectedPath.resolve("expected-flow.md")), example.directory());
            assertTrue(Files.isRegularFile(expectedPath.resolve("expected-flow.mmd")), example.directory());
            assertTrue(Files.isRegularFile(expectedPath.resolve("expected-context-pack.md")), example.directory());

            FlowGraph graph = analyzer.analyze(examplePath, example.entrypoint());
            JsonNode expectedJson = OBJECT_MAPPER.readTree(expectedPath.resolve("expected-flow.json").toFile());
            assertEquals("1.0", expectedJson.get("schemaVersion").asText(), example.directory());
            assertEquals(example.entrypoint(), expectedJson.get("entrypoint").asText(), example.directory());
            assertEquals(graph.nodes().size(), expectedJson.get("nodes").size(), example.directory());
            assertEquals(graph.edges().size(), expectedJson.get("edges").size(), example.directory());
            assertEquals(graph.resolutions().size(), expectedJson.get("resolutions").size(), example.directory());
            assertEquals(graph.boundaries().size(), expectedJson.get("boundaries").size(), example.directory());
            assertEquals(graph.unresolved().size(), expectedJson.get("unresolved").size(), example.directory());
        }
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

    private record Phase1Example(String directory, String entrypoint) {
    }
}
