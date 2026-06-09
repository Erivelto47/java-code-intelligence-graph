package com.codeatlas.adapter.java.source.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JavaStreamDecisionExtractor {
    List<StreamDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange
    ) {
        List<StreamDecision> decisions = new ArrayList<>();
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
                    decisions.addAll(parseIfCondition(sourceFile, ifStatement.get()));
                    index = JavaDecisionSourceSupport.skipWhitespace(
                            maskedSource,
                            ifStatement.get().statementEnd(),
                            methodRange.bodyEnd()
                    );
                    continue;
                }
            }

            if (JavaDecisionSourceSupport.startsWithWord(maskedSource, index, "switch")) {
                int skipped = skipSwitch(maskedSource, index, methodRange.bodyEnd());
                if (skipped > index) {
                    index = JavaDecisionSourceSupport.skipWhitespace(maskedSource, skipped, methodRange.bodyEnd());
                    continue;
                }
            }

            if (maskedSource.charAt(index) == '{') {
                int closeBrace = JavaDecisionSourceSupport.findMatching(maskedSource, index, '{', '}');
                if (closeBrace < 0 || closeBrace >= methodRange.bodyEnd()) {
                    break;
                }
                index = JavaDecisionSourceSupport.skipWhitespace(maskedSource, closeBrace + 1, methodRange.bodyEnd());
                continue;
            }

            int semicolon = JavaDecisionSourceSupport.findTopLevelSemicolon(maskedSource, index, methodRange.bodyEnd());
            if (semicolon < 0) {
                break;
            }
            decisions.addAll(parseStatement(sourceFile, index, semicolon));
            index = JavaDecisionSourceSupport.skipWhitespace(maskedSource, semicolon + 1, methodRange.bodyEnd());
        }
        return List.copyOf(decisions);
    }

    private static List<StreamDecision> parseIfCondition(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.IfStatement ifStatement
    ) {
        int conditionStart = conditionStart(sourceFile.maskedSource(), ifStatement.ifStart());
        int conditionEnd = conditionStart < 0
                ? -1
                : JavaDecisionSourceSupport.findMatching(sourceFile.maskedSource(), conditionStart - 1, '(', ')');
        if (conditionStart < 0 || conditionEnd < 0) {
            return List.of();
        }
        return parseExpression(
                sourceFile,
                conditionStart,
                conditionEnd,
                ifStatement.ifStart(),
                ifStatement.statementEnd(),
                StatementContext.IF_CONDITION,
                null,
                true
        );
    }

    private static List<StreamDecision> parseStatement(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int semicolon
    ) {
        String maskedSource = sourceFile.maskedSource();
        StatementContext context = StatementContext.EXPRESSION;
        String target = null;
        int expressionStart = JavaDecisionSourceSupport.skipWhitespace(maskedSource, statementStart, semicolon);

        if (JavaDecisionSourceSupport.startsWithWord(maskedSource, expressionStart, "return")) {
            context = StatementContext.RETURN;
            expressionStart = JavaDecisionSourceSupport.skipWhitespace(
                    maskedSource,
                    expressionStart + "return".length(),
                    semicolon
            );
        } else {
            int assignment = findTopLevelAssignment(maskedSource, statementStart, semicolon);
            if (assignment >= 0) {
                target = assignmentTarget(sourceFile.source().substring(statementStart, assignment));
                context = StatementContext.ASSIGNMENT;
                expressionStart = JavaDecisionSourceSupport.skipWhitespace(maskedSource, assignment + 1, semicolon);
            }
        }

        return parseExpression(
                sourceFile,
                expressionStart,
                semicolon,
                statementStart,
                semicolon + 1,
                context,
                target,
                false
        );
    }

    private static List<StreamDecision> parseExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int expressionStart,
            int expressionEnd,
            int snippetStart,
            int snippetEnd,
            StatementContext context,
            String target,
            boolean insideIfCondition
    ) {
        Range expressionRange = trim(sourceFile.source(), expressionStart, expressionEnd);
        if (expressionRange.start() >= expressionRange.end()) {
            return List.of();
        }
        String expression = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(expressionRange.start(), expressionRange.end())
        );
        String snippet = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(snippetStart, snippetEnd)
        );

        List<StreamDecision> decisions = new ArrayList<>();
        for (StreamOperation operation : findStreamOperations(sourceFile, expressionRange.start(), expressionRange.end())) {
            decisions.add(new StreamDecision(
                    operation.methodStart(),
                    operation.operation(),
                    operation.matchOperation(),
                    expression,
                    operation.pipeline(),
                    snippet,
                    parsePredicate(sourceFile, operation.argumentsStart(), operation.argumentsEnd()),
                    context,
                    target,
                    insideIfCondition
            ));
        }
        return List.copyOf(decisions);
    }

    private static List<StreamOperation> findStreamOperations(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int start,
            int end
    ) {
        List<StreamOperation> operations = new ArrayList<>();
        String maskedSource = sourceFile.maskedSource();
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = start; index < end; index++) {
            char current = maskedSource.charAt(index);
            if (current == '.' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                parseOperation(sourceFile, start, index, end).ifPresent(operations::add);
            }
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
        }
        return List.copyOf(operations);
    }

    private static Optional<StreamOperation> parseOperation(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int expressionStart,
            int dotStart,
            int expressionEnd
    ) {
        for (String operation : List.of("anyMatch", "allMatch", "noneMatch", "filter")) {
            int methodStart = dotStart + 1;
            int methodEnd = methodStart + operation.length();
            if (methodEnd >= expressionEnd || !sourceFile.maskedSource().startsWith(operation, methodStart)) {
                continue;
            }
            if (!JavaDecisionSourceSupport.hasIdentifierBoundary(sourceFile.maskedSource(), methodStart, methodEnd)) {
                continue;
            }
            int openParen = JavaDecisionSourceSupport.skipWhitespace(sourceFile.maskedSource(), methodEnd, expressionEnd);
            if (openParen >= expressionEnd || sourceFile.maskedSource().charAt(openParen) != '(') {
                continue;
            }
            int closeParen = JavaDecisionSourceSupport.findMatching(sourceFile.maskedSource(), openParen, '(', ')');
            if (closeParen < 0 || closeParen >= expressionEnd) {
                continue;
            }
            String beforeOperation = sourceFile.maskedSource().substring(expressionStart, dotStart);
            if (!beforeOperation.contains(".stream(") && !beforeOperation.contains(".parallelStream(")) {
                continue;
            }
            String pipeline = JavaDecisionSourceSupport.normalizeWhitespace(
                    sourceFile.source().substring(expressionStart, closeParen + 1)
            );
            return Optional.of(new StreamOperation(
                    methodStart,
                    operation,
                    !"filter".equals(operation),
                    pipeline,
                    openParen + 1,
                    closeParen
            ));
        }
        return Optional.empty();
    }

    private static StreamPredicate parsePredicate(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int argumentsStart,
            int argumentsEnd
    ) {
        String argument = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(argumentsStart, argumentsEnd)
        );
        int arrow = topLevelArrow(sourceFile.maskedSource(), argumentsStart, argumentsEnd);
        if (arrow < 0) {
            return new StreamPredicate(null, argument);
        }
        String parameter = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(argumentsStart, arrow)
        );
        parameter = stripSingleParameterParens(parameter);
        String text = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(arrow + 2, argumentsEnd)
        );
        return new StreamPredicate(parameter, text);
    }

    private static int conditionStart(String maskedSource, int ifStart) {
        int openParen = JavaDecisionSourceSupport.skipWhitespace(maskedSource, ifStart + "if".length(), maskedSource.length());
        if (openParen >= maskedSource.length() || maskedSource.charAt(openParen) != '(') {
            return -1;
        }
        return openParen + 1;
    }

    private static int topLevelArrow(String source, int start, int end) {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = start; index + 1 < end; index++) {
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
            } else if (current == '-' && source.charAt(index + 1) == '>'
                    && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int findTopLevelAssignment(String source, int start, int end) {
        int assignment = -1;
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = start; index < end; index++) {
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
            } else if (current == '=' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                char previous = index == start ? '\0' : source.charAt(index - 1);
                char next = index + 1 >= end ? '\0' : source.charAt(index + 1);
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
        String[] tokens = JavaDecisionSourceSupport.normalizeWhitespace(left).split("\\s+");
        String target = tokens[tokens.length - 1];
        int dot = target.lastIndexOf('.');
        return dot >= 0 ? target.substring(dot + 1) : target;
    }

    private static String stripSingleParameterParens(String parameter) {
        String trimmed = parameter.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (!inner.contains(",")) {
                return inner;
            }
        }
        return trimmed;
    }

    private static int skipSwitch(String maskedSource, int switchStart, int limit) {
        int selectorOpen = JavaDecisionSourceSupport.skipWhitespace(maskedSource, switchStart + "switch".length(), limit);
        if (selectorOpen >= limit || maskedSource.charAt(selectorOpen) != '(') {
            return -1;
        }
        int selectorClose = JavaDecisionSourceSupport.findMatching(maskedSource, selectorOpen, '(', ')');
        if (selectorClose < 0 || selectorClose >= limit) {
            return -1;
        }
        int bodyOpen = JavaDecisionSourceSupport.skipWhitespace(maskedSource, selectorClose + 1, limit);
        if (bodyOpen >= limit || maskedSource.charAt(bodyOpen) != '{') {
            return -1;
        }
        int bodyClose = JavaDecisionSourceSupport.findMatching(maskedSource, bodyOpen, '{', '}');
        if (bodyClose < 0 || bodyClose >= limit) {
            return -1;
        }
        int afterBody = JavaDecisionSourceSupport.skipWhitespace(maskedSource, bodyClose + 1, limit);
        return afterBody < limit && maskedSource.charAt(afterBody) == ';' ? afterBody + 1 : bodyClose + 1;
    }

    private static Range trim(String source, int start, int end) {
        int trimmedStart = start;
        int trimmedEnd = end;
        while (trimmedStart < trimmedEnd && Character.isWhitespace(source.charAt(trimmedStart))) {
            trimmedStart++;
        }
        while (trimmedEnd > trimmedStart && Character.isWhitespace(source.charAt(trimmedEnd - 1))) {
            trimmedEnd--;
        }
        return new Range(trimmedStart, trimmedEnd);
    }

    enum StatementContext {
        RETURN,
        ASSIGNMENT,
        EXPRESSION,
        IF_CONDITION
    }

    record StreamDecision(
            int expressionStart,
            String operation,
            boolean matchOperation,
            String expression,
            String pipeline,
            String snippet,
            StreamPredicate predicate,
            StatementContext context,
            String target,
            boolean insideIfCondition
    ) {
    }

    record StreamPredicate(String parameter, String text) {
    }

    private record StreamOperation(
            int methodStart,
            String operation,
            boolean matchOperation,
            String pipeline,
            int argumentsStart,
            int argumentsEnd
    ) {
    }

    private record Range(int start, int end) {
    }
}
