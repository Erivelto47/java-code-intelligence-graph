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

    private final JavaEarlyReturnDecisionExtractor earlyReturnDecisionExtractor;
    private final JavaUnsupportedDecisionShapeDetector unsupportedDecisionShapeDetector;

    public JavaIfThrowDecisionExtractor() {
        this(new JavaEarlyReturnDecisionExtractor(), new JavaUnsupportedDecisionShapeDetector());
    }

    JavaIfThrowDecisionExtractor(
            JavaEarlyReturnDecisionExtractor earlyReturnDecisionExtractor,
            JavaUnsupportedDecisionShapeDetector unsupportedDecisionShapeDetector
    ) {
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
                        "phase-4.2.1-java-block-throw-with-pre-statements",
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
        if (!ifStatement.hasBlock() || ifStatement.hasElse()) {
            return Optional.empty();
        }

        Optional<JavaDecisionSourceSupport.ThrowStatement> throwStatement =
                JavaDecisionSourceSupport.parseFinalThrowWithAllowedPreStatements(
                        sourceFile,
                        ifStatement.bodyStart(),
                        ifStatement.bodyEnd()
                );
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
