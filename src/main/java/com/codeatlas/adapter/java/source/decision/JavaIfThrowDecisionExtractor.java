package com.codeatlas.adapter.java.source.decision;

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

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JavaIfThrowDecisionExtractor {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;

    private final JavaIfElseDecisionExtractor ifElseDecisionExtractor;
    private final JavaEarlyReturnDecisionExtractor earlyReturnDecisionExtractor;
    private final JavaUnsupportedDecisionShapeDetector unsupportedDecisionShapeDetector;

    public JavaIfThrowDecisionExtractor() {
        this(
                new JavaIfElseDecisionExtractor(),
                new JavaEarlyReturnDecisionExtractor(),
                new JavaUnsupportedDecisionShapeDetector()
        );
    }

    JavaIfThrowDecisionExtractor(
            JavaIfElseDecisionExtractor ifElseDecisionExtractor,
            JavaEarlyReturnDecisionExtractor earlyReturnDecisionExtractor,
            JavaUnsupportedDecisionShapeDetector unsupportedDecisionShapeDetector
    ) {
        this.ifElseDecisionExtractor = Objects.requireNonNull(
                ifElseDecisionExtractor,
                "ifElseDecisionExtractor must not be null"
        );
        this.earlyReturnDecisionExtractor = Objects.requireNonNull(
                earlyReturnDecisionExtractor,
                "earlyReturnDecisionExtractor must not be null"
        );
        this.unsupportedDecisionShapeDetector = Objects.requireNonNull(
                unsupportedDecisionShapeDetector,
                "unsupportedDecisionShapeDetector must not be null"
        );
    }

    public DecisionTrace analyze(Path projectPath, String entrypoint) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        JavaDecisionSourceSupport.Entrypoint parsedEntrypoint = JavaDecisionSourceSupport.Entrypoint.parse(entrypoint);
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();

        JavaDecisionSourceSupport.SourceFile sourceFile = JavaDecisionSourceSupport
                .findSourceFile(normalizedProjectPath, parsedEntrypoint)
                .orElseThrow(() -> new IllegalArgumentException("Could not find Java type "
                        + parsedEntrypoint.classQualifiedName()
                        + " under " + normalizedProjectPath));
        JavaDecisionSourceSupport.MethodRange methodRange = JavaDecisionSourceSupport.findMethod(sourceFile, parsedEntrypoint)
                .orElseThrow(() -> new IllegalArgumentException("Could not find method "
                        + parsedEntrypoint.methodName()
                        + " in "
                        + parsedEntrypoint.classQualifiedName()
                        + " at "
                        + sourceFile.relativePath()));

        ExtractionResult extractionResult = extract(parsedEntrypoint, sourceFile, methodRange);
        return new DecisionTrace(
                SCHEMA_VERSION,
                DETERMINISTIC_GENERATED_AT,
                JavaDecisionSourceSupport.portablePath(projectPath),
                new DecisionScope("ENTRYPOINT", parsedEntrypoint.normalized(), null),
                new DecisionArtifactSource(null, null),
                extractionResult.decisions(),
                extractionResult.unresolved(),
                new DecisionTraceMetadata(
                        "java-source-text-decision-extractor",
                        "phase-4.3.1-java-mixed-throw-return-if-else",
                        true,
                        "source-text"
                )
        );
    }

    private ExtractionResult extract(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange
    ) {
        List<DecisionNode> decisions = new ArrayList<>();
        List<UnresolvedDecision> unresolved = new ArrayList<>();
        String maskedSource = sourceFile.maskedSource();
        int index = methodRange.bodyStart();
        int decisionOrdinal = 1;
        int unresolvedOrdinal = 1;
        while (index < methodRange.bodyEnd()) {
            int ifStart = JavaDecisionSourceSupport.indexOfWord(maskedSource, "if", index, methodRange.bodyEnd());
            if (ifStart < 0) {
                break;
            }

            Optional<JavaDecisionSourceSupport.IfStatement> parsedIf = JavaDecisionSourceSupport.parseIfStatement(
                    sourceFile,
                    methodRange,
                    ifStart
            );
            if (parsedIf.isEmpty()) {
                index = ifStart + 2;
                continue;
            }

            JavaDecisionSourceSupport.IfStatement ifStatement = parsedIf.get();
            Optional<IfThrowDecision> ifThrowDecision = parseIfThrow(sourceFile, ifStatement);
            if (ifThrowDecision.isPresent()) {
                decisions.add(toThrowDecisionNode(entrypoint, sourceFile, ifThrowDecision.get(), decisionOrdinal));
                decisionOrdinal++;
                index = ifStatement.statementEnd();
                continue;
            }

            Optional<JavaIfElseDecisionExtractor.IfElseDecision> ifElseDecision =
                    ifElseDecisionExtractor.parse(sourceFile, ifStatement);
            if (ifElseDecision.isPresent()) {
                decisions.add(toIfElseDecisionNode(entrypoint, sourceFile, ifElseDecision.get(), decisionOrdinal));
                decisionOrdinal++;
                index = ifStatement.statementEnd();
                continue;
            }

            Optional<JavaEarlyReturnDecisionExtractor.EarlyReturnDecision> earlyReturnDecision =
                    earlyReturnDecisionExtractor.parse(sourceFile, ifStatement);
            if (earlyReturnDecision.isPresent()) {
                decisions.add(toEarlyReturnDecisionNode(
                        entrypoint,
                        sourceFile,
                        earlyReturnDecision.get(),
                        decisionOrdinal
                ));
                decisionOrdinal++;
                index = ifStatement.statementEnd();
                continue;
            }

            Optional<JavaUnsupportedDecisionShapeDetector.UnsupportedDecisionShape> unsupportedShape =
                    unsupportedDecisionShapeDetector.detect(sourceFile, ifStatement);
            if (unsupportedShape.isPresent()) {
                unresolved.add(toUnresolvedDecision(entrypoint, sourceFile, unsupportedShape.get(), unresolvedOrdinal));
                unresolvedOrdinal++;
            }
            index = ifStatement.statementEnd();
        }
        return new ExtractionResult(List.copyOf(decisions), List.copyOf(unresolved));
    }

    private static Optional<IfThrowDecision> parseIfThrow(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        if (ifStatement.hasElse()) {
            return Optional.empty();
        }

        Optional<JavaDecisionSourceSupport.ThrowStatement> throwStatement = ifStatement.hasBlock()
                ? JavaDecisionSourceSupport.parseFinalThrowWithAllowedPreStatements(
                        sourceFile,
                        ifStatement.bodyStart(),
                        ifStatement.bodyEnd()
                )
                : JavaDecisionSourceSupport.parseDirectThrow(
                        sourceFile,
                        ifStatement.bodyStart(),
                        ifStatement.bodyEnd()
                ).filter(JavaDecisionSourceSupport.ThrowStatement::hasDirectLiteralMessage);
        if (throwStatement.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new IfThrowDecision(
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                ifStatement.condition(),
                ifStatement.snippet(),
                throwStatement.get().exceptionType(),
                throwStatement.get().message()
        ));
    }

    private static DecisionNode toThrowDecisionNode(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            IfThrowDecision parsedDecision,
            int ordinal
    ) {
        String method = entrypoint.normalized();
        DecisionCategory category = category(parsedDecision.condition(), parsedDecision.exceptionType());
        String normalizedCondition = JavaDecisionSourceSupport.normalizedCondition(parsedDecision.condition());
        List<DecisionSubject> subjects = subjects(parsedDecision.condition());
        String throwMeaning = category == DecisionCategory.VALIDATION
                ? "Input is rejected by this check"
                : "Condition throws " + parsedDecision.exceptionType();

        return new DecisionNode(
                "decision:" + method + ":if-throw:" + ordinal,
                DecisionKind.CONDITIONAL_THROW,
                category,
                method,
                new DecisionSource(entrypoint.classQualifiedName(), entrypoint.methodName(), method),
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(parsedDecision.ifStart())),
                new DecisionCondition(parsedDecision.condition(), normalizedCondition),
                subjects,
                List.of(
                        new DecisionOutcome(
                                "true",
                                DecisionOutcomeAction.THROW,
                                parsedDecision.exceptionType(),
                                parsedDecision.exceptionType(),
                                parsedDecision.message(),
                                throwMeaning
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
                new DecisionEvidence("SOURCE_TEXT", parsedDecision.snippet()),
                new DecisionLinks(List.of(), List.of(), List.of()),
                "HIGH"
        );
    }

    private static DecisionNode toEarlyReturnDecisionNode(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaEarlyReturnDecisionExtractor.EarlyReturnDecision parsedDecision,
            int ordinal
    ) {
        String method = entrypoint.normalized();
        String normalizedCondition = JavaDecisionSourceSupport.normalizedCondition(parsedDecision.condition());
        String returnMeaning = parsedDecision.returnExpression().isBlank()
                ? "Execution returns from this check"
                : "Returns " + parsedDecision.returnExpression() + " from this check";

        return new DecisionNode(
                "decision:" + method + ":if-return:" + ordinal,
                DecisionKind.EARLY_RETURN,
                DecisionCategory.UNKNOWN,
                method,
                new DecisionSource(entrypoint.classQualifiedName(), entrypoint.methodName(), method),
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(parsedDecision.ifStart())),
                new DecisionCondition(parsedDecision.condition(), normalizedCondition),
                subjects(parsedDecision.condition()),
                List.of(
                        new DecisionOutcome(
                                "true",
                                DecisionOutcomeAction.RETURN,
                                parsedDecision.returnTarget(),
                                null,
                                null,
                                returnMeaning
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
                new DecisionEvidence("SOURCE_TEXT", parsedDecision.snippet()),
                new DecisionLinks(List.of(), List.of(), List.of()),
                "HIGH"
        );
    }

    private static DecisionNode toIfElseDecisionNode(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaIfElseDecisionExtractor.IfElseDecision parsedDecision,
            int ordinal
    ) {
        String method = entrypoint.normalized();
        String normalizedCondition = JavaDecisionSourceSupport.normalizedCondition(parsedDecision.condition());

        return new DecisionNode(
                "decision:" + method + ":if-else:" + ordinal,
                DecisionKind.IF_ELSE_CONDITION,
                DecisionCategory.UNKNOWN,
                method,
                new DecisionSource(entrypoint.classQualifiedName(), entrypoint.methodName(), method),
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(parsedDecision.ifStart())),
                new DecisionCondition(parsedDecision.condition(), normalizedCondition),
                subjects(parsedDecision.condition()),
                List.of(
                        toBranchOutcome("true", parsedDecision.trueOutcome()),
                        toBranchOutcome("false", parsedDecision.falseOutcome())
                ),
                new DecisionEvidence("SOURCE_TEXT", parsedDecision.snippet()),
                new DecisionLinks(List.of(), List.of(), List.of()),
                "HIGH"
        );
    }

    private static DecisionOutcome toBranchOutcome(
            String when,
            JavaIfElseDecisionExtractor.BranchOutcome branchOutcome
    ) {
        DecisionOutcomeAction action = DecisionOutcomeAction.valueOf(branchOutcome.action().name());
        return new DecisionOutcome(
                when,
                action,
                branchOutcome.target(),
                branchOutcome.exceptionType(),
                branchOutcome.message(),
                branchMeaning(branchOutcome)
        );
    }

    private static UnresolvedDecision toUnresolvedDecision(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaUnsupportedDecisionShapeDetector.UnsupportedDecisionShape unsupportedShape,
            int ordinal
    ) {
        String method = entrypoint.normalized();
        return new UnresolvedDecision(
                "unresolved:" + method + ":if:" + ordinal,
                unsupportedShape.kind(),
                method,
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(unsupportedShape.ifStart())),
                unsupportedShape.message(),
                unsupportedShape.snippet()
        );
    }

    private static String returnMeaning(String returnExpression) {
        return returnExpression.isBlank()
                ? "Returns from this branch"
                : "Returns " + returnExpression + " from this branch";
    }

    private static String branchMeaning(JavaIfElseDecisionExtractor.BranchOutcome branchOutcome) {
        if (branchOutcome.action().name().equals(DecisionOutcomeAction.RETURN.name())) {
            return returnMeaning(branchOutcome.returnExpression());
        }
        String meaning = "Throws " + branchOutcome.exceptionType() + " from this branch";
        if (branchOutcome.message() != null && !branchOutcome.message().isBlank()) {
            meaning += ": " + branchOutcome.message();
        }
        return meaning;
    }

    private static List<DecisionSubject> subjects(String condition) {
        Optional<String> subjectName = JavaDecisionSourceSupport.subjectName(condition);
        return subjectName
                .map(name -> List.of(new DecisionSubject(name, "INPUT_FIELD")))
                .orElseGet(List::of);
    }

    private static DecisionCategory category(String condition, String exceptionType) {
        String lowerCondition = condition.toLowerCase();
        String lowerException = exceptionType.toLowerCase();
        if (lowerCondition.contains("request.")
                || lowerException.contains("invalid")
                || lowerException.contains("illegal")
                || lowerException.contains("required")
                || lowerException.contains("alreadyexists")
                || lowerException.contains("already_exists")) {
            return DecisionCategory.VALIDATION;
        }
        return DecisionCategory.UNKNOWN;
    }

    private record ExtractionResult(
            List<DecisionNode> decisions,
            List<UnresolvedDecision> unresolved
    ) {
    }

    private record IfThrowDecision(
            int ifStart,
            int blockEnd,
            String condition,
            String snippet,
            String exceptionType,
            String message
    ) {
    }
}
