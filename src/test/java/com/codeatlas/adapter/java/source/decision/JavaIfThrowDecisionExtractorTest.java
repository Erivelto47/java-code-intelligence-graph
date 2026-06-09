package com.codeatlas.adapter.java.source.decision;

import com.codeatlas.core.decision.DecisionBranch;
import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.UnresolvedDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaIfThrowDecisionExtractorTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsDirectIfThrowDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/UserService.java"),
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

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.UserService.create");

        assertEquals("1.0", trace.schemaVersion());
        assertEquals("com.example.UserService.create", trace.scope().entrypoint());
        assertEquals(1, trace.decisions().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("CONDITIONAL_THROW", decision.kind().name());
        assertEquals("VALIDATION", decision.category().name());
        assertEquals("com.example.UserService.create", decision.method());
        assertEquals("com.example.UserService", decision.source().className());
        assertEquals("create", decision.source().methodName());
        assertEquals("src/main/java/com/example/UserService.java", decision.sourceLocation().file());
        assertEquals(5, decision.sourceLocation().line());
        assertEquals("request.name() == null || request.name().isBlank()", decision.expression().text());
        assertEquals("isNullOrBlank(request.name)", decision.expression().normalized());
        assertEquals("request.name", decision.subjects().get(0).name());

        DecisionOutcome throwOutcome = decision.outcomes().get(0);
        assertEquals("true", throwOutcome.when());
        assertEquals("THROW", throwOutcome.action().name());
        assertEquals("IllegalArgumentException", throwOutcome.exceptionType());
        assertEquals("Name is required", throwOutcome.message());
    }

    @Test
    void extractsDirectSingleLineIfThrowDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/PaymentGuard.java"),
                """
                        package com.example;

                        public class PaymentGuard {
                            public void validate(PaymentRequest request) {
                                if (request.amount() == null) throw new IllegalArgumentException("Amount is required");
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.PaymentGuard.validate");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("CONDITIONAL_THROW", decision.kind().name());
        assertEquals("VALIDATION", decision.category().name());
        assertEquals("request.amount() == null", decision.expression().text());
        assertEquals("request.amount", decision.subjects().get(0).name());
        assertEquals(
                "if (request.amount() == null) throw new IllegalArgumentException(\"Amount is required\");",
                decision.evidence().snippet()
        );

        DecisionOutcome throwOutcome = decision.outcomes().get(0);
        assertEquals("THROW", throwOutcome.action().name());
        assertEquals("IllegalArgumentException", throwOutcome.exceptionType());
        assertEquals("Amount is required", throwOutcome.message());
    }

    @Test
    void extractsIfThrowDecisionWithAllowedPreStatements() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/RegistrationGuard.java"),
                """
                        package com.example;

                        public class RegistrationGuard {
                            public void validate(CreateUserRequest request) {
                                if (request.name() == null || request.name().isBlank()) {
                                    logInvalidName(request);
                                    metrics.increment("registration.invalid_name");
                                    throw new IllegalArgumentException("Name is required");
                                }
                            }

                            private void logInvalidName(CreateUserRequest request) {
                            }

                            private final Metrics metrics = new Metrics();

                            private static class Metrics {
                                void increment(String name) {
                                }
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.RegistrationGuard.validate");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("CONDITIONAL_THROW", decision.kind().name());
        assertEquals("request.name() == null || request.name().isBlank()", decision.expression().text());
        assertEquals("IllegalArgumentException", decision.outcomes().get(0).exceptionType());
        assertEquals("Name is required", decision.outcomes().get(0).message());
        assertEquals(
                "if (request.name() == null || request.name().isBlank()) { "
                        + "logInvalidName(request); "
                        + "metrics.increment(\"registration.invalid_name\"); "
                        + "throw new IllegalArgumentException(\"Name is required\"); }",
                decision.evidence().snippet()
        );
    }

    @Test
    void extractsSimpleEarlyReturnDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/ImportService.java"),
                """
                        package com.example;

                        public class ImportService {
                            public boolean process(ImportRequest request) {
                                if (request.processed()) {
                                    return false;
                                }

                                return true;
                            }

                            public record ImportRequest(boolean processed) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.ImportService.process");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("EARLY_RETURN", decision.kind().name());
        assertEquals("UNKNOWN", decision.category().name());
        assertEquals("request.processed()", decision.expression().text());
        assertEquals("request.processed", decision.expression().normalized());
        assertEquals("request.processed", decision.subjects().get(0).name());

        DecisionOutcome returnOutcome = decision.outcomes().get(0);
        assertEquals("true", returnOutcome.when());
        assertEquals("RETURN", returnOutcome.action().name());
        assertEquals("false", returnOutcome.target());
    }

    @Test
    void extractsSimpleIfElseReturnDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/FeatureToggleDecision.java"),
                """
                        package com.example;

                        public class FeatureToggleDecision {
                            public boolean resolve(FeatureRequest request) {
                                if (request.enabled()) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }

                            public record FeatureRequest(boolean enabled) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.FeatureToggleDecision.resolve");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("IF_ELSE_CONDITION", decision.kind().name());
        assertEquals("UNKNOWN", decision.category().name());
        assertEquals("request.enabled()", decision.expression().text());
        assertEquals("request.enabled", decision.expression().normalized());
        assertEquals("request.enabled", decision.subjects().get(0).name());
        assertEquals(
                "if (request.enabled()) { return true; } else { return false; }",
                decision.evidence().snippet()
        );

        DecisionOutcome trueOutcome = decision.outcomes().get(0);
        assertEquals("true", trueOutcome.when());
        assertEquals("RETURN", trueOutcome.action().name());
        assertEquals("true", trueOutcome.target());

        DecisionOutcome falseOutcome = decision.outcomes().get(1);
        assertEquals("false", falseOutcome.when());
        assertEquals("RETURN", falseOutcome.action().name());
        assertEquals("false", falseOutcome.target());
    }

    @Test
    void extractsIfElseThrowReturnDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/AccessDecision.java"),
                """
                        package com.example;

                        public class AccessDecision {
                            public boolean resolve(AccessRequest request) {
                                if (!request.allowed()) {
                                    throw new IllegalStateException("Access denied");
                                } else {
                                    return true;
                                }
                            }

                            public record AccessRequest(boolean allowed) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.AccessDecision.resolve");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("IF_ELSE_CONDITION", decision.kind().name());
        assertEquals("!request.allowed()", decision.expression().text());
        assertEquals("request.allowed", decision.subjects().get(0).name());
        assertEquals(
                "if (!request.allowed()) { throw new IllegalStateException(\"Access denied\"); } else { return true; }",
                decision.evidence().snippet()
        );

        DecisionOutcome throwOutcome = decision.outcomes().get(0);
        assertEquals("true", throwOutcome.when());
        assertEquals("THROW", throwOutcome.action().name());
        assertEquals("IllegalStateException", throwOutcome.target());
        assertEquals("IllegalStateException", throwOutcome.exceptionType());
        assertEquals("Access denied", throwOutcome.message());

        DecisionOutcome returnOutcome = decision.outcomes().get(1);
        assertEquals("false", returnOutcome.when());
        assertEquals("RETURN", returnOutcome.action().name());
        assertEquals("true", returnOutcome.target());
    }

    @Test
    void extractsIfElseReturnThrowDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/AccessDecision.java"),
                """
                        package com.example;

                        public class AccessDecision {
                            public boolean resolve(AccessRequest request) {
                                if (request.allowed()) {
                                    return true;
                                } else {
                                    throw new IllegalStateException("Access denied");
                                }
                            }

                            public record AccessRequest(boolean allowed) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.AccessDecision.resolve");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("IF_ELSE_CONDITION", decision.kind().name());

        DecisionOutcome returnOutcome = decision.outcomes().get(0);
        assertEquals("true", returnOutcome.when());
        assertEquals("RETURN", returnOutcome.action().name());
        assertEquals("true", returnOutcome.target());

        DecisionOutcome throwOutcome = decision.outcomes().get(1);
        assertEquals("false", throwOutcome.when());
        assertEquals("THROW", throwOutcome.action().name());
        assertEquals("IllegalStateException", throwOutcome.exceptionType());
        assertEquals("Access denied", throwOutcome.message());
    }

    @Test
    void extractsElseIfChainWithOrderedBranchesAndOutcomes() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/AccessDecision.java"),
                """
                        package com.example;

                        public class AccessDecision {
                            public String resolve(AccessRequest request) {
                                if (request == null) {
                                    throw new IllegalArgumentException("Request is required");
                                } else if (request.blocked()) {
                                    return "blocked";
                                } else if (request.admin()) {
                                    return "admin";
                                } else {
                                    return "standard";
                                }
                            }

                            public record AccessRequest(boolean blocked, boolean admin) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.AccessDecision.resolve");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("IF_ELSE_IF_CHAIN", decision.kind().name());
        assertEquals("request == null", decision.expression().text());
        assertEquals(4, decision.branches().size());
        assertEquals("IF", decision.branches().get(0).kind());
        assertEquals("request == null", decision.branches().get(0).condition());
        assertEquals(1, decision.branches().get(0).order());
        assertEquals("THROW", decision.branches().get(0).outcomes().get(0).action().name());
        assertEquals("ELSE_IF", decision.branches().get(1).kind());
        assertEquals("request.blocked()", decision.branches().get(1).condition());
        assertEquals(2, decision.branches().get(1).order());
        assertEquals("\"blocked\"", decision.branches().get(1).outcomes().get(0).target());
        assertEquals("ELSE_IF", decision.branches().get(2).kind());
        assertEquals("request.admin()", decision.branches().get(2).condition());
        assertEquals(3, decision.branches().get(2).order());
        assertEquals("ELSE", decision.branches().get(3).kind());
        assertEquals(4, decision.branches().get(3).order());
        assertEquals("\"standard\"", decision.branches().get(3).outcomes().get(0).target());
    }

    @Test
    void extractsNestedIfDecisionsWithParentBranchContext() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/RoutingDecision.java"),
                """
                        package com.example;

                        public class RoutingDecision {
                            public String resolve(RouteRequest request) {
                                if (request.internal()) {
                                    if (request.priority()) {
                                        return "priority";
                                    }
                                    return "internal";
                                } else {
                                    if (request.blocked()) {
                                        throw new IllegalStateException("Route blocked");
                                    }
                                    return "external";
                                }
                            }

                            public record RouteRequest(boolean internal, boolean priority, boolean blocked) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.RoutingDecision.resolve");

        assertEquals(3, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode outer = trace.decisions().get(0);
        assertEquals("IF_ELSE_CONDITION", outer.kind().name());
        assertEquals("request.internal()", outer.expression().text());
        assertEquals(2, outer.branches().size());
        assertEquals(2, outer.children().size());
        assertEquals(1, outer.children().get(0).parentBranchOrder());
        assertEquals(2, outer.children().get(1).parentBranchOrder());

        DecisionNode nestedThen = trace.decisions().get(1);
        assertEquals("EARLY_RETURN", nestedThen.kind().name());
        assertEquals("request.priority()", nestedThen.expression().text());
        assertEquals(outer.id(), nestedThen.parent().decisionId());
        assertEquals(1, nestedThen.parent().branchOrder());
        assertEquals("\"priority\"", nestedThen.outcomes().get(0).target());

        DecisionNode nestedElse = trace.decisions().get(2);
        assertEquals("CONDITIONAL_THROW", nestedElse.kind().name());
        assertEquals("request.blocked()", nestedElse.expression().text());
        assertEquals(outer.id(), nestedElse.parent().decisionId());
        assertEquals(2, nestedElse.parent().branchOrder());
        assertEquals("IllegalStateException", nestedElse.outcomes().get(0).exceptionType());
    }

    @Test
    void linksSupportedDecisionFromSimpleSameClassHelperCall() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/UserRegistration.java"),
                """
                        package com.example;

                        public class UserRegistration {
                            public void create(CreateUserRequest request) {
                                validateName(request);
                            }

                            private void validateName(CreateUserRequest request) {
                                if (request.name() == null || request.name().isBlank()) {
                                    throw new IllegalArgumentException("Name is required");
                                }
                            }

                            public record CreateUserRequest(String name) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.UserRegistration.create");

        assertEquals(1, trace.decisions().size());
        assertEquals(0, trace.unresolved().size());

        DecisionNode decision = trace.decisions().get(0);
        assertEquals("CONDITIONAL_THROW", decision.kind().name());
        assertEquals("com.example.UserRegistration.validateName", decision.method());
        assertEquals("com.example.UserRegistration", decision.source().className());
        assertEquals("validateName", decision.source().methodName());
        assertEquals("com.example.UserRegistration.validateName", decision.source().signature());
        assertEquals(9, decision.sourceLocation().line());
        assertEquals("request.name() == null || request.name().isBlank()", decision.expression().text());
        assertEquals(
                "if (request.name() == null || request.name().isBlank()) { "
                        + "throw new IllegalArgumentException(\"Name is required\"); }",
                decision.evidence().snippet()
        );
        assertEquals(List.of("com.example.UserRegistration.validateName"), decision.links().calledMethods());
    }

    @Test
    void recordsAmbiguousLocalHelperOverloadsAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/UserRegistration.java"),
                """
                        package com.example;

                        public class UserRegistration {
                            public void create(CreateUserRequest request) {
                                validateName(request);
                            }

                            private void validateName(CreateUserRequest request) {
                                if (request.name() == null) {
                                    throw new IllegalArgumentException("Name is required");
                                }
                            }

                            private void validateName(String name) {
                                if (name == null) {
                                    throw new IllegalArgumentException("Name is required");
                                }
                            }

                            public record CreateUserRequest(String name) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.UserRegistration.create");

        assertEquals(0, trace.decisions().size());
        assertEquals(1, trace.unresolved().size());
        assertEquals("UNRESOLVED_LOCAL_HELPER_OVERLOAD", trace.unresolved().get(0).kind());
        assertEquals("validateName(request);", trace.unresolved().get(0).expression());
    }

    @Test
    void recordsUnsafeLocalHelperCallVariantsAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/UserRegistration.java"),
                """
                        package com.example;

                        public class UserRegistration {
                            private final Validator validator = new Validator();

                            public void create(CreateUserRequest request) {
                                validateName(transform(request));
                                validator.validateName(request);
                            }

                            private void validateName(CreateUserRequest request) {
                                if (request.name() == null) {
                                    throw new IllegalArgumentException("Name is required");
                                }
                            }

                            private CreateUserRequest transform(CreateUserRequest request) {
                                return request;
                            }

                            private static class Validator {
                                void validateName(CreateUserRequest request) {
                                }
                            }

                            public record CreateUserRequest(String name) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.UserRegistration.create");

        assertEquals(0, trace.decisions().size());
        assertEquals(2, trace.unresolved().size());
        assertEquals("UNRESOLVED_LOCAL_HELPER_ARGUMENTS", trace.unresolved().get(0).kind());
        assertEquals("validateName(transform(request));", trace.unresolved().get(0).expression());
        assertEquals("UNRESOLVED_LOCAL_HELPER_CROSS_OBJECT", trace.unresolved().get(1).kind());
        assertEquals("validator.validateName(request);", trace.unresolved().get(1).expression());
    }

    @Test
    void recordsUnsupportedIfElseWithoutTerminalReturnsAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/FeatureToggleDecision.java"),
                """
                        package com.example;

                        public class FeatureToggleDecision {
                            public void resolve(FeatureRequest request) {
                                if (request.enabled()) {
                                    enable();
                                } else {
                                    disable();
                                }
                            }

                            private void enable() {
                            }

                            private void disable() {
                            }

                            public record FeatureRequest(boolean enabled) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.FeatureToggleDecision.resolve");

        assertEquals(0, trace.decisions().size());
        assertEquals(1, trace.unresolved().size());
        assertEquals("UNSUPPORTED_IF_ELSE", trace.unresolved().get(0).kind());
        assertEquals(
                "if (request.enabled()) { enable(); } else { disable(); }",
                trace.unresolved().get(0).expression()
        );
    }

    @Test
    void recordsUnsupportedMixedIfElseBranchesAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/AccessDecision.java"),
                """
                        package com.example;

                        public class AccessDecision {
                            public boolean resolve(AccessRequest request) {
                                if (!request.allowed()) {
                                    throw createException();
                                } else {
                                    return true;
                                }

                                if (request.expired()) {
                                    throw new IllegalStateException(buildMessage());
                                } else {
                                    return false;
                                }

                                if (request.auditRequired()) {
                                    logAudit();
                                    throw new IllegalStateException("Audit required");
                                } else {
                                    return true;
                                }
                            }

                            private RuntimeException createException() {
                                return new IllegalStateException("Access denied");
                            }

                            private String buildMessage() {
                                return "Access denied";
                            }

                            private void logAudit() {
                            }

                            public record AccessRequest(boolean allowed, boolean expired, boolean auditRequired) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.AccessDecision.resolve");

        assertEquals(0, trace.decisions().size());
        assertEquals(3, trace.unresolved().size());
        assertEquals("UNSUPPORTED_IF_ELSE", trace.unresolved().get(0).kind());
        assertEquals("UNSUPPORTED_IF_ELSE", trace.unresolved().get(1).kind());
        assertEquals("UNSUPPORTED_IF_ELSE", trace.unresolved().get(2).kind());
    }

    @Test
    void recordsRecognizedUnsupportedDecisionShapesAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/RegistrationGuard.java"),
                """
                        package com.example;

                        public class RegistrationGuard {
                            public void validate(RegisterRequest request) {
                                if (request.email() == null) throw createInlineException(request.email());

                                if (request.blocked()) {
                                    if (request.email().isBlank()) {
                                        throw new IllegalStateException("Blocked registration");
                                    }
                                    throw new IllegalStateException("Blocked registration");
                                }

                                if (request.legacy()) {
                                    throw createLegacyException(request.email());
                                }

                                if (request.name() == null) throw new IllegalArgumentException(buildMessage());

                                if (request.missingCode()) throw new IllegalStateException();

                                if (request.disabled()) throw new IllegalStateException("Disabled registration"); else allowDisabled();
                            }

                            private RuntimeException createInlineException(String email) {
                                return new IllegalArgumentException(email);
                            }

                            private RuntimeException createLegacyException(String email) {
                                return new IllegalStateException(email);
                            }

                            private String buildMessage() {
                                return "Name is required";
                            }

                            private void allowDisabled() {
                            }

                            public record RegisterRequest(
                                    String email,
                                    String name,
                                    boolean blocked,
                                    boolean legacy,
                                    boolean missingCode,
                                    boolean disabled) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.RegistrationGuard.validate");

        assertEquals(0, trace.decisions().size());
        assertEquals(6, trace.unresolved().size());

        UnresolvedDecision inlineThrow = trace.unresolved().get(0);
        assertEquals("UNSUPPORTED_INLINE_THROW", inlineThrow.kind());
        assertEquals("com.example.RegistrationGuard.validate", inlineThrow.method());
        assertEquals(5, inlineThrow.sourceLocation().line());
        assertEquals("if (request.email() == null) throw createInlineException(request.email());",
                inlineThrow.expression());

        assertEquals("UNSUPPORTED_NESTED_IF", trace.unresolved().get(1).kind());
        assertEquals("UNSUPPORTED_THROW_EXPRESSION", trace.unresolved().get(2).kind());
        assertEquals("UNSUPPORTED_INLINE_THROW", trace.unresolved().get(3).kind());
        assertEquals(
                "if (request.name() == null) throw new IllegalArgumentException(buildMessage());",
                trace.unresolved().get(3).expression()
        );
        assertEquals("UNSUPPORTED_INLINE_THROW", trace.unresolved().get(4).kind());
        assertEquals(
                "if (request.missingCode()) throw new IllegalStateException();",
                trace.unresolved().get(4).expression()
        );
        assertEquals("UNSUPPORTED_IF_ELSE", trace.unresolved().get(5).kind());
    }

    @Test
    void recordsUnsafePreStatementsBeforeFinalThrowAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/RegistrationGuard.java"),
                """
                        package com.example;

                        public class RegistrationGuard {
                            public void validate(RegisterRequest request) {
                                if (request.blocked()) {
                                    notify(() -> {
                                        throw new IllegalStateException("lambda");
                                    });
                                    throw new IllegalStateException("Blocked registration");
                                }
                            }

                            private void notify(Runnable runnable) {
                            }

                            public record RegisterRequest(boolean blocked) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.RegistrationGuard.validate");

        assertEquals(0, trace.decisions().size());
        assertEquals(1, trace.unresolved().size());
        assertEquals("UNSUPPORTED_THROW_WITH_ADDITIONAL_STATEMENTS", trace.unresolved().get(0).kind());
    }

    @Test
    void recordsComplexEarlyReturnExpressionAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/ImportService.java"),
                """
                        package com.example;

                        public class ImportService {
                            private final ResultFactory resultFactory = new ResultFactory();

                            public ProcessingResult process(ImportRequest request) {
                                if (request.processed()) {
                                    return resultFactory.create(request.id()).normalize();
                                }

                                return new ProcessingResult(request.id());
                            }

                            public record ImportRequest(String id, boolean processed) {
                            }

                            public record ProcessingResult(String id) {
                                ProcessingResult normalize() {
                                    return this;
                                }
                            }

                            private static class ResultFactory {
                                ProcessingResult create(String id) {
                                    return new ProcessingResult(id);
                                }
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.ImportService.process");

        assertEquals(0, trace.decisions().size());
        assertEquals(1, trace.unresolved().size());
        assertEquals("UNSUPPORTED_COMPLEX_RETURN", trace.unresolved().get(0).kind());
        assertEquals(
                "if (request.processed()) { return resultFactory.create(request.id()).normalize(); }",
                trace.unresolved().get(0).expression()
        );
    }

    @Test
    void extractsSwitchStatementDecisionWithOrderedLabelsAndDefault() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/StatusDecision.java"),
                """
                        package com.example;

                        public class StatusDecision {
                            public String resolve(StatusRequest request) {
                                switch (request.status()) {
                                    case "PENDING":
                                        return "queued";
                                    case "APPROVED", "ACTIVE":
                                        return "open";
                                    default:
                                        return "closed";
                                }
                            }

                            public record StatusRequest(String status) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.StatusDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("SWITCH_DECISION", decision.kind().name());
        assertEquals("request.status()", decision.selector());
        assertEquals("request.status()", decision.expression().text());
        assertEquals("request.status", decision.expression().normalized());
        assertEquals(3, decision.branches().size());

        DecisionBranch pending = decision.branches().get(0);
        assertEquals("CASE", pending.kind());
        assertEquals(List.of("\"PENDING\""), pending.labels());
        assertEquals("\"queued\"", pending.outcomes().get(0).target());

        DecisionBranch approvedOrActive = decision.branches().get(1);
        assertEquals("CASE", approvedOrActive.kind());
        assertEquals(List.of("\"APPROVED\"", "\"ACTIVE\""), approvedOrActive.labels());
        assertEquals("\"open\"", approvedOrActive.outcomes().get(0).target());

        DecisionBranch defaultBranch = decision.branches().get(2);
        assertEquals("DEFAULT", defaultBranch.kind());
        assertEquals(List.of(), defaultBranch.labels());
        assertEquals("\"closed\"", defaultBranch.outcomes().get(0).target());
    }

    @Test
    void extractsSwitchExpressionDecisionWithAssignmentArrowAndYieldOutputs() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/FeeDecision.java"),
                """
                        package com.example;

                        public class FeeDecision {
                            public String resolve(FeeRequest request) {
                                String fee = switch (request.type()) {
                                    case STANDARD -> "standard";
                                    case PREMIUM -> {
                                        yield "premium";
                                    }
                                    default -> "unknown";
                                };
                                return fee;
                            }

                            public record FeeRequest(FeeType type) {
                            }

                            public enum FeeType {
                                STANDARD,
                                PREMIUM,
                                UNKNOWN
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.FeeDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("SWITCH_EXPRESSION_DECISION", decision.kind().name());
        assertEquals("request.type()", decision.selector());
        assertEquals("fee", decision.assignedTo());
        assertEquals("fee", decision.subjects().get(1).name());
        assertEquals("ASSIGNMENT_TARGET", decision.subjects().get(1).kind());
        assertEquals(3, decision.branches().size());

        assertEquals(List.of("STANDARD"), decision.branches().get(0).labels());
        assertEquals("ASSIGN", decision.branches().get(0).outcomes().get(0).action().name());
        assertEquals("\"standard\"", decision.branches().get(0).outcomes().get(0).target());

        assertEquals(List.of("PREMIUM"), decision.branches().get(1).labels());
        assertEquals("ASSIGN", decision.branches().get(1).outcomes().get(0).action().name());
        assertEquals("\"premium\"", decision.branches().get(1).outcomes().get(0).target());

        assertEquals("DEFAULT", decision.branches().get(2).kind());
        assertEquals("ASSIGN", decision.branches().get(2).outcomes().get(0).action().name());
        assertEquals("\"unknown\"", decision.branches().get(2).outcomes().get(0).target());
    }

    @Test
    void extractsReturnTernaryDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/StatusDecision.java"),
                """
                        package com.example;

                        public class StatusDecision {
                            public String resolve(StatusRequest request) {
                                return request.active() ? "active" : "inactive";
                            }

                            public record StatusRequest(boolean active) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.StatusDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("TERNARY_CONDITION", decision.kind().name());
        assertEquals("request.active()", decision.expression().text());
        assertEquals("request.active", decision.expression().normalized());
        assertEquals(2, decision.branches().size());

        DecisionOutcome trueOutcome = decision.outcomes().get(0);
        assertEquals("true", trueOutcome.when());
        assertEquals("RETURN", trueOutcome.action().name());
        assertEquals("\"active\"", trueOutcome.target());
        assertEquals("Ternary true branch returns \"active\"", trueOutcome.meaning());

        DecisionOutcome falseOutcome = decision.outcomes().get(1);
        assertEquals("false", falseOutcome.when());
        assertEquals("RETURN", falseOutcome.action().name());
        assertEquals("\"inactive\"", falseOutcome.target());
        assertEquals("Ternary false branch returns \"inactive\"", falseOutcome.meaning());
        assertEquals(
                "return request.active() ? \"active\" : \"inactive\";",
                decision.evidence().snippet()
        );
    }

    @Test
    void extractsAssignmentTernaryDecisionFromEntrypointMethod() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/StatusDecision.java"),
                """
                        package com.example;

                        public class StatusDecision {
                            public String resolve(StatusRequest request) {
                                String label = request.active() ? "active" : "inactive";
                                return label;
                            }

                            public record StatusRequest(boolean active) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.StatusDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("TERNARY_CONDITION", decision.kind().name());
        assertEquals("request.active()", decision.expression().text());
        assertEquals("label", decision.subjects().get(1).name());
        assertEquals("ASSIGNMENT_TARGET", decision.subjects().get(1).kind());
        assertEquals("ASSIGN", decision.outcomes().get(0).action().name());
        assertEquals("\"active\"", decision.outcomes().get(0).target());
        assertEquals("Ternary true branch initializes label with \"active\"", decision.outcomes().get(0).meaning());
        assertEquals("ASSIGN", decision.outcomes().get(1).action().name());
        assertEquals("\"inactive\"", decision.outcomes().get(1).target());
        assertEquals(
                "String label = request.active() ? \"active\" : \"inactive\";",
                decision.evidence().snippet()
        );
    }

    @Test
    void extractsNestedTernaryDecisionHierarchy() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/StatusDecision.java"),
                """
                        package com.example;

                        public class StatusDecision {
                            public String resolve(StatusRequest request) {
                                return request.active() ? (request.admin() ? "admin" : "active") : "inactive";
                            }

                            public record StatusRequest(boolean active, boolean admin) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.StatusDecision.resolve");

        assertEquals(2, trace.decisions().size());
        DecisionNode parent = trace.decisions().get(0);
        DecisionNode child = trace.decisions().get(1);
        assertEquals("TERNARY_CONDITION", parent.kind().name());
        assertEquals("request.active()", parent.expression().text());
        assertEquals(1, parent.children().size());
        assertEquals(child.id(), parent.children().get(0).decisionId());
        assertEquals(1, parent.children().get(0).parentBranchOrder());
        assertEquals("WHEN_TRUE", parent.children().get(0).parentBranchKind());

        assertEquals("TERNARY_CONDITION", child.kind().name());
        assertEquals("request.admin()", child.expression().text());
        assertEquals(parent.id(), child.parent().decisionId());
        assertEquals(1, child.parent().branchOrder());
        assertEquals("WHEN_TRUE", child.parent().branchKind());
    }

    @Test
    void extractsOptionalOrElseThrowDecision() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/UserDecision.java"),
                """
                        package com.example;

                        import java.util.Optional;

                        public class UserDecision {
                            public User resolve(UserRepository repository, String id) {
                                return repository.findById(id)
                                        .filter(user -> user.active())
                                        .orElseThrow(() -> new NotFoundException("User not found"));
                            }

                            public interface UserRepository {
                                Optional<User> findById(String id);
                            }

                            public record User(boolean active) {
                            }

                            public static class NotFoundException extends RuntimeException {
                                NotFoundException(String message) {
                                    super(message);
                                }
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.UserDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("OPTIONAL_BRANCH", decision.kind().name());
        assertEquals(
                "repository.findById(id) .filter(user -> user.active()) .orElseThrow(() -> new NotFoundException(\"User not found\"))",
                decision.expression().text()
        );
        assertEquals(2, decision.branches().size());
        assertEquals("PRESENT", decision.branches().get(0).kind());
        assertEquals("RETURN", decision.branches().get(0).outcomes().get(0).action().name());
        assertEquals("EMPTY", decision.branches().get(1).kind());
        assertEquals("THROW", decision.branches().get(1).outcomes().get(0).action().name());
        assertEquals("NotFoundException", decision.branches().get(1).outcomes().get(0).exceptionType());
        assertEquals("User not found", decision.branches().get(1).outcomes().get(0).message());
    }

    @Test
    void extractsOptionalFallbackDecision() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/DisplayNameDecision.java"),
                """
                        package com.example;

                        import java.util.Optional;

                        public class DisplayNameDecision {
                            public String resolve(Profile profile) {
                                String displayName = Optional.ofNullable(profile.name())
                                        .map(String::trim)
                                        .orElseGet(() -> "anonymous");
                                return displayName;
                            }

                            public record Profile(String name) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.DisplayNameDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("OPTIONAL_BRANCH", decision.kind().name());
        assertEquals("profile.name", decision.subjects().get(0).name());
        assertEquals("displayName", decision.subjects().get(1).name());
        assertEquals("ASSIGNMENT_TARGET", decision.subjects().get(1).kind());
        assertEquals("PRESENT", decision.branches().get(0).kind());
        assertEquals("ASSIGN", decision.branches().get(0).outcomes().get(0).action().name());
        assertEquals("EMPTY", decision.branches().get(1).kind());
        assertEquals("ASSIGN", decision.branches().get(1).outcomes().get(0).action().name());
        assertEquals("() -> \"anonymous\"", decision.branches().get(1).outcomes().get(0).target());
    }

    @Test
    void extractsOptionalIfPresentOrElseDecisionBranches() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/NotificationDecision.java"),
                """
                        package com.example;

                        import java.util.Optional;

                        public class NotificationDecision {
                            public void resolve(Optional<User> user) {
                                user.ifPresentOrElse(
                                        value -> notify(value),
                                        () -> auditMissing());
                            }

                            private void notify(User user) {
                            }

                            private void auditMissing() {
                            }

                            public record User(String id) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.NotificationDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("OPTIONAL_BRANCH", decision.kind().name());
        assertEquals("user.ifPresentOrElse( value -> notify(value), () -> auditMissing())", decision.expression().text());
        assertEquals("PRESENT", decision.branches().get(0).kind());
        assertEquals("value -> notify(value)", decision.branches().get(0).outcomes().get(0).target());
        assertEquals("EMPTY", decision.branches().get(1).kind());
        assertEquals("() -> auditMissing()", decision.branches().get(1).outcomes().get(0).target());
    }

    @Test
    void extractsStreamFilterDecision() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/ActiveUserDecision.java"),
                """
                        package com.example;

                        import java.util.List;

                        public class ActiveUserDecision {
                            public List<User> resolve(List<User> users) {
                                return users.stream()
                                        .filter(user -> user.active())
                                        .toList();
                            }

                            public record User(boolean active) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.ActiveUserDecision.resolve");

        assertEquals(1, trace.decisions().size());
        DecisionNode decision = trace.decisions().get(0);
        assertEquals("STREAM_FILTER_DECISION", decision.kind().name());
        assertEquals("filter", decision.operation());
        assertEquals("users.stream() .filter(user -> user.active())", decision.pipeline());
        assertEquals("user", decision.predicate().parameter());
        assertEquals("user.active()", decision.predicate().text());
        assertEquals("FILTER", decision.outcomes().get(0).action().name());
    }

    @Test
    void extractsStreamAnyMatchDecisionFromIfCondition() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/InvalidItemDecision.java"),
                """
                        package com.example;

                        import java.util.List;

                        public class InvalidItemDecision {
                            public boolean resolve(List<Item> items) {
                                if (items.stream().anyMatch(item -> item.invalid())) {
                                    return false;
                                }
                                return true;
                            }

                            public record Item(boolean invalid) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.InvalidItemDecision.resolve");

        assertEquals(2, trace.decisions().size());
        DecisionNode ifDecision = trace.decisions().get(0);
        DecisionNode streamDecision = trace.decisions().get(1);
        assertEquals("EARLY_RETURN", ifDecision.kind().name());
        assertEquals("STREAM_MATCH_DECISION", streamDecision.kind().name());
        assertEquals("anyMatch", streamDecision.operation());
        assertEquals("items.stream().anyMatch(item -> item.invalid())", streamDecision.pipeline());
        assertEquals("item", streamDecision.predicate().parameter());
        assertEquals("item.invalid()", streamDecision.predicate().text());
        assertEquals(ifDecision.id(), streamDecision.parent().decisionId());
        assertEquals("CONDITION", streamDecision.parent().branchKind());
    }

    @Test
    void extractsStreamAllAndNoneMatchDecisions() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/EligibilityDecision.java"),
                """
                        package com.example;

                        import java.util.List;

                        public class EligibilityDecision {
                            public boolean resolve(List<Account> accounts) {
                                boolean allActive = accounts.stream().allMatch(account -> account.active());
                                boolean noneBlocked = accounts.stream().noneMatch(account -> account.blocked());
                                return allActive && noneBlocked;
                            }

                            public record Account(boolean active, boolean blocked) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor()
                .analyze(tempDir, "com.example.EligibilityDecision.resolve");

        assertEquals(2, trace.decisions().size());
        DecisionNode allMatch = trace.decisions().get(0);
        DecisionNode noneMatch = trace.decisions().get(1);
        assertEquals("STREAM_MATCH_DECISION", allMatch.kind().name());
        assertEquals("allMatch", allMatch.operation());
        assertEquals("account", allMatch.predicate().parameter());
        assertEquals("account.active()", allMatch.predicate().text());
        assertEquals("allActive", allMatch.assignedTo());

        assertEquals("STREAM_MATCH_DECISION", noneMatch.kind().name());
        assertEquals("noneMatch", noneMatch.operation());
        assertEquals("account", noneMatch.predicate().parameter());
        assertEquals("account.blocked()", noneMatch.predicate().text());
        assertEquals("noneBlocked", noneMatch.assignedTo());
    }

    private static void writeJavaFile(Path sourceFile, String source) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
    }
}
