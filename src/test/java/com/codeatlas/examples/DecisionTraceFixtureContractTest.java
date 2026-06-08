package com.codeatlas.examples;

import com.codeatlas.cli.AnalyzeFlowCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionTraceFixtureContractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> DECISION_KINDS = Set.of(
            "IF_CONDITION",
            "IF_ELSE_CONDITION",
            "IF_ELSE_IF_CHAIN",
            "EARLY_RETURN",
            "CONDITIONAL_THROW",
            "SWITCH_DECISION",
            "SWITCH_EXPRESSION_DECISION",
            "SWITCH_CASE",
            "TERNARY_CONDITION",
            "OPTIONAL_BRANCH",
            "STREAM_FILTER",
            "UNKNOWN_CONDITION"
    );
    private static final Set<String> CATEGORIES = Set.of(
            "VALIDATION",
            "AUTHORIZATION",
            "BUSINESS_RULE",
            "ERROR_HANDLING",
            "ROUTING",
            "TRANSFORMATION",
            "SIDE_EFFECT_GUARD",
            "UNKNOWN"
    );
    private static final Set<String> ACTIONS = Set.of(
            "THROW",
            "RETURN",
            "CONTINUE",
            "CALL",
            "ASSIGN",
            "MAP",
            "FILTER",
            "UNKNOWN"
    );

    @Test
    void ifThrowValidationFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/01-if-throw-validation"),
                "com.example.UserService.create",
                tempDir
        );
    }

    @Test
    void earlyReturnFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/02-if-return-early-return"),
                "com.example.decisiontrace.ifreturn.ImportService.process",
                tempDir
        );
    }

    @Test
    void unresolvedDecisionShapesFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/03-unresolved-decision-shapes"),
                "com.example.decisiontrace.unresolved.RegistrationGuard.validate",
                tempDir
        );
    }

    @Test
    void ifThrowWithPreStatementsFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/04-if-throw-with-pre-statements"),
                "com.example.decisiontrace.blockthrow.RegistrationGuard.validate",
                tempDir
        );
    }

    @Test
    void singleLineIfThrowFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/05-single-line-if-throw"),
                "com.example.decisiontrace.singleline.PaymentGuard.validate",
                tempDir
        );
    }

    @Test
    void ifElseReturnBranchesFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/06-if-else-return-branches"),
                "com.example.decisiontrace.ifelse.FeatureToggleDecision.resolve",
                tempDir
        );
    }

    @Test
    void ifElseThrowReturnBranchesFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/07-if-else-throw-return-branches"),
                "com.example.decisiontrace.ifelsemixed.AccessDecision.resolve",
                tempDir
        );
    }

    @Test
    void methodLocalDecisionCallFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/08-method-local-decision-call"),
                "com.example.decisiontrace.localcall.UserRegistration.create",
                tempDir
        );
    }

    @Test
    void elseIfChainFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/09-else-if-chain"),
                "com.example.decisiontrace.elseif.AccessDecision.resolve",
                tempDir
        );
    }

    @Test
    void nestedIfDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/10-nested-if-decision"),
                "com.example.decisiontrace.nestedif.RoutingDecision.resolve",
                tempDir
        );
    }

    @Test
    void booleanAndOrNotConditionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/11-boolean-and-or-not-condition"),
                "com.example.decisiontrace.booleancondition.AccessPolicy.resolve",
                tempDir
        );
    }

    @Test
    void comparisonAndMethodPredicateConditionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/12-comparison-and-method-predicate-condition"),
                "com.example.decisiontrace.comparisonpredicate.EligibilityDecision.resolve",
                tempDir
        );
    }

    @Test
    void returnTernaryDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/13-return-ternary-decision"),
                "com.example.decisiontrace.ternaryreturn.StatusDecision.resolve",
                tempDir
        );
    }

    @Test
    void assignmentTernaryDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/14-assignment-ternary-decision"),
                "com.example.decisiontrace.ternaryassignment.StatusDecision.resolve",
                tempDir
        );
    }

    @Test
    void nestedTernaryDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/15-nested-ternary-decision"),
                "com.example.decisiontrace.ternarynested.StatusDecision.resolve",
                tempDir
        );
    }

    @Test
    void switchStatementDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/15-switch-statement-decision"),
                "com.example.decisiontrace.switchstatement.StatusDecision.resolve",
                tempDir
        );
    }

    @Test
    void switchExpressionDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/16-switch-expression-decision"),
                "com.example.decisiontrace.switchexpression.FeeDecision.resolve",
                tempDir
        );
    }

    @Test
    void switchFallthroughDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/17-switch-fallthrough-decision"),
                "com.example.decisiontrace.switchfallthrough.StatusDecision.resolve",
                tempDir
        );
    }

    @Test
    void optionalOrElseThrowDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/18-optional-or-else-throw-decision"),
                "com.example.decisiontrace.optionalthrow.UserDecision.resolve",
                tempDir
        );
    }

    @Test
    void optionalFallbackDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/19-optional-or-else-fallback-decision"),
                "com.example.decisiontrace.optionalfallback.DisplayNameDecision.resolve",
                tempDir
        );
    }

    @Test
    void optionalIfPresentOrElseDecisionFixtureMatchesGeneratedArtifactsExactly(@TempDir Path tempDir) throws Exception {
        assertAnalyzeDecisionsFixtureMatches(
                Path.of("examples/phase-4-decision-trace/20-optional-if-present-or-else-decision"),
                "com.example.decisiontrace.optionalifpresent.NotificationDecision.resolve",
                tempDir
        );
    }

    private static void assertAnalyzeDecisionsFixtureMatches(
            Path fixture,
            String entrypoint,
            Path tempDir
    ) throws Exception {
        Path expected = fixture.resolve("expected");
        Path generated = tempDir.resolve(fixture.getFileName().toString());

        int exitCode = new AnalyzeFlowCommand().run(new String[]{
                "analyze-decisions",
                "--project", fixture.toString(),
                "--entrypoint", entrypoint,
                "--output", generated.toString()
        });

        assertEquals(0, exitCode);
        assertArtifactMatches(expected, generated, "decisions.json");
        assertArtifactMatches(expected, generated, "decisions.md");
        assertArtifactMatches(expected, generated, "decisions.mmd");
        assertTrue(Files.readString(generated.resolve("decisions.json")).endsWith("\n"));
    }

    @Test
    void phaseFourDecisionFixturesContainValidDecisionJson() throws Exception {
        Path fixturesRoot = Path.of("examples/phase-4-decision-trace");

        assertTrue(Files.isDirectory(fixturesRoot), "Phase 4 fixture root must exist");

        List<Path> decisionJsonFiles;
        try (Stream<Path> paths = Files.walk(fixturesRoot)) {
            decisionJsonFiles = paths
                    .filter(path -> path.getFileName().toString().equals("decisions.json"))
                    .sorted()
                    .toList();
        }

        assertTrue(decisionJsonFiles.size() >= 3, "At least three Phase 4 decisions.json fixtures are required");

        for (Path decisionJsonFile : decisionJsonFiles) {
            JsonNode root = OBJECT_MAPPER.readTree(decisionJsonFile.toFile());

            assertEquals("1.0", root.path("schemaVersion").asText(), decisionJsonFile.toString());
            assertNonBlank(decisionJsonFile, root, "generatedAt");
            assertNonBlank(decisionJsonFile, root, "project");
            assertTrue(root.path("scope").isObject(), decisionJsonFile + " must contain scope object");
            assertTrue(root.path("source").isObject(), decisionJsonFile + " must contain source object");
            assertTrue(root.path("source").has("flowRef"), decisionJsonFile + " must contain source.flowRef");
            assertTrue(root.path("source").has("projectIndexRef"), decisionJsonFile + " must contain source.projectIndexRef");
            assertTrue(root.path("decisions").isArray(), decisionJsonFile + " must contain decisions array");
            assertTrue(root.path("unresolved").isArray(), decisionJsonFile + " must contain unresolved array");
            assertTrue(
                    !root.path("decisions").isEmpty() || !root.path("unresolved").isEmpty(),
                    decisionJsonFile + " must contain at least one decision or unresolved item"
            );
            assertTrue(root.path("metadata").isObject(), decisionJsonFile + " must contain metadata object");

            for (JsonNode decision : root.path("decisions")) {
                assertDecisionContract(decisionJsonFile, decision);
            }
            for (JsonNode unresolved : root.path("unresolved")) {
                assertUnresolvedContract(decisionJsonFile, unresolved);
            }
        }
    }

    private static void assertDecisionContract(Path decisionJsonFile, JsonNode decision) {
        assertTrue(decision.path("id").asText().startsWith("decision:"), decisionJsonFile + " decision id must be stable");
        assertTrue(DECISION_KINDS.contains(decision.path("kind").asText()), decisionJsonFile + " decision kind is unknown");
        assertTrue(CATEGORIES.contains(decision.path("category").asText()), decisionJsonFile + " category is unknown");
        assertNonBlank(decisionJsonFile, decision, "method");
        assertTrue(decision.path("sourceLocation").isObject(), decisionJsonFile + " decision must contain sourceLocation");
        assertNonBlank(decisionJsonFile, decision.path("sourceLocation"), "file");
        assertTrue(decision.path("sourceLocation").path("line").asInt() > 0, decisionJsonFile + " line must be positive");
        assertTrue(decision.path("expression").isObject(), decisionJsonFile + " decision must contain expression");
        assertNonBlank(decisionJsonFile, decision.path("expression"), "text");
        assertTrue(decision.path("expression").has("normalized"), decisionJsonFile + " expression.normalized must be present");
        if (decision.path("expression").has("conditionExpression")) {
            assertConditionExpressionContract(decisionJsonFile, decision.path("expression").path("conditionExpression"));
        }
        assertTrue(decision.path("subjects").isArray(), decisionJsonFile + " subjects must be an array");
        assertTrue(decision.path("outcomes").isArray(), decisionJsonFile + " outcomes must be an array");
        assertFalse(decision.path("outcomes").isEmpty(), decisionJsonFile + " outcomes must not be empty");
        assertTrue(decision.path("evidence").isObject(), decisionJsonFile + " evidence must be an object");
        assertEquals("SOURCE_TEXT", decision.path("evidence").path("kind").asText(), decisionJsonFile + " evidence kind");
        assertNonBlank(decisionJsonFile, decision.path("evidence"), "snippet");
        assertTrue(decision.path("links").isObject(), decisionJsonFile + " links must be an object");
        assertTrue(decision.path("links").path("flowNodeIds").isArray(), decisionJsonFile + " links.flowNodeIds must be an array");
        assertTrue(decision.path("links").path("calledMethods").isArray(), decisionJsonFile + " links.calledMethods must be an array");
        assertTrue(decision.path("links").path("relatedBoundaries").isArray(), decisionJsonFile + " links.relatedBoundaries must be an array");
        assertNonBlank(decisionJsonFile, decision, "confidence");

        for (JsonNode outcome : decision.path("outcomes")) {
            assertNonBlank(decisionJsonFile, outcome, "when");
            assertTrue(ACTIONS.contains(outcome.path("action").asText()), decisionJsonFile + " outcome action is unknown");
            assertTrue(outcome.has("target"), decisionJsonFile + " outcome.target must be present");
            assertNonBlank(decisionJsonFile, outcome, "meaning");
        }
    }

    private static void assertNonBlank(Path file, JsonNode node, String fieldName) {
        assertTrue(node.has(fieldName), file + " missing field " + fieldName);
        assertFalse(node.path(fieldName).asText().isBlank(), file + " field " + fieldName + " must not be blank");
    }

    private static void assertConditionExpressionContract(Path decisionJsonFile, JsonNode expression) {
        assertNonBlank(decisionJsonFile, expression, "kind");
        assertNonBlank(decisionJsonFile, expression, "text");
        switch (expression.path("kind").asText()) {
            case "AND", "OR" -> {
                assertTrue(expression.path("operands").isArray(), decisionJsonFile + " boolean expression operands");
                assertFalse(expression.path("operands").isEmpty(), decisionJsonFile + " boolean expression operands");
                for (JsonNode operand : expression.path("operands")) {
                    assertConditionExpressionContract(decisionJsonFile, operand);
                }
            }
            case "NOT", "GROUP" -> {
                assertTrue(expression.path("operand").isObject(), decisionJsonFile + " unary expression operand");
                assertConditionExpressionContract(decisionJsonFile, expression.path("operand"));
            }
            case "COMPARISON" -> {
                assertNonBlank(decisionJsonFile, expression, "operator");
                assertNonBlank(decisionJsonFile, expression, "left");
                assertNonBlank(decisionJsonFile, expression, "right");
            }
            case "METHOD_CALL", "REFERENCE", "EXPRESSION" -> {
                // Leaf nodes only require deterministic kind and original text.
            }
            default -> throw new AssertionError(decisionJsonFile + " unknown condition expression kind");
        }
    }

    private static void assertUnresolvedContract(Path decisionJsonFile, JsonNode unresolved) {
        assertTrue(unresolved.path("id").asText().startsWith("unresolved:"), decisionJsonFile + " unresolved id must be stable");
        assertNonBlank(decisionJsonFile, unresolved, "kind");
        assertNonBlank(decisionJsonFile, unresolved, "method");
        assertTrue(unresolved.path("sourceLocation").isObject(), decisionJsonFile + " unresolved must contain sourceLocation");
        assertNonBlank(decisionJsonFile, unresolved.path("sourceLocation"), "file");
        assertTrue(unresolved.path("sourceLocation").path("line").asInt() > 0, decisionJsonFile + " line must be positive");
        assertNonBlank(decisionJsonFile, unresolved, "message");
        assertNonBlank(decisionJsonFile, unresolved, "expression");
    }

    private static void assertArtifactMatches(Path expectedDirectory, Path generatedDirectory, String fileName) throws Exception {
        assertEquals(
                Files.readString(expectedDirectory.resolve(fileName)),
                Files.readString(generatedDirectory.resolve(fileName)),
                fileName + " must match the expected fixture exactly"
        );
    }
}
