package com.codeatlas.output.decision.json;

import com.codeatlas.core.decision.DecisionArtifactSource;
import com.codeatlas.core.decision.DecisionCategory;
import com.codeatlas.core.decision.DecisionCondition;
import com.codeatlas.core.decision.DecisionEvidence;
import com.codeatlas.core.decision.DecisionKind;
import com.codeatlas.core.decision.DecisionLinks;
import com.codeatlas.core.decision.DecisionNode;
import com.codeatlas.core.decision.DecisionOutcome;
import com.codeatlas.core.decision.DecisionOutcomeAction;
import com.codeatlas.core.decision.DecisionScope;
import com.codeatlas.core.decision.DecisionSource;
import com.codeatlas.core.decision.DecisionSourceLocation;
import com.codeatlas.core.decision.DecisionSubject;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.DecisionTraceMetadata;
import com.codeatlas.core.decision.UnresolvedDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionTraceJsonWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writesCanonicalJsonWithFinalNewline() throws Exception {
        Path outputFile = new DecisionTraceJsonWriter().write(sampleTrace(), tempDir);

        String json = Files.readString(outputFile);

        assertEquals("""
                {
                  "schemaVersion": "1.0",
                  "generatedAt": "1970-01-01T00:00:00Z",
                  "project": "examples/phase-4-decision-trace/01-if-throw-validation",
                  "scope": {
                    "kind": "ENTRYPOINT",
                    "entrypoint": "com.example.UserService.create",
                    "endpoint": null
                  },
                  "source": {
                    "flowRef": null,
                    "projectIndexRef": null
                  },
                  "decisions": [
                    {
                      "id": "decision:com.example.UserService.create:if-throw:1",
                      "kind": "CONDITIONAL_THROW",
                      "category": "VALIDATION",
                      "method": "com.example.UserService.create",
                      "source": {
                        "className": "com.example.UserService",
                        "methodName": "create",
                        "signature": "com.example.UserService.create"
                      },
                      "sourceLocation": {
                        "file": "src/main/java/com/example/UserService.java",
                        "line": 5
                      },
                      "expression": {
                        "text": "request.name() == null || request.name().isBlank()",
                        "normalized": "isNullOrBlank(request.name)"
                      },
                      "subjects": [
                        {
                          "name": "request.name",
                          "kind": "INPUT_FIELD"
                        }
                      ],
                      "outcomes": [
                        {
                          "when": "true",
                          "action": "THROW",
                          "target": "IllegalArgumentException",
                          "exceptionType": "IllegalArgumentException",
                          "message": "Name is required",
                          "meaning": "Input is rejected by this check"
                        },
                        {
                          "when": "false",
                          "action": "CONTINUE",
                          "target": null,
                          "exceptionType": null,
                          "message": null,
                          "meaning": "Execution continues after this check"
                        }
                      ],
                      "evidence": {
                        "kind": "SOURCE_TEXT",
                        "snippet": "if (request.name() == null || request.name().isBlank()) { throw new IllegalArgumentException(\\"Name is required\\"); }"
                      },
                      "links": {
                        "flowNodeIds": [],
                        "calledMethods": [],
                        "relatedBoundaries": []
                      },
                      "confidence": "HIGH"
                    }
                  ],
                  "unresolved": [],
                  "metadata": {
                    "analyzer": "if-throw-source-text-decision-extractor",
                    "phase": "phase-4.1-decision-trace-extractor",
                    "deterministic": true,
                    "source": "source-text"
                  }
                }
                """, json);
    }

    @Test
    void writesTheSameTraceDeterministically() throws Exception {
        DecisionTrace trace = sampleTrace();
        DecisionTraceJsonWriter writer = new DecisionTraceJsonWriter();

        Path firstOutput = writer.write(trace, tempDir.resolve("first"));
        Path secondOutput = writer.write(trace, tempDir.resolve("second"));

        String firstJson = Files.readString(firstOutput);
        String secondJson = Files.readString(secondOutput);
        assertEquals(firstJson, secondJson);
        assertTrue(firstJson.endsWith("\n"));
    }

    private static DecisionTrace sampleTrace() {
        return new DecisionTrace(
                "1.0",
                Instant.EPOCH,
                "examples/phase-4-decision-trace/01-if-throw-validation",
                new DecisionScope("ENTRYPOINT", "com.example.UserService.create", null),
                new DecisionArtifactSource(null, null),
                List.of(new DecisionNode(
                        "decision:com.example.UserService.create:if-throw:1",
                        DecisionKind.CONDITIONAL_THROW,
                        DecisionCategory.VALIDATION,
                        "com.example.UserService.create",
                        new DecisionSource("com.example.UserService", "create", "com.example.UserService.create"),
                        new DecisionSourceLocation("src/main/java/com/example/UserService.java", 5),
                        new DecisionCondition(
                                "request.name() == null || request.name().isBlank()",
                                "isNullOrBlank(request.name)"
                        ),
                        List.of(new DecisionSubject("request.name", "INPUT_FIELD")),
                        List.of(
                                new DecisionOutcome(
                                        "true",
                                        DecisionOutcomeAction.THROW,
                                        "IllegalArgumentException",
                                        "IllegalArgumentException",
                                        "Name is required",
                                        "Input is rejected by this check"
                                ),
                                new DecisionOutcome(
                                        "false",
                                        DecisionOutcomeAction.CONTINUE,
                                        null,
                                        null,
                                        null,
                                        "Execution continues after this check"
                                )
                        ),
                        new DecisionEvidence(
                                "SOURCE_TEXT",
                                "if (request.name() == null || request.name().isBlank()) { throw new IllegalArgumentException(\"Name is required\"); }"
                        ),
                        new DecisionLinks(List.of(), List.of(), List.of()),
                        "HIGH"
                )),
                List.<UnresolvedDecision>of(),
                new DecisionTraceMetadata(
                        "if-throw-source-text-decision-extractor",
                        "phase-4.1-decision-trace-extractor",
                        true,
                        "source-text"
                )
        );
    }
}
