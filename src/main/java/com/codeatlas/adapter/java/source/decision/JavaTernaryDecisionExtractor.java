package com.codeatlas.adapter.java.source.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JavaTernaryDecisionExtractor {
    List<TernaryDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange
    ) {
        List<TernaryDecision> decisions = new ArrayList<>();
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

    private static List<TernaryDecision> parseStatement(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int semicolon
    ) {
        String maskedSource = sourceFile.maskedSource();
        if (JavaDecisionSourceSupport.startsWithWord(maskedSource, statementStart, "return")) {
            int expressionStart = JavaDecisionSourceSupport.skipWhitespace(
                    maskedSource,
                    statementStart + "return".length(),
                    semicolon
            );
            return parseExpression(sourceFile, statementStart, semicolon, expressionStart, semicolon, StatementContext.RETURN, null);
        }

        Optional<AssignmentExpression> assignment = parseAssignment(sourceFile, statementStart, semicolon);
        if (assignment.isPresent()) {
            AssignmentExpression parsed = assignment.get();
            return parseExpression(
                    sourceFile,
                    statementStart,
                    semicolon,
                    parsed.expressionStart(),
                    semicolon,
                    parsed.context(),
                    parsed.target()
            );
        }

        Optional<CallExpression> callExpression = parseCallExpression(sourceFile, statementStart, semicolon);
        if (callExpression.isEmpty()) {
            return List.of();
        }

        List<TernaryDecision> decisions = new ArrayList<>();
        for (Range argument : splitTopLevelArguments(sourceFile.maskedSource(), callExpression.get().argumentsStart(), callExpression.get().argumentsEnd())) {
            Optional<TernaryExpression> expression = parseTernaryExpression(sourceFile, argument.start(), argument.end());
            expression.ifPresent(ternary -> decisions.add(new TernaryDecision(
                    ternary.expressionStart(),
                    statementStart,
                    semicolon + 1,
                    StatementContext.CALL_ARGUMENT,
                    callExpression.get().callee(),
                    JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(statementStart, semicolon + 1)),
                    ternary
            )));
        }
        return List.copyOf(decisions);
    }

    private static List<TernaryDecision> parseExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int semicolon,
            int expressionStart,
            int expressionEnd,
            StatementContext context,
            String target
    ) {
        return parseTernaryExpression(sourceFile, expressionStart, expressionEnd)
                .map(expression -> List.of(new TernaryDecision(
                        expression.expressionStart(),
                        statementStart,
                        semicolon + 1,
                        context,
                        target,
                        JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(statementStart, semicolon + 1)),
                        expression
                )))
                .orElseGet(List::of);
    }

    private static Optional<TernaryExpression> parseTernaryExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int start,
            int end
    ) {
        Range expressionRange = trim(sourceFile.source(), start, end);
        if (isWholeParenthesized(sourceFile.maskedSource(), expressionRange.start(), expressionRange.end())) {
            return parseTernaryExpression(sourceFile, expressionRange.start() + 1, expressionRange.end() - 1);
        }
        Optional<QuestionMark> questionMark = findTopLevelQuestion(sourceFile.maskedSource(), expressionRange.start(), expressionRange.end());
        if (questionMark.isEmpty()) {
            return Optional.empty();
        }

        int question = questionMark.get().index();
        int colon = findMatchingTernaryColon(sourceFile.maskedSource(), question + 1, expressionRange.end());
        if (colon < 0) {
            return Optional.empty();
        }

        Range conditionRange = trim(sourceFile.source(), expressionRange.start(), question);
        Range trueRange = trim(sourceFile.source(), question + 1, colon);
        Range falseRange = trim(sourceFile.source(), colon + 1, expressionRange.end());
        return Optional.of(new TernaryExpression(
                expressionRange.start(),
                JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(expressionRange.start(), expressionRange.end())),
                JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(conditionRange.start(), conditionRange.end())),
                JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(trueRange.start(), trueRange.end())),
                JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(falseRange.start(), falseRange.end())),
                parseTernaryExpression(sourceFile, trueRange.start(), trueRange.end()).orElse(null),
                parseTernaryExpression(sourceFile, falseRange.start(), falseRange.end()).orElse(null)
        ));
    }

    private static Optional<AssignmentExpression> parseAssignment(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int semicolon
    ) {
        int assignment = findTopLevelAssignment(sourceFile.maskedSource(), statementStart, semicolon);
        if (assignment < 0) {
            return Optional.empty();
        }

        String left = sourceFile.source().substring(statementStart, assignment).trim();
        if (left.isBlank()) {
            return Optional.empty();
        }
        String target = assignmentTarget(left);
        StatementContext context = left.matches(".*\\s+.*")
                ? StatementContext.VARIABLE_INITIALIZER
                : StatementContext.ASSIGNMENT;
        return Optional.of(new AssignmentExpression(
                target,
                context,
                JavaDecisionSourceSupport.skipWhitespace(sourceFile.maskedSource(), assignment + 1, semicolon)
        ));
    }

    private static Optional<CallExpression> parseCallExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int semicolon
    ) {
        String expression = sourceFile.source().substring(statementStart, semicolon).trim();
        if (expression.isBlank() || expression.contains("=") || expression.contains("->") || expression.contains("::")) {
            return Optional.empty();
        }

        int absoluteExpressionStart = statementStart;
        while (absoluteExpressionStart < semicolon && Character.isWhitespace(sourceFile.source().charAt(absoluteExpressionStart))) {
            absoluteExpressionStart++;
        }
        int openParen = sourceFile.maskedSource().indexOf('(', absoluteExpressionStart);
        if (openParen <= absoluteExpressionStart || openParen >= semicolon) {
            return Optional.empty();
        }
        int closeParen = JavaDecisionSourceSupport.findMatching(sourceFile.maskedSource(), openParen, '(', ')');
        if (closeParen != semicolon - 1) {
            return Optional.empty();
        }
        String callee = sourceFile.source().substring(absoluteExpressionStart, openParen).trim();
        if (callee.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CallExpression(callee, openParen + 1, closeParen));
    }

    private static int findTopLevelAssignment(String source, int start, int end) {
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
                    return index;
                }
            }
        }
        return -1;
    }

    private static Optional<QuestionMark> findTopLevelQuestion(String source, int start, int end) {
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
            } else if (current == '?' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                return Optional.of(new QuestionMark(index));
            }
        }
        return Optional.empty();
    }

    private static boolean isWholeParenthesized(String source, int start, int end) {
        if (start >= end || source.charAt(start) != '(' || source.charAt(end - 1) != ')') {
            return false;
        }
        return JavaDecisionSourceSupport.findMatching(source, start, '(', ')') == end - 1;
    }

    private static int findMatchingTernaryColon(String source, int start, int end) {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        int nestedTernaryDepth = 0;
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
            } else if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                if (current == '?') {
                    nestedTernaryDepth++;
                } else if (current == ':') {
                    if (nestedTernaryDepth == 0) {
                        return index;
                    }
                    nestedTernaryDepth--;
                }
            }
        }
        return -1;
    }

    private static List<Range> splitTopLevelArguments(String source, int start, int end) {
        List<Range> ranges = new ArrayList<>();
        int argumentStart = start;
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
            } else if (current == ',' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                ranges.add(trim(source, argumentStart, index));
                argumentStart = index + 1;
            }
        }
        ranges.add(trim(source, argumentStart, end));
        return ranges;
    }

    private static String assignmentTarget(String left) {
        String normalized = JavaDecisionSourceSupport.normalizeWhitespace(left);
        int lastSpace = normalized.lastIndexOf(' ');
        if (lastSpace >= 0) {
            return normalized.substring(lastSpace + 1);
        }
        return normalized;
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
        VARIABLE_INITIALIZER,
        ASSIGNMENT,
        CALL_ARGUMENT
    }

    record TernaryDecision(
            int expressionStart,
            int statementStart,
            int statementEnd,
            StatementContext context,
            String target,
            String snippet,
            TernaryExpression expression
    ) {
    }

    record TernaryExpression(
            int expressionStart,
            String text,
            String condition,
            String whenTrue,
            String whenFalse,
            TernaryExpression trueChild,
            TernaryExpression falseChild
    ) {
    }

    private record AssignmentExpression(String target, StatementContext context, int expressionStart) {
    }

    private record CallExpression(String callee, int argumentsStart, int argumentsEnd) {
    }

    private record QuestionMark(int index) {
    }

    private record Range(int start, int end) {
    }
}
