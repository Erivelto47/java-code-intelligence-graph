package com.codeatlas.adapter.java.source.decision;

import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.UnresolvedDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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

    private static void writeJavaFile(Path sourceFile, String source) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
    }
}
