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
    void recordsRecognizedUnsupportedDecisionShapesAsUnresolved() throws Exception {
        writeJavaFile(
                tempDir.resolve("src/main/java/com/example/RegistrationGuard.java"),
                """
                        package com.example;

                        public class RegistrationGuard {
                            public void validate(RegisterRequest request) {
                                if (request.email() == null) throw new IllegalArgumentException("Email is required");

                                if (request.blocked()) {
                                    audit(request.email());
                                    throw new IllegalStateException("Blocked registration");
                                }

                                if (request.legacy()) {
                                    throw createLegacyException(request.email());
                                }
                            }

                            private void audit(String email) {
                            }

                            private RuntimeException createLegacyException(String email) {
                                return new IllegalStateException(email);
                            }

                            public record RegisterRequest(String email, boolean blocked, boolean legacy) {
                            }
                        }
                        """
        );

        DecisionTrace trace = new JavaIfThrowDecisionExtractor().analyze(tempDir, "com.example.RegistrationGuard.validate");

        assertEquals(0, trace.decisions().size());
        assertEquals(3, trace.unresolved().size());

        UnresolvedDecision inlineThrow = trace.unresolved().get(0);
        assertEquals("UNSUPPORTED_INLINE_THROW", inlineThrow.kind());
        assertEquals("com.example.RegistrationGuard.validate", inlineThrow.method());
        assertEquals(5, inlineThrow.sourceLocation().line());
        assertEquals("if (request.email() == null) throw new IllegalArgumentException(\"Email is required\");",
                inlineThrow.expression());

        assertEquals("UNSUPPORTED_THROW_WITH_ADDITIONAL_STATEMENTS", trace.unresolved().get(1).kind());
        assertEquals("UNSUPPORTED_THROW_EXPRESSION", trace.unresolved().get(2).kind());
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
