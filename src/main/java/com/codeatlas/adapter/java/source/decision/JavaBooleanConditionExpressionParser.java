package com.codeatlas.adapter.java.source.decision;

import com.codeatlas.core.decision.DecisionConditionExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

final class JavaBooleanConditionExpressionParser {
    private static final Pattern SIMPLE_REFERENCE = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*"
    );
    private static final List<String> COMPARISON_OPERATORS = List.of("==", "!=", "<=", ">=", "<", ">");

    Optional<DecisionConditionExpression> parseComposed(String condition) {
        if (condition == null || condition.isBlank() || !hasComposedBooleanSyntax(condition)) {
            return Optional.empty();
        }
        return Optional.of(parse(condition, 0, condition.length()));
    }

    private DecisionConditionExpression parse(String source, int start, int end) {
        Range range = trim(source, start, end);
        return parseOr(source, range.start(), range.end());
    }

    private DecisionConditionExpression parseOr(String source, int start, int end) {
        List<Range> parts = splitTopLevel(source, start, end, "||");
        if (parts.size() > 1) {
            return new DecisionConditionExpression(
                    "OR",
                    text(source, start, end),
                    null,
                    null,
                    null,
                    null,
                    parts.stream()
                            .map(part -> parseAnd(source, part.start(), part.end()))
                            .toList()
            );
        }
        return parseAnd(source, start, end);
    }

    private DecisionConditionExpression parseAnd(String source, int start, int end) {
        List<Range> parts = splitTopLevel(source, start, end, "&&");
        if (parts.size() > 1) {
            return new DecisionConditionExpression(
                    "AND",
                    text(source, start, end),
                    null,
                    null,
                    null,
                    null,
                    parts.stream()
                            .map(part -> parseNot(source, part.start(), part.end()))
                            .toList()
            );
        }
        return parseNot(source, start, end);
    }

    private DecisionConditionExpression parseNot(String source, int start, int end) {
        Range range = trim(source, start, end);
        if (range.start() < range.end()
                && source.charAt(range.start()) == '!'
                && (range.start() + 1 >= range.end() || source.charAt(range.start() + 1) != '=')) {
            return new DecisionConditionExpression(
                    "NOT",
                    text(source, range.start(), range.end()),
                    null,
                    null,
                    null,
                    parseNot(source, range.start() + 1, range.end()),
                    null
            );
        }
        return parsePrimary(source, range.start(), range.end());
    }

    private DecisionConditionExpression parsePrimary(String source, int start, int end) {
        Range range = trim(source, start, end);
        if (isWholeParenthesizedExpression(source, range.start(), range.end())) {
            return new DecisionConditionExpression(
                    "GROUP",
                    text(source, range.start(), range.end()),
                    null,
                    null,
                    null,
                    parse(source, range.start() + 1, range.end() - 1),
                    null
            );
        }

        Optional<Comparison> comparison = findTopLevelComparison(source, range.start(), range.end());
        if (comparison.isPresent()) {
            Comparison parsed = comparison.get();
            return new DecisionConditionExpression(
                    "COMPARISON",
                    text(source, range.start(), range.end()),
                    parsed.operator(),
                    text(source, range.start(), parsed.operatorStart()),
                    text(source, parsed.operatorStart() + parsed.operator().length(), range.end()),
                    null,
                    null
            );
        }

        String expressionText = text(source, range.start(), range.end());
        if (isMethodCallExpression(expressionText)) {
            return new DecisionConditionExpression("METHOD_CALL", expressionText, null, null, null, null, null);
        }
        if (SIMPLE_REFERENCE.matcher(expressionText).matches()) {
            return new DecisionConditionExpression("REFERENCE", expressionText, null, null, null, null, null);
        }
        return new DecisionConditionExpression("EXPRESSION", expressionText, null, null, null, null, null);
    }

    private static boolean hasComposedBooleanSyntax(String condition) {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int index = 0; index < condition.length(); index++) {
            char current = condition.charAt(index);
            if (inString || inChar) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (inString && current == '"') {
                    inString = false;
                } else if (inChar && current == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '\'') {
                inChar = true;
                continue;
            }
            if (current == '(') {
                parenDepth++;
                if (parenDepth == 1 && looksLikeGrouping(condition, index)) {
                    return true;
                }
                continue;
            }
            if (current == ')') {
                parenDepth--;
                continue;
            }
            if (current == '[') {
                bracketDepth++;
                continue;
            }
            if (current == ']') {
                bracketDepth--;
                continue;
            }
            if (current == '{') {
                braceDepth++;
                continue;
            }
            if (current == '}') {
                braceDepth--;
                continue;
            }
            if (bracketDepth == 0 && braceDepth == 0) {
                if (current == '&' && index + 1 < condition.length() && condition.charAt(index + 1) == '&') {
                    return true;
                }
                if (current == '|' && index + 1 < condition.length() && condition.charAt(index + 1) == '|') {
                    return true;
                }
                if (current == '!' && (index + 1 >= condition.length() || condition.charAt(index + 1) != '=')) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean looksLikeGrouping(String source, int openParen) {
        int previous = openParen - 1;
        while (previous >= 0 && Character.isWhitespace(source.charAt(previous))) {
            previous--;
        }
        if (previous >= 0 && (Character.isJavaIdentifierPart(source.charAt(previous)) || source.charAt(previous) == '.')) {
            return false;
        }
        int closeParen = findMatching(source, openParen, '(', ')');
        if (closeParen < 0) {
            return false;
        }
        String inner = source.substring(openParen + 1, closeParen);
        return inner.contains("&&") || inner.contains("||") || inner.contains("!");
    }

    private static List<Range> splitTopLevel(String source, int start, int end, String delimiter) {
        List<Range> ranges = new ArrayList<>();
        int partStart = start;
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int index = start; index < end; index++) {
            char current = source.charAt(index);
            if (inString || inChar) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (inString && current == '"') {
                    inString = false;
                } else if (inChar && current == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '\'') {
                inChar = true;
                continue;
            }
            if (current == '(') {
                parenDepth++;
                continue;
            }
            if (current == ')') {
                parenDepth--;
                continue;
            }
            if (current == '[') {
                bracketDepth++;
                continue;
            }
            if (current == ']') {
                bracketDepth--;
                continue;
            }
            if (current == '{') {
                braceDepth++;
                continue;
            }
            if (current == '}') {
                braceDepth--;
                continue;
            }
            if (parenDepth == 0
                    && bracketDepth == 0
                    && braceDepth == 0
                    && source.startsWith(delimiter, index)) {
                ranges.add(trim(source, partStart, index));
                partStart = index + delimiter.length();
                index += delimiter.length() - 1;
            }
        }
        ranges.add(trim(source, partStart, end));
        return ranges;
    }

    private static boolean isWholeParenthesizedExpression(String source, int start, int end) {
        if (start >= end || source.charAt(start) != '(' || source.charAt(end - 1) != ')') {
            return false;
        }
        return findMatching(source, start, '(', ')') == end - 1;
    }

    private static Optional<Comparison> findTopLevelComparison(String source, int start, int end) {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int index = start; index < end; index++) {
            char current = source.charAt(index);
            if (inString || inChar) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (inString && current == '"') {
                    inString = false;
                } else if (inChar && current == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '\'') {
                inChar = true;
                continue;
            }
            if (current == '(') {
                parenDepth++;
                continue;
            }
            if (current == ')') {
                parenDepth--;
                continue;
            }
            if (current == '[') {
                bracketDepth++;
                continue;
            }
            if (current == ']') {
                bracketDepth--;
                continue;
            }
            if (current == '{') {
                braceDepth++;
                continue;
            }
            if (current == '}') {
                braceDepth--;
                continue;
            }
            if (parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                for (String operator : COMPARISON_OPERATORS) {
                    if (source.startsWith(operator, index)) {
                        return Optional.of(new Comparison(operator, index));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static int findMatching(String source, int openIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int index = openIndex; index < source.length(); index++) {
            char current = source.charAt(index);
            if (inString || inChar) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (inString && current == '"') {
                    inString = false;
                } else if (inChar && current == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '\'') {
                inChar = true;
                continue;
            }
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static boolean isMethodCallExpression(String expression) {
        if (!expression.endsWith(")") || expression.indexOf('(') <= 0) {
            return false;
        }
        return findMatching(expression, expression.lastIndexOf('('), '(', ')') == expression.length() - 1;
    }

    private static String text(String source, int start, int end) {
        return JavaDecisionSourceSupport.normalizeWhitespace(source.substring(start, end));
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

    private record Range(int start, int end) {
    }

    private record Comparison(String operator, int operatorStart) {
    }
}
