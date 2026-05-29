package com.codeatlas.adapter.java.source.decision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class JavaDecisionSourceSupport {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "\\bpackage\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\s*;"
    );
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );
    private static final Pattern SUBJECT_METHOD_PATTERN = Pattern.compile(
            "\\b([a-z_$][A-Za-z0-9_$]*)\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\("
    );
    private static final Pattern JAVA_QUALIFIED_NAME = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*"
    );
    private static final Pattern NUMERIC_LITERAL = Pattern.compile(
            "[-+]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)(?:[dDfFlL])?"
    );

    private JavaDecisionSourceSupport() {
    }

    static Optional<SourceFile> findSourceFile(Path projectPath, Entrypoint entrypoint) {
        String packagePath = entrypoint.classQualifiedName().replace('.', '/') + ".java";
        Path expectedFile = projectPath.resolve("src/main/java").resolve(packagePath);
        if (Files.isRegularFile(expectedFile)) {
            return readSourceFile(projectPath, expectedFile);
        }

        Path sourceRoot = projectPath.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) {
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .map(path -> readSourceFile(projectPath, path))
                    .flatMap(Optional::stream)
                    .filter(sourceFile -> sourceFile.containsType(entrypoint.packageName(), entrypoint.classSimpleName()))
                    .findFirst();
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Failed to read Java sources under " + sourceRoot + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    static Optional<MethodRange> findMethod(SourceFile sourceFile, Entrypoint entrypoint) {
        String maskedSource = sourceFile.maskedSource();
        int index = 0;
        while (index < maskedSource.length()) {
            int methodNameStart = maskedSource.indexOf(entrypoint.methodName(), index);
            if (methodNameStart < 0) {
                return Optional.empty();
            }
            int methodNameEnd = methodNameStart + entrypoint.methodName().length();
            if (!hasIdentifierBoundary(maskedSource, methodNameStart, methodNameEnd)) {
                index = methodNameEnd;
                continue;
            }

            int openParen = skipWhitespace(maskedSource, methodNameEnd, maskedSource.length());
            if (openParen >= maskedSource.length() || maskedSource.charAt(openParen) != '(') {
                index = methodNameEnd;
                continue;
            }
            int closeParen = findMatching(maskedSource, openParen, '(', ')');
            if (closeParen < 0) {
                index = methodNameEnd;
                continue;
            }

            int afterParameters = skipWhitespace(maskedSource, closeParen + 1, maskedSource.length());
            if (startsWithWord(maskedSource, afterParameters, "throws")) {
                afterParameters += "throws".length();
                while (afterParameters < maskedSource.length()
                        && maskedSource.charAt(afterParameters) != '{'
                        && maskedSource.charAt(afterParameters) != ';') {
                    afterParameters++;
                }
            }
            afterParameters = skipWhitespace(maskedSource, afterParameters, maskedSource.length());
            if (afterParameters >= maskedSource.length() || maskedSource.charAt(afterParameters) != '{') {
                index = methodNameEnd;
                continue;
            }

            int closeBrace = findMatching(maskedSource, afterParameters, '{', '}');
            if (closeBrace < 0) {
                index = methodNameEnd;
                continue;
            }
            return Optional.of(new MethodRange(methodNameStart, afterParameters + 1, closeBrace));
        }
        return Optional.empty();
    }

    static Optional<IfStatement> parseIfStatement(SourceFile sourceFile, MethodRange methodRange, int ifStart) {
        String maskedSource = sourceFile.maskedSource();
        int conditionOpen = skipWhitespace(maskedSource, ifStart + 2, methodRange.bodyEnd());
        if (conditionOpen >= methodRange.bodyEnd() || maskedSource.charAt(conditionOpen) != '(') {
            return Optional.empty();
        }
        int conditionClose = findMatching(maskedSource, conditionOpen, '(', ')');
        if (conditionClose < 0 || conditionClose >= methodRange.bodyEnd()) {
            return Optional.empty();
        }

        int thenStart = skipWhitespace(maskedSource, conditionClose + 1, methodRange.bodyEnd());
        Optional<StatementRange> thenRange = parseStatement(maskedSource, thenStart, methodRange.bodyEnd());
        if (thenRange.isEmpty()) {
            return Optional.empty();
        }

        int statementEnd = thenRange.get().statementEnd();
        int afterThen = skipWhitespace(maskedSource, statementEnd, methodRange.bodyEnd());
        boolean hasElse = startsWithWord(maskedSource, afterThen, "else");
        int elseStart = -1;
        int elseEnd = -1;
        boolean elseHasBlock = false;
        int elseBodyStart = -1;
        int elseBodyEnd = -1;
        if (hasElse) {
            elseStart = afterThen;
            int elseStatementStart = skipWhitespace(maskedSource, afterThen + "else".length(), methodRange.bodyEnd());
            Optional<StatementRange> elseRange = parseStatement(maskedSource, elseStatementStart, methodRange.bodyEnd());
            if (elseRange.isPresent()) {
                elseHasBlock = elseRange.get().hasBlock();
                elseBodyStart = elseRange.get().bodyStart();
                elseBodyEnd = elseRange.get().bodyEnd();
                elseEnd = elseRange.get().statementEnd();
                statementEnd = elseEnd;
            } else {
                elseEnd = elseStatementStart;
                statementEnd = elseStatementStart;
            }
        }

        String condition = normalizeWhitespace(sourceFile.source().substring(conditionOpen + 1, conditionClose));
        String snippet = normalizeWhitespace(sourceFile.source().substring(ifStart, statementEnd));
        return Optional.of(new IfStatement(
                ifStart,
                statementEnd,
                condition,
                snippet,
                thenRange.get().hasBlock(),
                thenRange.get().bodyStart(),
                thenRange.get().bodyEnd(),
                hasElse,
                elseStart,
                elseEnd,
                elseHasBlock,
                elseBodyStart,
                elseBodyEnd
        ));
    }

    static Optional<ThrowStatement> parseDirectThrow(SourceFile sourceFile, int bodyStart, int bodyEnd) {
        String maskedSource = sourceFile.maskedSource();
        int index = skipWhitespace(maskedSource, bodyStart, bodyEnd);
        if (!startsWithWord(maskedSource, index, "throw")) {
            return Optional.empty();
        }
        index = skipWhitespace(maskedSource, index + "throw".length(), bodyEnd);
        if (!startsWithWord(maskedSource, index, "new")) {
            return Optional.empty();
        }
        index = skipWhitespace(maskedSource, index + "new".length(), bodyEnd);

        int exceptionTypeStart = index;
        while (index < bodyEnd) {
            char current = maskedSource.charAt(index);
            if (Character.isJavaIdentifierPart(current) || current == '.' || current == '$') {
                index++;
            } else {
                break;
            }
        }
        if (exceptionTypeStart == index) {
            return Optional.empty();
        }
        String exceptionType = sourceFile.source().substring(exceptionTypeStart, index).trim();
        index = skipWhitespace(maskedSource, index, bodyEnd);
        if (index >= bodyEnd || maskedSource.charAt(index) != '(') {
            return Optional.empty();
        }
        int constructorClose = findMatching(maskedSource, index, '(', ')');
        if (constructorClose < 0 || constructorClose > bodyEnd) {
            return Optional.empty();
        }

        String constructorArguments = sourceFile.source().substring(index + 1, constructorClose);
        String message = firstStringLiteral(constructorArguments);
        boolean hasDirectLiteralMessage = isSingleStringLiteralArgument(constructorArguments);
        index = skipWhitespace(maskedSource, constructorClose + 1, bodyEnd);
        if (index >= bodyEnd || maskedSource.charAt(index) != ';') {
            return Optional.empty();
        }
        index = skipWhitespace(maskedSource, index + 1, bodyEnd);
        if (index != bodyEnd) {
            return Optional.empty();
        }
        return Optional.of(new ThrowStatement(exceptionType, message, hasDirectLiteralMessage));
    }

    static Optional<ThrowStatement> parseFinalThrowWithAllowedPreStatements(
            SourceFile sourceFile,
            int bodyStart,
            int bodyEnd
    ) {
        String maskedSource = sourceFile.maskedSource();
        int index = skipWhitespace(maskedSource, bodyStart, bodyEnd);
        while (index < bodyEnd) {
            if (startsWithWord(maskedSource, index, "throw")) {
                return parseDirectThrow(sourceFile, index, bodyEnd);
            }
            if (startsWithDisallowedPreThrowStatement(maskedSource, index)) {
                return Optional.empty();
            }

            int semicolon = findTopLevelSemicolon(maskedSource, index, bodyEnd);
            if (semicolon < 0) {
                return Optional.empty();
            }
            if (!isAllowedPreThrowExpressionStatement(sourceFile, index, semicolon)) {
                return Optional.empty();
            }
            index = skipWhitespace(maskedSource, semicolon + 1, bodyEnd);
        }
        return Optional.empty();
    }

    static Optional<ReturnStatement> parseDirectReturn(SourceFile sourceFile, int bodyStart, int bodyEnd) {
        String maskedSource = sourceFile.maskedSource();
        int index = skipWhitespace(maskedSource, bodyStart, bodyEnd);
        if (!startsWithWord(maskedSource, index, "return")) {
            return Optional.empty();
        }

        int expressionStart = skipWhitespace(maskedSource, index + "return".length(), bodyEnd);
        int semicolon = findTopLevelSemicolon(maskedSource, expressionStart, bodyEnd);
        if (semicolon < 0) {
            return Optional.empty();
        }
        int afterSemicolon = skipWhitespace(maskedSource, semicolon + 1, bodyEnd);
        if (afterSemicolon != bodyEnd) {
            return Optional.empty();
        }

        String expression = sourceFile.source().substring(expressionStart, semicolon).trim();
        return Optional.of(new ReturnStatement(expression));
    }

    static boolean isSimpleReturnExpression(String expression) {
        String trimmed = expression.trim();
        if (trimmed.isEmpty()
                || trimmed.equals("null")
                || trimmed.equals("true")
                || trimmed.equals("false")
                || NUMERIC_LITERAL.matcher(trimmed).matches()
                || isQuotedLiteral(trimmed)
                || isSimpleQualifiedName(trimmed)) {
            return true;
        }
        if (trimmed.contains("->")
                || trimmed.contains("::")
                || trimmed.contains("?")
                || trimmed.matches(".*\\bnew\\b.*")
                || trimmed.matches(".*\\)\\s*\\..*")) {
            return false;
        }
        return isSimpleMethodCall(trimmed);
    }

    static String returnTarget(String expression) {
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int openParen = firstTopLevelOpenParen(trimmed);
        if (openParen > 0 && trimmed.endsWith(")") && isSimpleQualifiedName(trimmed.substring(0, openParen).trim())) {
            return trimmed.substring(0, openParen).trim();
        }
        return trimmed;
    }

    static Optional<String> subjectName(String condition) {
        Matcher matcher = SUBJECT_METHOD_PATTERN.matcher(condition);
        while (matcher.find()) {
            String receiver = matcher.group(1);
            String method = matcher.group(2);
            if (!"this".equals(receiver) && !"super".equals(receiver)) {
                return Optional.of(receiver + "." + method);
            }
        }
        return Optional.empty();
    }

    static String normalizedCondition(String condition) {
        Optional<String> subject = subjectName(condition);
        if (subject.isPresent()) {
            String subjectAsAccessor = subject.get() + "()";
            if (condition.equals(subjectAsAccessor + " == null || " + subjectAsAccessor + ".isBlank()")) {
                return "isNullOrBlank(" + subject.get() + ")";
            }
            if (condition.equals(subjectAsAccessor + " == null || " + subjectAsAccessor + ".isEmpty()")) {
                return "isNullOrEmpty(" + subject.get() + ")";
            }
            if (condition.equals(subjectAsAccessor)) {
                return subject.get();
            }
        }
        return null;
    }

    static boolean containsNestedIf(SourceFile sourceFile, IfStatement ifStatement) {
        return containsWord(sourceFile.maskedSource(), "if", ifStatement.bodyStart(), ifStatement.bodyEnd());
    }

    static boolean containsTopLevelWord(SourceFile sourceFile, IfStatement ifStatement, String word) {
        return containsTopLevelWord(sourceFile.maskedSource(), word, ifStatement.bodyStart(), ifStatement.bodyEnd());
    }

    static boolean startsWithBodyWord(SourceFile sourceFile, IfStatement ifStatement, String word) {
        int bodyStart = skipWhitespace(sourceFile.maskedSource(), ifStatement.bodyStart(), ifStatement.bodyEnd());
        return startsWithWord(sourceFile.maskedSource(), bodyStart, word);
    }

    static boolean containsBodyWord(SourceFile sourceFile, IfStatement ifStatement, String word) {
        return containsWord(sourceFile.maskedSource(), word, ifStatement.bodyStart(), ifStatement.bodyEnd());
    }

    static boolean hasIdentifierBoundary(String source, int start, int end) {
        boolean leftBoundary = start == 0 || !Character.isJavaIdentifierPart(source.charAt(start - 1));
        boolean rightBoundary = end >= source.length() || !Character.isJavaIdentifierPart(source.charAt(end));
        return leftBoundary && rightBoundary;
    }

    static int indexOfWord(String source, String word, int start, int limit) {
        int index = start;
        while (index >= 0 && index < limit) {
            int found = source.indexOf(word, index);
            if (found < 0 || found >= limit) {
                return -1;
            }
            if (hasIdentifierBoundary(source, found, found + word.length())) {
                return found;
            }
            index = found + word.length();
        }
        return -1;
    }

    static boolean startsWithWord(String source, int index, String word) {
        return index >= 0
                && index + word.length() <= source.length()
                && source.startsWith(word, index)
                && hasIdentifierBoundary(source, index, index + word.length());
    }

    static int skipWhitespace(String source, int index, int limit) {
        int current = index;
        while (current < limit && Character.isWhitespace(source.charAt(current))) {
            current++;
        }
        return current;
    }

    static int findMatching(String source, int openIndex, char open, char close) {
        if (openIndex < 0 || openIndex >= source.length() || source.charAt(openIndex) != open) {
            return -1;
        }
        int depth = 0;
        for (int index = openIndex; index < source.length(); index++) {
            char current = source.charAt(index);
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

    static String normalizeWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static Optional<SourceFile> readSourceFile(Path projectPath, Path sourcePath) {
        try {
            Path relativePath = projectPath.relativize(sourcePath);
            String source = Files.readString(sourcePath);
            return Optional.of(new SourceFile(
                    relativePath.toString().replace('\\', '/'),
                    source,
                    maskCommentsAndStrings(source)
            ));
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Failed to read Java source " + sourcePath + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    private static Optional<StatementRange> parseStatement(String maskedSource, int statementStart, int limit) {
        int start = skipWhitespace(maskedSource, statementStart, limit);
        if (start >= limit) {
            return Optional.empty();
        }
        if (maskedSource.charAt(start) == '{') {
            int closeBrace = findMatching(maskedSource, start, '{', '}');
            if (closeBrace < 0 || closeBrace > limit) {
                return Optional.empty();
            }
            return Optional.of(new StatementRange(true, start + 1, closeBrace, closeBrace + 1));
        }
        if (startsWithWord(maskedSource, start, "if")) {
            Optional<StatementEnd> nestedIf = parseIfStatementEnd(maskedSource, start, limit);
            return nestedIf.map(statementEnd -> new StatementRange(false, start, statementEnd.value(), statementEnd.value()));
        }

        int semicolon = findTopLevelSemicolon(maskedSource, start, limit);
        if (semicolon < 0) {
            return Optional.empty();
        }
        return Optional.of(new StatementRange(false, start, semicolon + 1, semicolon + 1));
    }

    private static Optional<StatementEnd> parseIfStatementEnd(String maskedSource, int ifStart, int limit) {
        int conditionOpen = skipWhitespace(maskedSource, ifStart + 2, limit);
        if (conditionOpen >= limit || maskedSource.charAt(conditionOpen) != '(') {
            return Optional.empty();
        }
        int conditionClose = findMatching(maskedSource, conditionOpen, '(', ')');
        if (conditionClose < 0 || conditionClose >= limit) {
            return Optional.empty();
        }
        Optional<StatementRange> thenRange = parseStatement(maskedSource, conditionClose + 1, limit);
        if (thenRange.isEmpty()) {
            return Optional.empty();
        }
        int statementEnd = thenRange.get().statementEnd();
        int afterThen = skipWhitespace(maskedSource, statementEnd, limit);
        if (startsWithWord(maskedSource, afterThen, "else")) {
            Optional<StatementRange> elseRange = parseStatement(maskedSource, afterThen + "else".length(), limit);
            if (elseRange.isPresent()) {
                statementEnd = elseRange.get().statementEnd();
            }
        }
        return Optional.of(new StatementEnd(statementEnd));
    }

    private static int findTopLevelSemicolon(String maskedSource, int start, int limit) {
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
            } else if (current == ';' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static boolean containsWord(String source, String word, int start, int limit) {
        return indexOfWord(source, word, start, limit) >= 0;
    }

    private static boolean containsTopLevelWord(String source, String word, int start, int limit) {
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        int index = start;
        while (index < limit) {
            char current = source.charAt(index);
            if (current == '(') {
                parenDepth++;
                index++;
                continue;
            }
            if (current == ')') {
                parenDepth--;
                index++;
                continue;
            }
            if (current == '[') {
                bracketDepth++;
                index++;
                continue;
            }
            if (current == ']') {
                bracketDepth--;
                index++;
                continue;
            }
            if (current == '{') {
                braceDepth++;
                index++;
                continue;
            }
            if (current == '}') {
                braceDepth--;
                index++;
                continue;
            }
            if (parenDepth == 0
                    && bracketDepth == 0
                    && braceDepth == 0
                    && startsWithWord(source, index, word)) {
                return true;
            }
            index++;
        }
        return false;
    }

    private static boolean startsWithDisallowedPreThrowStatement(String maskedSource, int index) {
        return startsWithWord(maskedSource, index, "if")
                || startsWithWord(maskedSource, index, "switch")
                || startsWithWord(maskedSource, index, "try")
                || startsWithWord(maskedSource, index, "catch")
                || startsWithWord(maskedSource, index, "for")
                || startsWithWord(maskedSource, index, "while")
                || startsWithWord(maskedSource, index, "do")
                || startsWithWord(maskedSource, index, "return")
                || startsWithWord(maskedSource, index, "throw");
    }

    private static boolean isAllowedPreThrowExpressionStatement(SourceFile sourceFile, int start, int semicolon) {
        String statement = sourceFile.source().substring(start, semicolon).trim();
        String maskedStatement = sourceFile.maskedSource().substring(start, semicolon);
        if (statement.isBlank()
                || statement.contains("->")
                || statement.contains("::")
                || statement.contains("{")
                || statement.contains("}")) {
            return false;
        }
        for (String disallowedWord : List.of(
                "if",
                "switch",
                "try",
                "catch",
                "for",
                "while",
                "do",
                "return",
                "throw")) {
            if (indexOfWord(maskedStatement, disallowedWord, 0, maskedStatement.length()) >= 0) {
                return false;
            }
        }
        return isSimpleMethodCall(statement);
    }

    private static String maskCommentsAndStrings(String source) {
        StringBuilder masked = new StringBuilder(source);
        int index = 0;
        while (index < masked.length()) {
            char current = masked.charAt(index);
            if (current == '"' || current == '\'') {
                char quote = current;
                index++;
                while (index < masked.length()) {
                    char value = masked.charAt(index);
                    if (value == '\\') {
                        masked.setCharAt(index, ' ');
                        if (index + 1 < masked.length()) {
                            masked.setCharAt(index + 1, ' ');
                        }
                        index += 2;
                        continue;
                    }
                    if (value == quote) {
                        index++;
                        break;
                    }
                    if (value != '\n' && value != '\r') {
                        masked.setCharAt(index, ' ');
                    }
                    index++;
                }
                continue;
            }
            if (current == '/' && index + 1 < masked.length() && masked.charAt(index + 1) == '/') {
                masked.setCharAt(index, ' ');
                masked.setCharAt(index + 1, ' ');
                index += 2;
                while (index < masked.length() && masked.charAt(index) != '\n') {
                    masked.setCharAt(index, ' ');
                    index++;
                }
                continue;
            }
            if (current == '/' && index + 1 < masked.length() && masked.charAt(index + 1) == '*') {
                masked.setCharAt(index, ' ');
                masked.setCharAt(index + 1, ' ');
                index += 2;
                while (index + 1 < masked.length()) {
                    if (masked.charAt(index) == '*' && masked.charAt(index + 1) == '/') {
                        masked.setCharAt(index, ' ');
                        masked.setCharAt(index + 1, ' ');
                        index += 2;
                        break;
                    }
                    if (masked.charAt(index) != '\n' && masked.charAt(index) != '\r') {
                        masked.setCharAt(index, ' ');
                    }
                    index++;
                }
                continue;
            }
            index++;
        }
        return masked.toString();
    }

    private static String firstStringLiteral(String value) {
        int index = 0;
        while (index < value.length()) {
            if (value.charAt(index) != '"') {
                index++;
                continue;
            }
            StringBuilder literal = new StringBuilder();
            index++;
            while (index < value.length()) {
                char current = value.charAt(index);
                if (current == '\\' && index + 1 < value.length()) {
                    char escaped = value.charAt(index + 1);
                    literal.append(switch (escaped) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case '"' -> '"';
                        case '\\' -> '\\';
                        default -> escaped;
                    });
                    index += 2;
                    continue;
                }
                if (current == '"') {
                    return literal.toString();
                }
                literal.append(current);
                index++;
            }
        }
        return null;
    }

    private static boolean isSingleStringLiteralArgument(String value) {
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '"') {
            return false;
        }
        int index = 1;
        while (index < trimmed.length()) {
            char current = trimmed.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == '"') {
                return index == trimmed.length() - 1;
            }
            index++;
        }
        return false;
    }

    private static boolean isQuotedLiteral(String value) {
        return (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
                || (value.length() >= 3 && value.startsWith("'") && value.endsWith("'"));
    }

    private static boolean isSimpleQualifiedName(String value) {
        return JAVA_QUALIFIED_NAME.matcher(value).matches();
    }

    private static boolean isSimpleMethodCall(String value) {
        int openParen = firstTopLevelOpenParen(value);
        if (openParen <= 0 || !value.endsWith(")")) {
            return false;
        }
        String callee = value.substring(0, openParen).trim();
        if (!isSimpleQualifiedName(callee)) {
            return false;
        }
        String arguments = value.substring(openParen + 1, value.length() - 1).trim();
        if (arguments.isEmpty()) {
            return true;
        }
        for (String argument : splitTopLevelArguments(arguments)) {
            if (!isSimpleReturnExpression(argument)) {
                return false;
            }
        }
        return true;
    }

    private static int firstTopLevelOpenParen(String value) {
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth--;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth--;
            } else if (current == '(' && bracketDepth == 0 && braceDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> splitTopLevelArguments(String arguments) {
        List<String> values = new ArrayList<>();
        int start = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        for (int index = 0; index < arguments.length(); index++) {
            char current = arguments.charAt(index);
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
                values.add(arguments.substring(start, index).trim());
                start = index + 1;
            }
        }
        values.add(arguments.substring(start).trim());
        return values;
    }

    record Entrypoint(String classQualifiedName, String methodName) {
        static Entrypoint parse(String value) {
            if (value == null || value.isBlank() || value.contains(" ")) {
                throw new IllegalArgumentException("Missing or invalid required argument: --entrypoint");
            }
            int lastSeparator = value.lastIndexOf('.');
            int firstSeparator = value.indexOf('.');
            if (firstSeparator <= 0 || lastSeparator <= firstSeparator || lastSeparator >= value.length() - 1) {
                throw new IllegalArgumentException("Missing or invalid required argument: --entrypoint");
            }
            return new Entrypoint(value.substring(0, lastSeparator), value.substring(lastSeparator + 1));
        }

        String packageName() {
            int separator = classQualifiedName.lastIndexOf('.');
            return separator < 0 ? "" : classQualifiedName.substring(0, separator);
        }

        String classSimpleName() {
            int separator = classQualifiedName.lastIndexOf('.');
            return separator < 0 ? classQualifiedName : classQualifiedName.substring(separator + 1);
        }

        String normalized() {
            return classQualifiedName + "." + methodName;
        }
    }

    record SourceFile(String relativePath, String source, String maskedSource) {
        boolean containsType(String expectedPackageName, String expectedSimpleName) {
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(maskedSource);
            String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
            if (!packageName.equals(expectedPackageName)) {
                return false;
            }

            Matcher typeMatcher = TYPE_PATTERN.matcher(maskedSource);
            while (typeMatcher.find()) {
                if (typeMatcher.group(2).equals(expectedSimpleName)) {
                    return true;
                }
            }
            return false;
        }

        int lineOf(int offset) {
            int line = 1;
            int limit = Math.min(offset, source.length());
            for (int index = 0; index < limit; index++) {
                if (source.charAt(index) == '\n') {
                    line++;
                }
            }
            return line;
        }
    }

    record MethodRange(int methodNameStart, int bodyStart, int bodyEnd) {
    }

    record IfStatement(
            int ifStart,
            int statementEnd,
            String condition,
            String snippet,
            boolean hasBlock,
            int bodyStart,
            int bodyEnd,
            boolean hasElse,
            int elseStart,
            int elseEnd,
            boolean elseHasBlock,
            int elseBodyStart,
            int elseBodyEnd
    ) {
    }

    record ThrowStatement(String exceptionType, String message, boolean hasDirectLiteralMessage) {
    }

    record ReturnStatement(String expression) {
    }

    private record StatementRange(boolean hasBlock, int bodyStart, int bodyEnd, int statementEnd) {
    }

    private record StatementEnd(int value) {
    }
}
