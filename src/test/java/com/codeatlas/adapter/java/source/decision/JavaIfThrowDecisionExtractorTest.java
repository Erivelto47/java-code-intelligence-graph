package com.codeatlas.adapter.java.source.decision;

import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionTrace;
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

    private static void writeJavaFile(Path sourceFile, String source) throws Exception {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);
    }
}
