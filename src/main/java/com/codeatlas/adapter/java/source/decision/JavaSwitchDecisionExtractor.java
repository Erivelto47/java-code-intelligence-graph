package com.codeatlas.adapter.java.source.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JavaSwitchDecisionExtractor {
    List<SwitchDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange
    ) {
        List<SwitchDecision> decisions = new ArrayList<>();
        String maskedSource = sourceFile.maskedSource();
        int index = methodRange.bodyStart();
        while (index < methodRange.bodyEnd()) {
            int switchStart = JavaDecisionSourceSupport.indexOfWord(maskedSource, "switch", index, methodRange.bodyEnd());
            if (switchStart < 0) {
                break;
            }

            Optional<SwitchDecision> parsed = parseSwitch(sourceFile, methodRange, switchStart);
            if (parsed.isPresent()) {
                decisions.add(parsed.get());
                index = parsed.get().statementEnd();
            } else {
                index = switchStart + "switch".length();
            }
        }
        return List.copyOf(decisions);
    }

    private static Optional<SwitchDecision> parseSwitch(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange,
            int switchStart
    ) {
        String maskedSource = sourceFile.maskedSource();
        int selectorOpen = JavaDecisionSourceSupport.skipWhitespace(
                maskedSource,
                switchStart + "switch".length(),
                methodRange.bodyEnd()
        );
        if (selectorOpen >= methodRange.bodyEnd() || maskedSource.charAt(selectorOpen) != '(') {
            return Optional.empty();
        }
        int selectorClose = JavaDecisionSourceSupport.findMatching(maskedSource, selectorOpen, '(', ')');
        if (selectorClose < 0 || selectorClose >= methodRange.bodyEnd()) {
            return Optional.empty();
        }
        int bodyOpen = JavaDecisionSourceSupport.skipWhitespace(maskedSource, selectorClose + 1, methodRange.bodyEnd());
        if (bodyOpen >= methodRange.bodyEnd() || maskedSource.charAt(bodyOpen) != '{') {
            return Optional.empty();
        }
        int bodyClose = JavaDecisionSourceSupport.findMatching(maskedSource, bodyOpen, '{', '}');
        if (bodyClose < 0 || bodyClose > methodRange.bodyEnd()) {
            return Optional.empty();
        }

        int statementStart = statementStart(maskedSource, methodRange.bodyStart(), switchStart);
        int afterBody = JavaDecisionSourceSupport.skipWhitespace(maskedSource, bodyClose + 1, methodRange.bodyEnd());
        boolean expression = afterBody < methodRange.bodyEnd() && maskedSource.charAt(afterBody) == ';';
        int statementEnd = expression ? afterBody + 1 : bodyClose + 1;
        SwitchContext context = expression
                ? expressionContext(sourceFile, statementStart, switchStart)
                : new SwitchContext(SwitchUsage.STATEMENT, null);

        String selector = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(selectorOpen + 1, selectorClose)
        );
        String snippet = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(statementStart, statementEnd)
        );
        List<SwitchCase> cases = parseCases(sourceFile, bodyOpen, bodyClose, context);
        if (cases.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SwitchDecision(
                switchStart,
                statementEnd,
                selector,
                snippet,
                expression,
                context.assignedTo(),
                cases
        ));
    }

    private static SwitchContext expressionContext(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int switchStart
    ) {
        String prefix = sourceFile.source().substring(statementStart, switchStart);
        String maskedPrefix = sourceFile.maskedSource().substring(statementStart, switchStart);
        if (JavaDecisionSourceSupport.startsWithWord(maskedPrefix, leadingWhitespaceLength(maskedPrefix), "return")) {
            return new SwitchContext(SwitchUsage.RETURN_EXPRESSION, null);
        }

        int assignment = findLastAssignment(maskedPrefix);
        if (assignment >= 0) {
            String left = prefix.substring(0, assignment).trim();
            return new SwitchContext(SwitchUsage.ASSIGNMENT_EXPRESSION, assignmentTarget(left));
        }
        return new SwitchContext(SwitchUsage.EXPRESSION, null);
    }

    private static List<SwitchCase> parseCases(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int bodyOpen,
            int bodyClose,
            SwitchContext context
    ) {
        List<CaseHeader> headers = new ArrayList<>();
        int index = bodyOpen + 1;
        while (index < bodyClose) {
            Optional<CaseHeader> header = findNextCaseHeader(sourceFile, index, bodyClose);
            if (header.isEmpty()) {
                break;
            }
            headers.add(header.get());
            index = header.get().bodyStart();
        }

        List<SwitchCase> cases = new ArrayList<>();
        List<String> pendingFallThroughLabels = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            CaseHeader header = headers.get(i);
            int caseBodyEnd = i + 1 < headers.size() ? headers.get(i + 1).labelStart() : bodyClose;
            List<String> labels = new ArrayList<>(pendingFallThroughLabels);
            labels.addAll(header.labels());

            if (!header.defaultCase()
                    && !header.arrowCase()
                    && isBlank(sourceFile.maskedSource(), header.bodyStart(), caseBodyEnd)) {
                pendingFallThroughLabels = labels;
                continue;
            }

            List<DecisionBranchOutcome> outcomes = branchOutcomes(
                    sourceFile,
                    header.bodyStart(),
                    caseBodyEnd,
                    header.defaultCase(),
                    labels,
                    context
            );
            cases.add(new SwitchCase(
                    header.defaultCase() ? "DEFAULT" : "CASE",
                    List.copyOf(labels),
                    cases.size() + 1,
                    header.bodyStart(),
                    caseBodyEnd,
                    outcomes
            ));
            pendingFallThroughLabels = new ArrayList<>();
        }
        return List.copyOf(cases);
    }

    private static Optional<CaseHeader> findNextCaseHeader(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int start,
            int limit
    ) {
        String maskedSource = sourceFile.maskedSource();
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = start; index < limit; index++) {
            char current = maskedSource.charAt(index);
            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            }
            if (parenDepth != 0 || bracketDepth != 0 || braceDepth != 0) {
                continue;
            }
            if (JavaDecisionSourceSupport.startsWithWord(maskedSource, index, "case")) {
                return parseCaseHeader(sourceFile, index, limit);
            }
            if (JavaDecisionSourceSupport.startsWithWord(maskedSource, index, "default")) {
                return parseDefaultHeader(sourceFile, index, limit);
            }
        }
        return Optional.empty();
    }

    private static Optional<CaseHeader> parseCaseHeader(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int labelStart,
            int limit
    ) {
        int labelsStart = JavaDecisionSourceSupport.skipWhitespace(
                sourceFile.maskedSource(),
                labelStart + "case".length(),
                limit
        );
        Optional<CaseTerminator> terminator = findCaseTerminator(sourceFile.maskedSource(), labelsStart, limit);
        if (terminator.isEmpty()) {
            return Optional.empty();
        }
        String labelsText = sourceFile.source().substring(labelsStart, terminator.get().start()).trim();
        List<String> labels = splitLabels(labelsText);
        if (labels.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CaseHeader(
                labelStart,
                terminator.get().end(),
                terminator.get().end(),
                false,
                terminator.get().arrow(),
                labels
        ));
    }

    private static Optional<CaseHeader> parseDefaultHeader(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int labelStart,
            int limit
    ) {
        int afterDefault = JavaDecisionSourceSupport.skipWhitespace(
                sourceFile.maskedSource(),
                labelStart + "default".length(),
                limit
        );
        Optional<CaseTerminator> terminator = findDefaultTerminator(sourceFile.maskedSource(), afterDefault, limit);
        return terminator.map(value -> new CaseHeader(
                labelStart,
                value.end(),
                value.end(),
                true,
                value.arrow(),
                List.of()
        ));
    }

    private static Optional<CaseTerminator> findDefaultTerminator(String source, int start, int limit) {
        int index = JavaDecisionSourceSupport.skipWhitespace(source, start, limit);
        if (index >= limit) {
            return Optional.empty();
        }
        if (source.charAt(index) == ':') {
            return Optional.of(new CaseTerminator(index, index + 1, false));
        }
        if (index + 1 < limit && source.charAt(index) == '-' && source.charAt(index + 1) == '>') {
            return Optional.of(new CaseTerminator(index, index + 2, true));
        }
        return Optional.empty();
    }

    private static Optional<CaseTerminator> findCaseTerminator(String source, int start, int limit) {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = start; index < limit; index++) {
            char current = source.charAt(index);
            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            } else if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                if (current == ':') {
                    return Optional.of(new CaseTerminator(index, index + 1, false));
                }
                if (current == '-' && index + 1 < limit && source.charAt(index + 1) == '>') {
                    return Optional.of(new CaseTerminator(index, index + 2, true));
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> splitLabels(String labelsText) {
        List<String> labels = new ArrayList<>();
        int start = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = 0; index < labelsText.length(); index++) {
            char current = labelsText.charAt(index);
            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            } else if (current == ',' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                addLabel(labels, labelsText.substring(start, index));
                start = index + 1;
            }
        }
        addLabel(labels, labelsText.substring(start));
        return List.copyOf(labels);
    }

    private static void addLabel(List<String> labels, String label) {
        String normalized = JavaDecisionSourceSupport.normalizeWhitespace(label);
        if (!normalized.isBlank()) {
            labels.add(normalized);
        }
    }

    private static List<DecisionBranchOutcome> branchOutcomes(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int bodyStart,
            int bodyEnd,
            boolean defaultCase,
            List<String> labels,
            SwitchContext context
    ) {
        Optional<BranchExpression> branchExpression = parseBranchExpression(sourceFile, bodyStart, bodyEnd);
        if (branchExpression.isPresent()) {
            return List.of(toExpressionOutcome(defaultCase, labels, context, branchExpression.get()));
        }

        Optional<JavaDecisionSourceSupport.ReturnStatement> returnStatement = JavaDecisionSourceSupport.parseDirectReturn(
                sourceFile,
                bodyStart,
                bodyEnd
        );
        if (returnStatement.isPresent()) {
            String expression = returnStatement.get().expression();
            return List.of(new DecisionBranchOutcome(
                    DecisionOutcomeKind.RETURN,
                    expression,
                    JavaDecisionSourceSupport.returnTarget(expression),
                    null,
                    null,
                    "Switch branch returns " + expression
            ));
        }

        Optional<JavaDecisionSourceSupport.ThrowStatement> throwStatement = JavaDecisionSourceSupport.parseDirectThrow(
                sourceFile,
                bodyStart,
                bodyEnd
        ).filter(JavaDecisionSourceSupport.ThrowStatement::hasDirectLiteralMessage);
        if (throwStatement.isPresent()) {
            return List.of(new DecisionBranchOutcome(
                    DecisionOutcomeKind.THROW,
                    null,
                    throwStatement.get().exceptionType(),
                    throwStatement.get().exceptionType(),
                    throwStatement.get().message(),
                    "Switch branch throws " + throwStatement.get().exceptionType()
            ));
        }

        return List.of(new DecisionBranchOutcome(
                DecisionOutcomeKind.CONTINUE,
                null,
                null,
                null,
                null,
                "Enters " + (defaultCase ? "DEFAULT" : "CASE") + " switch branch"
        ));
    }

    private static DecisionBranchOutcome toExpressionOutcome(
            boolean defaultCase,
            List<String> labels,
            SwitchContext context,
            BranchExpression branchExpression
    ) {
        String caseName = defaultCase ? "default" : String.join(", ", labels);
        return switch (context.usage()) {
            case RETURN_EXPRESSION -> new DecisionBranchOutcome(
                    DecisionOutcomeKind.RETURN,
                    branchExpression.expression(),
                    JavaDecisionSourceSupport.returnTarget(branchExpression.expression()),
                    null,
                    null,
                    "Switch " + caseName + " branch returns " + branchExpression.expression()
            );
            case ASSIGNMENT_EXPRESSION -> new DecisionBranchOutcome(
                    DecisionOutcomeKind.ASSIGN,
                    branchExpression.expression(),
                    branchExpression.expression(),
                    null,
                    null,
                    context.assignedTo() == null
                            ? "Switch " + caseName + " branch assigns " + branchExpression.expression()
                            : "Switch " + caseName + " branch assigns " + context.assignedTo() + " to " + branchExpression.expression()
            );
            case EXPRESSION -> new DecisionBranchOutcome(
                    DecisionOutcomeKind.UNKNOWN,
                    branchExpression.expression(),
                    branchExpression.expression(),
                    null,
                    null,
                    "Switch " + caseName + " branch yields " + branchExpression.expression()
            );
            case STATEMENT -> new DecisionBranchOutcome(
                    DecisionOutcomeKind.CONTINUE,
                    branchExpression.expression(),
                    null,
                    null,
                    null,
                    "Enters " + (defaultCase ? "DEFAULT" : "CASE") + " switch branch"
            );
        };
    }

    private static Optional<BranchExpression> parseBranchExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int bodyStart,
            int bodyEnd
    ) {
        Range body = trim(sourceFile.maskedSource(), bodyStart, bodyEnd);
        if (body.start() >= body.end()) {
            return Optional.empty();
        }
        if (sourceFile.maskedSource().charAt(body.start()) == '{'
                && JavaDecisionSourceSupport.findMatching(sourceFile.maskedSource(), body.start(), '{', '}') == body.end() - 1) {
            return parseYield(sourceFile, body.start() + 1, body.end() - 1);
        }
        Optional<BranchExpression> yielded = parseYield(sourceFile, body.start(), body.end());
        if (yielded.isPresent()) {
            return yielded;
        }
        int semicolon = JavaDecisionSourceSupport.findTopLevelSemicolon(sourceFile.maskedSource(), body.start(), body.end());
        if (semicolon < 0) {
            return Optional.empty();
        }
        int afterSemicolon = JavaDecisionSourceSupport.skipWhitespace(sourceFile.maskedSource(), semicolon + 1, body.end());
        if (afterSemicolon != body.end()) {
            return Optional.empty();
        }
        String expression = sourceFile.source().substring(body.start(), semicolon).trim();
        if (expression.isBlank()
                || JavaDecisionSourceSupport.startsWithWord(sourceFile.maskedSource(), body.start(), "return")
                || JavaDecisionSourceSupport.startsWithWord(sourceFile.maskedSource(), body.start(), "throw")
                || JavaDecisionSourceSupport.startsWithWord(sourceFile.maskedSource(), body.start(), "break")) {
            return Optional.empty();
        }
        return Optional.of(new BranchExpression(expression));
    }

    private static Optional<BranchExpression> parseYield(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int bodyStart,
            int bodyEnd
    ) {
        int yieldStart = JavaDecisionSourceSupport.skipWhitespace(sourceFile.maskedSource(), bodyStart, bodyEnd);
        if (!JavaDecisionSourceSupport.startsWithWord(sourceFile.maskedSource(), yieldStart, "yield")) {
            return Optional.empty();
        }
        int expressionStart = JavaDecisionSourceSupport.skipWhitespace(
                sourceFile.maskedSource(),
                yieldStart + "yield".length(),
                bodyEnd
        );
        int semicolon = JavaDecisionSourceSupport.findTopLevelSemicolon(sourceFile.maskedSource(), expressionStart, bodyEnd);
        if (semicolon < 0) {
            return Optional.empty();
        }
        int afterSemicolon = JavaDecisionSourceSupport.skipWhitespace(sourceFile.maskedSource(), semicolon + 1, bodyEnd);
        if (afterSemicolon != bodyEnd) {
            return Optional.empty();
        }
        return Optional.of(new BranchExpression(sourceFile.source().substring(expressionStart, semicolon).trim()));
    }

    private static int statementStart(String maskedSource, int methodBodyStart, int switchStart) {
        int index = switchStart - 1;
        while (index >= methodBodyStart) {
            char current = maskedSource.charAt(index);
            if (current == ';' || current == '{' || current == '}') {
                return JavaDecisionSourceSupport.skipWhitespace(maskedSource, index + 1, switchStart);
            }
            index--;
        }
        return methodBodyStart;
    }

    private static int findLastAssignment(String value) {
        int assignment = -1;
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            } else if (current == '=' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                char previous = index == 0 ? '\0' : value.charAt(index - 1);
                char next = index + 1 >= value.length() ? '\0' : value.charAt(index + 1);
                if (previous != '=' && previous != '!' && previous != '<' && previous != '>' && next != '=') {
                    assignment = index;
                }
            }
        }
        return assignment;
    }

    private static String assignmentTarget(String left) {
        if (left == null || left.isBlank()) {
            return null;
        }
        String[] tokens = left.trim().split("\\s+");
        String target = tokens[tokens.length - 1].trim();
        int dot = target.lastIndexOf('.');
        return dot >= 0 ? target.substring(dot + 1) : target;
    }

    private static int leadingWhitespaceLength(String value) {
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isBlank(String value, int start, int end) {
        for (int index = start; index < end; index++) {
            if (!Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static Range trim(String value, int start, int end) {
        int trimmedStart = start;
        int trimmedEnd = end;
        while (trimmedStart < trimmedEnd && Character.isWhitespace(value.charAt(trimmedStart))) {
            trimmedStart++;
        }
        while (trimmedEnd > trimmedStart && Character.isWhitespace(value.charAt(trimmedEnd - 1))) {
            trimmedEnd--;
        }
        return new Range(trimmedStart, trimmedEnd);
    }

    record SwitchDecision(
            int switchStart,
            int statementEnd,
            String selector,
            String snippet,
            boolean expression,
            String assignedTo,
            List<SwitchCase> cases
    ) {
    }

    record SwitchCase(
            String kind,
            List<String> labels,
            int order,
            int bodyStart,
            int bodyEnd,
            List<DecisionBranchOutcome> outcomes
    ) {
    }

    record DecisionBranchOutcome(
            DecisionOutcomeKind kind,
            String expression,
            String target,
            String exceptionType,
            String message,
            String meaning
    ) {
    }

    enum DecisionOutcomeKind {
        THROW,
        RETURN,
        CONTINUE,
        ASSIGN,
        UNKNOWN
    }

    private record SwitchContext(SwitchUsage usage, String assignedTo) {
    }

    private enum SwitchUsage {
        STATEMENT,
        RETURN_EXPRESSION,
        ASSIGNMENT_EXPRESSION,
        EXPRESSION
    }

    private record CaseHeader(
            int labelStart,
            int headerEnd,
            int bodyStart,
            boolean defaultCase,
            boolean arrowCase,
            List<String> labels
    ) {
    }

    private record CaseTerminator(int start, int end, boolean arrow) {
    }

    private record BranchExpression(String expression) {
    }

    private record Range(int start, int end) {
    }
}
