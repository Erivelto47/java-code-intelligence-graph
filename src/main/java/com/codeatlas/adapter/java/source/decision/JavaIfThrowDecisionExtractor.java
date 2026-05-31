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
import java.util.regex.Pattern;

public final class JavaIfThrowDecisionExtractor {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;
    private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern SIMPLE_ARGUMENT = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

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
        DecisionContext directContext = DecisionContext.forEntrypoint(entrypoint);
        ExtractionResult directResult = extractDirectDecisions(directContext, sourceFile, methodRange, 1, 1);
        ExtractionResult helperResult = extractMethodLocalDecisionCalls(
                entrypoint,
                sourceFile,
                methodRange,
                directResult.decisions().size() + 1,
                directResult.unresolved().size() + 1
        );
        List<DecisionNode> decisions = new ArrayList<>(directResult.decisions());
        decisions.addAll(helperResult.decisions());
        List<UnresolvedDecision> unresolved = new ArrayList<>(directResult.unresolved());
        unresolved.addAll(helperResult.unresolved());
        return new ExtractionResult(List.copyOf(decisions), List.copyOf(unresolved));
    }

    private ExtractionResult extractDirectDecisions(
            DecisionContext context,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange,
            int startingDecisionOrdinal,
            int startingUnresolvedOrdinal
    ) {
        List<DecisionNode> decisions = new ArrayList<>();
        List<UnresolvedDecision> unresolved = new ArrayList<>();
        String maskedSource = sourceFile.maskedSource();
        int index = methodRange.bodyStart();
        int decisionOrdinal = startingDecisionOrdinal;
        int unresolvedOrdinal = startingUnresolvedOrdinal;
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
                decisions.add(toThrowDecisionNode(context, sourceFile, ifThrowDecision.get(), decisionOrdinal));
                decisionOrdinal++;
                index = ifStatement.statementEnd();
                continue;
            }

            Optional<JavaIfElseDecisionExtractor.IfElseDecision> ifElseDecision =
                    ifElseDecisionExtractor.parse(sourceFile, ifStatement);
            if (ifElseDecision.isPresent()) {
                decisions.add(toIfElseDecisionNode(context, sourceFile, ifElseDecision.get(), decisionOrdinal));
                decisionOrdinal++;
                index = ifStatement.statementEnd();
                continue;
            }

            Optional<JavaEarlyReturnDecisionExtractor.EarlyReturnDecision> earlyReturnDecision =
                    earlyReturnDecisionExtractor.parse(sourceFile, ifStatement);
            if (earlyReturnDecision.isPresent()) {
                decisions.add(toEarlyReturnDecisionNode(
                        context,
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
                unresolved.add(toUnresolvedDecision(context, sourceFile, unsupportedShape.get(), unresolvedOrdinal));
                unresolvedOrdinal++;
            }
            index = ifStatement.statementEnd();
        }
        return new ExtractionResult(List.copyOf(decisions), List.copyOf(unresolved));
    }

    private ExtractionResult extractMethodLocalDecisionCalls(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange,
            int startingDecisionOrdinal,
            int startingUnresolvedOrdinal
    ) {
        List<DecisionNode> decisions = new ArrayList<>();
        List<UnresolvedDecision> unresolved = new ArrayList<>();
        int decisionOrdinal = startingDecisionOrdinal;
        int unresolvedOrdinal = startingUnresolvedOrdinal;

        for (LocalCallStatement call : findTopLevelCallStatements(sourceFile, methodRange)) {
            if (!call.helperLike()) {
                continue;
            }
            if (call.qualified()) {
                unresolved.add(toUnresolvedLocalCall(
                        entrypoint,
                        sourceFile,
                        call,
                        unresolvedOrdinal,
                        "UNRESOLVED_LOCAL_HELPER_CROSS_OBJECT",
                        "Method-local decision helper call is qualified and requires cross-object or cross-class resolution"
                ));
                unresolvedOrdinal++;
                continue;
            }
            if (!call.simpleArguments()) {
                unresolved.add(toUnresolvedLocalCall(
                        entrypoint,
                        sourceFile,
                        call,
                        unresolvedOrdinal,
                        "UNRESOLVED_LOCAL_HELPER_ARGUMENTS",
                        "Method-local decision helper call has arguments that require binding or data-flow analysis"
                ));
                unresolvedOrdinal++;
                continue;
            }

            List<JavaDecisionSourceSupport.MethodRange> targets = JavaDecisionSourceSupport.findMethodsByName(
                    sourceFile,
                    call.methodName()
            );
            if (targets.isEmpty()) {
                unresolved.add(toUnresolvedLocalCall(
                        entrypoint,
                        sourceFile,
                        call,
                        unresolvedOrdinal,
                        "UNRESOLVED_LOCAL_HELPER_TARGET",
                        "Method-local decision helper call has no unique same-class target method"
                ));
                unresolvedOrdinal++;
                continue;
            }
            if (targets.size() > 1) {
                unresolved.add(toUnresolvedLocalCall(
                        entrypoint,
                        sourceFile,
                        call,
                        unresolvedOrdinal,
                        "UNRESOLVED_LOCAL_HELPER_OVERLOAD",
                        "Method-local decision helper call is overloaded and cannot be safely resolved"
                ));
                unresolvedOrdinal++;
                continue;
            }
            if (targets.get(0).methodNameStart() == methodRange.methodNameStart()) {
                unresolved.add(toUnresolvedLocalCall(
                        entrypoint,
                        sourceFile,
                        call,
                        unresolvedOrdinal,
                        "UNRESOLVED_LOCAL_HELPER_RECURSION",
                        "Method-local decision helper call is recursive and outside Phase 4.4 scope"
                ));
                unresolvedOrdinal++;
                continue;
            }

            DecisionContext helperContext = DecisionContext.forLocalHelper(entrypoint, call.methodName());
            ExtractionResult helperResult = extractDirectDecisions(
                    helperContext,
                    sourceFile,
                    targets.get(0),
                    decisionOrdinal,
                    unresolvedOrdinal
            );
            decisions.addAll(helperResult.decisions());
            unresolved.addAll(helperResult.unresolved());
            decisionOrdinal += helperResult.decisions().size();
            unresolvedOrdinal += helperResult.unresolved().size();
        }

        return new ExtractionResult(List.copyOf(decisions), List.copyOf(unresolved));
    }

    private static List<LocalCallStatement> findTopLevelCallStatements(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange
    ) {
        List<LocalCallStatement> calls = new ArrayList<>();
        String maskedSource = sourceFile.maskedSource();
        int index = JavaDecisionSourceSupport.skipWhitespace(maskedSource, methodRange.bodyStart(), methodRange.bodyEnd());
        while (index < methodRange.bodyEnd()) {
            if (JavaDecisionSourceSupport.startsWithWord(maskedSource, index, "if")) {
                Optional<JavaDecisionSourceSupport.IfStatement> ifStatement = JavaDecisionSourceSupport.parseIfStatement(
                        sourceFile,
                        methodRange,
                        index
                );
                if (ifStatement.isPresent()) {
                    index = JavaDecisionSourceSupport.skipWhitespace(
                            maskedSource,
                            ifStatement.get().statementEnd(),
                            methodRange.bodyEnd()
                    );
                    continue;
                }
            }

            int semicolon = JavaDecisionSourceSupport.findTopLevelSemicolon(maskedSource, index, methodRange.bodyEnd());
            if (semicolon < 0) {
                break;
            }
            parseCallStatement(sourceFile, index, semicolon).ifPresent(calls::add);
            index = JavaDecisionSourceSupport.skipWhitespace(maskedSource, semicolon + 1, methodRange.bodyEnd());
        }
        return List.copyOf(calls);
    }

    private static Optional<LocalCallStatement> parseCallStatement(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int semicolon
    ) {
        String expression = sourceFile.source().substring(statementStart, semicolon).trim();
        if (expression.isBlank()
                || expression.contains("=")
                || expression.contains("->")
                || expression.contains("::")) {
            return Optional.empty();
        }

        int openParen = expression.indexOf('(');
        if (openParen <= 0 || !expression.endsWith(")")) {
            return Optional.empty();
        }
        String callee = expression.substring(0, openParen).trim();
        String methodName = callee.contains(".")
                ? callee.substring(callee.lastIndexOf('.') + 1)
                : callee;
        if (!SIMPLE_NAME.matcher(methodName).matches()) {
            return Optional.empty();
        }

        String arguments = expression.substring(openParen + 1, expression.length() - 1).trim();
        return Optional.of(new LocalCallStatement(
                statementStart,
                JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(statementStart, semicolon + 1)),
                methodName,
                callee.contains("."),
                hasSimpleArguments(arguments),
                helperLikeMethodName(methodName)
        ));
    }

    private static boolean hasSimpleArguments(String arguments) {
        if (arguments.isBlank()) {
            return true;
        }
        for (String argument : arguments.split(",")) {
            if (!SIMPLE_ARGUMENT.matcher(argument.trim()).matches()) {
                return false;
            }
        }
        return true;
    }

    private static boolean helperLikeMethodName(String methodName) {
        String lower = methodName.toLowerCase();
        return lower.startsWith("validate")
                || lower.startsWith("check")
                || lower.startsWith("ensure")
                || lower.startsWith("guard")
                || lower.startsWith("require")
                || lower.startsWith("assert");
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
            DecisionContext context,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            IfThrowDecision parsedDecision,
            int ordinal
    ) {
        DecisionCategory category = category(parsedDecision.condition(), parsedDecision.exceptionType());
        String normalizedCondition = JavaDecisionSourceSupport.normalizedCondition(parsedDecision.condition());
        List<DecisionSubject> subjects = subjects(parsedDecision.condition());
        String throwMeaning = category == DecisionCategory.VALIDATION
                ? "Input is rejected by this check"
                : "Condition throws " + parsedDecision.exceptionType();

        return new DecisionNode(
                "decision:" + context.idBase() + ":if-throw:" + ordinal,
                DecisionKind.CONDITIONAL_THROW,
                category,
                context.methodSignature(),
                new DecisionSource(context.className(), context.methodName(), context.methodSignature()),
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
                new DecisionLinks(List.of(), context.calledMethods(), List.of()),
                "HIGH"
        );
    }

    private static DecisionNode toEarlyReturnDecisionNode(
            DecisionContext context,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaEarlyReturnDecisionExtractor.EarlyReturnDecision parsedDecision,
            int ordinal
    ) {
        String normalizedCondition = JavaDecisionSourceSupport.normalizedCondition(parsedDecision.condition());
        String returnMeaning = parsedDecision.returnExpression().isBlank()
                ? "Execution returns from this check"
                : "Returns " + parsedDecision.returnExpression() + " from this check";

        return new DecisionNode(
                "decision:" + context.idBase() + ":if-return:" + ordinal,
                DecisionKind.EARLY_RETURN,
                DecisionCategory.UNKNOWN,
                context.methodSignature(),
                new DecisionSource(context.className(), context.methodName(), context.methodSignature()),
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
                new DecisionLinks(List.of(), context.calledMethods(), List.of()),
                "HIGH"
        );
    }

    private static DecisionNode toIfElseDecisionNode(
            DecisionContext context,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaIfElseDecisionExtractor.IfElseDecision parsedDecision,
            int ordinal
    ) {
        String normalizedCondition = JavaDecisionSourceSupport.normalizedCondition(parsedDecision.condition());

        return new DecisionNode(
                "decision:" + context.idBase() + ":if-else:" + ordinal,
                DecisionKind.IF_ELSE_CONDITION,
                DecisionCategory.UNKNOWN,
                context.methodSignature(),
                new DecisionSource(context.className(), context.methodName(), context.methodSignature()),
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(parsedDecision.ifStart())),
                new DecisionCondition(parsedDecision.condition(), normalizedCondition),
                subjects(parsedDecision.condition()),
                List.of(
                        toBranchOutcome("true", parsedDecision.trueOutcome()),
                        toBranchOutcome("false", parsedDecision.falseOutcome())
                ),
                new DecisionEvidence("SOURCE_TEXT", parsedDecision.snippet()),
                new DecisionLinks(List.of(), context.calledMethods(), List.of()),
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
            DecisionContext context,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaUnsupportedDecisionShapeDetector.UnsupportedDecisionShape unsupportedShape,
            int ordinal
    ) {
        return new UnresolvedDecision(
                "unresolved:" + context.idBase() + ":if:" + ordinal,
                unsupportedShape.kind(),
                context.methodSignature(),
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(unsupportedShape.ifStart())),
                unsupportedShape.message(),
                unsupportedShape.snippet()
        );
    }

    private static UnresolvedDecision toUnresolvedLocalCall(
            JavaDecisionSourceSupport.Entrypoint entrypoint,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            LocalCallStatement call,
            int ordinal,
            String kind,
            String message
    ) {
        String method = entrypoint.normalized();
        return new UnresolvedDecision(
                "unresolved:" + method + ":local-helper-call:" + ordinal,
                kind,
                method,
                new DecisionSourceLocation(sourceFile.relativePath(), sourceFile.lineOf(call.statementStart())),
                message,
                call.snippet()
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

    private record DecisionContext(
            String idBase,
            String className,
            String methodName,
            String methodSignature,
            List<String> calledMethods
    ) {
        static DecisionContext forEntrypoint(JavaDecisionSourceSupport.Entrypoint entrypoint) {
            return new DecisionContext(
                    entrypoint.normalized(),
                    entrypoint.classQualifiedName(),
                    entrypoint.methodName(),
                    entrypoint.normalized(),
                    List.of()
            );
        }

        static DecisionContext forLocalHelper(
                JavaDecisionSourceSupport.Entrypoint entrypoint,
                String helperMethodName
        ) {
            String helperSignature = entrypoint.classQualifiedName() + "." + helperMethodName;
            return new DecisionContext(
                    entrypoint.normalized() + ":local-helper:" + helperMethodName,
                    entrypoint.classQualifiedName(),
                    helperMethodName,
                    helperSignature,
                    List.of(helperSignature)
            );
        }
    }

    private record LocalCallStatement(
            int statementStart,
            String snippet,
            String methodName,
            boolean qualified,
            boolean simpleArguments,
            boolean helperLike
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
