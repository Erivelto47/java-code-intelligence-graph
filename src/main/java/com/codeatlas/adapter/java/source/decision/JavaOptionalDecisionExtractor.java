package com.codeatlas.adapter.java.source.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JavaOptionalDecisionExtractor {
    List<OptionalDecision> parse(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            JavaDecisionSourceSupport.MethodRange methodRange
    ) {
        List<OptionalDecision> decisions = new ArrayList<>();
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
            parseStatement(sourceFile, index, semicolon).ifPresent(decisions::add);
            index = JavaDecisionSourceSupport.skipWhitespace(maskedSource, semicolon + 1, methodRange.bodyEnd());
        }
        return List.copyOf(decisions);
    }

    private static Optional<OptionalDecision> parseStatement(
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
                String left = sourceFile.source().substring(statementStart, assignment).trim();
                context = StatementContext.ASSIGNMENT;
                target = assignmentTarget(left);
                expressionStart = JavaDecisionSourceSupport.skipWhitespace(maskedSource, assignment + 1, semicolon);
            } else if (looksLikeCallStatement(maskedSource, expressionStart, semicolon)) {
                context = StatementContext.CALL;
            }
        }

        Range expressionRange = trim(sourceFile.source(), expressionStart, semicolon);
        if (expressionRange.start() >= expressionRange.end()) {
            return Optional.empty();
        }

        return parseOptionalExpression(sourceFile, statementStart, semicolon + 1, expressionRange, context, target);
    }

    private static Optional<OptionalDecision> parseOptionalExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int statementStart,
            int statementEnd,
            Range expressionRange,
            StatementContext context,
            String target
    ) {
        Optional<TerminalCall> terminalCall = findTerminalOptionalCall(
                sourceFile,
                expressionRange.start(),
                expressionRange.end()
        );
        if (terminalCall.isEmpty()) {
            return Optional.empty();
        }

        TerminalCall call = terminalCall.get();
        String expression = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(expressionRange.start(), expressionRange.end())
        );
        String sourceExpression = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(expressionRange.start(), call.dotStart())
        );
        String argumentText = JavaDecisionSourceSupport.normalizeWhitespace(
                sourceFile.source().substring(call.argumentsStart(), call.argumentsEnd())
        );
        List<Range> arguments = splitTopLevelArguments(sourceFile.maskedSource(), call.argumentsStart(), call.argumentsEnd());
        List<OptionalBranch> branches = branchesFor(call.methodName(), sourceFile, sourceExpression, argumentText, arguments, context, target);
        if (branches.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new OptionalDecision(
                call.dotStart() + 1,
                statementEnd,
                call.methodName(),
                expression,
                sourceExpression,
                JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(statementStart, statementEnd)),
                context,
                target,
                branches
        ));
    }

    private static List<OptionalBranch> branchesFor(
            String methodName,
            JavaDecisionSourceSupport.SourceFile sourceFile,
            String sourceExpression,
            String argumentText,
            List<Range> arguments,
            StatementContext context,
            String target
    ) {
        return switch (methodName) {
            case "orElseThrow" -> orElseThrowBranches(sourceFile, sourceExpression, argumentText, arguments, context, target);
            case "orElse" -> orElseBranches(sourceExpression, argumentText, context, target, "value");
            case "orElseGet" -> orElseBranches(sourceExpression, argumentText, context, target, "supplier");
            case "ifPresent" -> ifPresentBranches(argumentText);
            case "ifPresentOrElse" -> ifPresentOrElseBranches(sourceFile, arguments);
            default -> List.of();
        };
    }

    private static List<OptionalBranch> orElseThrowBranches(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            String sourceExpression,
            String argumentText,
            List<Range> arguments,
            StatementContext context,
            String target
    ) {
        ExceptionExpression exception = parseExceptionExpression(sourceFile, argumentText, arguments);
        return List.of(
                new OptionalBranch(
                        "PRESENT",
                        "present",
                        1,
                        contextAction(context),
                        contextTarget(context, target, sourceExpression),
                        null,
                        null,
                        "Optional present branch uses " + sourceExpression
                ),
                new OptionalBranch(
                        "EMPTY",
                        "empty",
                        2,
                        OptionalOutcomeKind.THROW,
                        exception.exceptionType() == null ? exception.expression() : exception.exceptionType(),
                        exception.exceptionType(),
                        exception.message(),
                        "Optional empty branch throws " + exception.expression()
                )
        );
    }

    private static List<OptionalBranch> orElseBranches(
            String sourceExpression,
            String fallback,
            StatementContext context,
            String target,
            String fallbackKind
    ) {
        if (fallback.isBlank()) {
            return List.of();
        }
        return List.of(
                new OptionalBranch(
                        "PRESENT",
                        "present",
                        1,
                        contextAction(context),
                        contextTarget(context, target, sourceExpression),
                        null,
                        null,
                        "Optional present branch uses " + sourceExpression
                ),
                new OptionalBranch(
                        "EMPTY",
                        "empty",
                        2,
                        contextAction(context),
                        contextTarget(context, target, fallback),
                        null,
                        null,
                        "Optional empty branch uses fallback " + fallbackKind + " " + fallback
                )
        );
    }

    private static List<OptionalBranch> ifPresentBranches(String presentBranch) {
        if (presentBranch.isBlank()) {
            return List.of();
        }
        return List.of(
                new OptionalBranch(
                        "PRESENT",
                        "present",
                        1,
                        OptionalOutcomeKind.CALL,
                        presentBranch,
                        null,
                        null,
                        "Optional present branch executes " + presentBranch
                ),
                new OptionalBranch(
                        "EMPTY",
                        "empty",
                        2,
                        OptionalOutcomeKind.CONTINUE,
                        null,
                        null,
                        null,
                        "Optional empty branch has no visible action"
                )
        );
    }

    private static List<OptionalBranch> ifPresentOrElseBranches(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            List<Range> arguments
    ) {
        if (arguments.size() != 2) {
            return List.of();
        }
        String presentBranch = text(sourceFile, arguments.get(0));
        String emptyBranch = text(sourceFile, arguments.get(1));
        if (presentBranch.isBlank() || emptyBranch.isBlank()) {
            return List.of();
        }
        return List.of(
                new OptionalBranch(
                        "PRESENT",
                        "present",
                        1,
                        OptionalOutcomeKind.CALL,
                        presentBranch,
                        null,
                        null,
                        "Optional present branch executes " + presentBranch
                ),
                new OptionalBranch(
                        "EMPTY",
                        "empty",
                        2,
                        OptionalOutcomeKind.CALL,
                        emptyBranch,
                        null,
                        null,
                        "Optional empty branch executes " + emptyBranch
                )
        );
    }

    private static ExceptionExpression parseExceptionExpression(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            String argumentText,
            List<Range> arguments
    ) {
        String expression = argumentText.isBlank() ? "NoSuchElementException" : argumentText;
        if (arguments.size() == 1) {
            String argument = text(sourceFile, arguments.get(0));
            int arrow = topLevelArrow(sourceFile.maskedSource(), arguments.get(0).start(), arguments.get(0).end());
            if (arrow >= 0) {
                expression = JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(arrow + 2, arguments.get(0).end()));
            } else if (!argument.isBlank()) {
                expression = argument;
            }
        }

        String exceptionType = exceptionType(expression);
        String message = firstStringLiteral(expression);
        return new ExceptionExpression(expression, exceptionType, message);
    }

    private static OptionalOutcomeKind contextAction(StatementContext context) {
        return switch (context) {
            case RETURN -> OptionalOutcomeKind.RETURN;
            case ASSIGNMENT -> OptionalOutcomeKind.ASSIGN;
            case CALL -> OptionalOutcomeKind.CALL;
            case EXPRESSION -> OptionalOutcomeKind.UNKNOWN;
        };
    }

    private static String contextTarget(StatementContext context, String statementTarget, String expression) {
        return switch (context) {
            case RETURN -> JavaDecisionSourceSupport.returnTarget(expression);
            case ASSIGNMENT -> expression;
            case CALL, EXPRESSION -> expression;
        };
    }

    private static Optional<TerminalCall> findTerminalOptionalCall(
            JavaDecisionSourceSupport.SourceFile sourceFile,
            int start,
            int end
    ) {
        String maskedSource = sourceFile.maskedSource();
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = start; index < end; index++) {
            char current = maskedSource.charAt(index);
            if (current == '.' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                Optional<TerminalCall> parsed = parseTerminalCall(maskedSource, index, end);
                if (parsed.isPresent()) {
                    return parsed;
                }
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
        return Optional.empty();
    }

    private static Optional<TerminalCall> parseTerminalCall(String maskedSource, int dotStart, int expressionEnd) {
        for (String methodName : List.of("ifPresentOrElse", "orElseThrow", "orElseGet", "ifPresent", "orElse")) {
            int methodStart = dotStart + 1;
            int methodEnd = methodStart + methodName.length();
            if (methodEnd >= expressionEnd || !maskedSource.startsWith(methodName, methodStart)) {
                continue;
            }
            if (!JavaDecisionSourceSupport.hasIdentifierBoundary(maskedSource, methodStart, methodEnd)) {
                continue;
            }
            int openParen = JavaDecisionSourceSupport.skipWhitespace(maskedSource, methodEnd, expressionEnd);
            if (openParen >= expressionEnd || maskedSource.charAt(openParen) != '(') {
                continue;
            }
            int closeParen = JavaDecisionSourceSupport.findMatching(maskedSource, openParen, '(', ')');
            if (closeParen < 0 || closeParen >= expressionEnd) {
                continue;
            }
            int afterClose = JavaDecisionSourceSupport.skipWhitespace(maskedSource, closeParen + 1, expressionEnd);
            if (afterClose == expressionEnd) {
                return Optional.of(new TerminalCall(dotStart, methodName, openParen + 1, closeParen));
            }
        }
        return Optional.empty();
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

    private static boolean looksLikeCallStatement(String maskedSource, int expressionStart, int semicolon) {
        int openParen = maskedSource.indexOf('(', expressionStart);
        if (openParen <= expressionStart || openParen >= semicolon) {
            return false;
        }
        int closeParen = JavaDecisionSourceSupport.findMatching(maskedSource, openParen, '(', ')');
        return closeParen == semicolon - 1;
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
        return ranges.stream().filter(range -> range.start() < range.end()).toList();
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

    private static String exceptionType(String expression) {
        String trimmed = expression.trim();
        int newIndex = JavaDecisionSourceSupport.indexOfWord(trimmed, "new", 0, trimmed.length());
        if (newIndex < 0) {
            return null;
        }
        int typeStart = JavaDecisionSourceSupport.skipWhitespace(trimmed, newIndex + "new".length(), trimmed.length());
        int index = typeStart;
        while (index < trimmed.length()) {
            char current = trimmed.charAt(index);
            if (Character.isJavaIdentifierPart(current) || current == '.' || current == '$') {
                index++;
            } else {
                break;
            }
        }
        return typeStart == index ? null : trimmed.substring(typeStart, index);
    }

    private static String firstStringLiteral(String expression) {
        int start = expression.indexOf('"');
        if (start < 0) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int index = start + 1; index < expression.length(); index++) {
            char current = expression.charAt(index);
            if (escaping) {
                value.append(current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }
        return null;
    }

    private static String text(JavaDecisionSourceSupport.SourceFile sourceFile, Range range) {
        return JavaDecisionSourceSupport.normalizeWhitespace(sourceFile.source().substring(range.start(), range.end()));
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
        CALL,
        EXPRESSION
    }

    enum OptionalOutcomeKind {
        THROW,
        RETURN,
        CONTINUE,
        CALL,
        ASSIGN,
        UNKNOWN
    }

    record OptionalDecision(
            int expressionStart,
            int statementEnd,
            String optionalMethod,
            String expression,
            String sourceExpression,
            String snippet,
            StatementContext context,
            String target,
            List<OptionalBranch> branches
    ) {
    }

    record OptionalBranch(
            String kind,
            String condition,
            int order,
            OptionalOutcomeKind outcomeKind,
            String target,
            String exceptionType,
            String message,
            String meaning
    ) {
    }

    private record TerminalCall(int dotStart, String methodName, int argumentsStart, int argumentsEnd) {
    }

    private record ExceptionExpression(String expression, String exceptionType, String message) {
    }

    private record Range(int start, int end) {
    }
}
