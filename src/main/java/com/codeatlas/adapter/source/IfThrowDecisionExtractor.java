package com.codeatlas.adapter.source;

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
import com.codeatlas.core.decision.DecisionTraceExtractor;
import com.codeatlas.core.decision.DecisionTraceMetadata;
import com.codeatlas.core.decision.UnresolvedDecision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class IfThrowDecisionExtractor implements DecisionTraceExtractor {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "\\bpackage\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\s*;"
    );
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );
    private static final Pattern SUBJECT_METHOD_PATTERN = Pattern.compile(
            "\\b([a-z_$][A-Za-z0-9_$]*)\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\("
    );

    @Override
    public DecisionTrace analyze(Path projectPath, String entrypoint) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Entrypoint parsedEntrypoint = Entrypoint.parse(entrypoint);
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();

        SourceFile sourceFile = findSourceFile(normalizedProjectPath, parsedEntrypoint)
                .orElseThrow(() -> new IllegalArgumentException("Could not find Java type "
                        + parsedEntrypoint.classQualifiedName()
                        + " under " + normalizedProjectPath));
        MethodRange methodRange = findMethod(sourceFile, parsedEntrypoint)
                .orElseThrow(() -> new IllegalArgumentException("Could not find method "
                        + parsedEntrypoint.methodName()
                        + " in "
                        + parsedEntrypoint.classQualifiedName()
                        + " at "
                        + sourceFile.relativePath()));

        List<DecisionNode> decisions = extractDecisions(parsedEntrypoint, sourceFile, methodRange);
        return new DecisionTrace(
                SCHEMA_VERSION,
                DETERMINISTIC_GENERATED_AT,
                portablePath(projectPath),
                new DecisionScope("ENTRYPOINT", parsedEntrypoint.normalized(), null),
                new DecisionArtifactSource(null, null),
                decisions,
                List.<UnresolvedDecision>of(),
                new DecisionTraceMetadata(
                        "if-throw-source-text-decision-extractor",
                        "phase-4.1-decision-trace-extractor",
                        true,
                        "source-text"
                )
        );
    }

    private static Optional<SourceFile> findSourceFile(Path projectPath, Entrypoint entrypoint) {
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
            throw new IllegalArgumentException("Failed to read Java sources under " + sourceRoot + ": " + exception.getMessage(), exception);
        }
    }

    private static Optional<SourceFile> readSourceFile(Path projectPath, Path sourcePath) {
        try {
            Path relativePath = projectPath.relativize(sourcePath);
            String source = Files.readString(sourcePath);
            return Optional.of(new SourceFile(relativePath.toString().replace('\\', '/'), source, maskCommentsAndStrings(source)));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read Java source " + sourcePath + ": " + exception.getMessage(), exception);
        }
    }

    private static Optional<MethodRange> findMethod(SourceFile sourceFile, Entrypoint entrypoint) {
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

    private static List<DecisionNode> extractDecisions(
            Entrypoint entrypoint,
            SourceFile sourceFile,
            MethodRange methodRange
    ) {
        List<DecisionNode> decisions = new ArrayList<>();
        String maskedSource = sourceFile.maskedSource();
        int index = methodRange.bodyStart();
        int ordinal = 1;
        while (index < methodRange.bodyEnd()) {
            int ifStart = indexOfWord(maskedSource, "if", index, methodRange.bodyEnd());
            if (ifStart < 0) {
                break;
            }

            Optional<IfThrowDecision> parsedDecision = parseIfThrow(sourceFile, methodRange, ifStart);
            if (parsedDecision.isPresent()) {
                decisions.add(toDecisionNode(entrypoint, sourceFile, parsedDecision.get(), ordinal));
                ordinal++;
                index = parsedDecision.get().blockEnd() + 1;
            } else {
                index = ifStart + 2;
            }
        }
        return List.copyOf(decisions);
    }

    private static Optional<IfThrowDecision> parseIfThrow(SourceFile sourceFile, MethodRange methodRange, int ifStart) {
        String maskedSource = sourceFile.maskedSource();
        int conditionOpen = skipWhitespace(maskedSource, ifStart + 2, methodRange.bodyEnd());
        if (conditionOpen >= methodRange.bodyEnd() || maskedSource.charAt(conditionOpen) != '(') {
            return Optional.empty();
        }
        int conditionClose = findMatching(maskedSource, conditionOpen, '(', ')');
        if (conditionClose < 0 || conditionClose >= methodRange.bodyEnd()) {
            return Optional.empty();
        }

        int blockOpen = skipWhitespace(maskedSource, conditionClose + 1, methodRange.bodyEnd());
        if (blockOpen >= methodRange.bodyEnd() || maskedSource.charAt(blockOpen) != '{') {
            return Optional.empty();
        }
        int blockClose = findMatching(maskedSource, blockOpen, '{', '}');
        if (blockClose < 0 || blockClose > methodRange.bodyEnd()) {
            return Optional.empty();
        }

        Optional<ThrowStatement> throwStatement = parseDirectThrow(sourceFile, blockOpen + 1, blockClose);
        if (throwStatement.isEmpty()) {
            return Optional.empty();
        }

        String condition = normalizeWhitespace(sourceFile.source().substring(conditionOpen + 1, conditionClose));
        String snippet = normalizeWhitespace(sourceFile.source().substring(ifStart, blockClose + 1));
        return Optional.of(new IfThrowDecision(
                ifStart,
                blockClose,
                condition,
                snippet,
                throwStatement.get().exceptionType(),
                throwStatement.get().message()
        ));
    }

    private static Optional<ThrowStatement> parseDirectThrow(SourceFile sourceFile, int bodyStart, int bodyEnd) {
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
        index = skipWhitespace(maskedSource, constructorClose + 1, bodyEnd);
        if (index >= bodyEnd || maskedSource.charAt(index) != ';') {
            return Optional.empty();
        }
        index = skipWhitespace(maskedSource, index + 1, bodyEnd);
        if (index != bodyEnd) {
            return Optional.empty();
        }
        return Optional.of(new ThrowStatement(exceptionType, message));
    }

    private static DecisionNode toDecisionNode(
            Entrypoint entrypoint,
            SourceFile sourceFile,
            IfThrowDecision parsedDecision,
            int ordinal
    ) {
        String method = entrypoint.normalized();
        DecisionCategory category = category(parsedDecision.condition(), parsedDecision.exceptionType());
        String normalizedCondition = normalizedCondition(parsedDecision.condition());
        String subjectName = subjectName(parsedDecision.condition()).orElse(null);
        List<DecisionSubject> subjects = subjectName == null
                ? List.of()
                : List.of(new DecisionSubject(subjectName, "INPUT_FIELD"));
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

    private static Optional<String> subjectName(String condition) {
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

    private static String normalizedCondition(String condition) {
        Optional<String> subject = subjectName(condition);
        if (subject.isPresent()) {
            String subjectAsAccessor = subject.get() + "()";
            if (condition.equals(subjectAsAccessor + " == null || " + subjectAsAccessor + ".isBlank()")) {
                return "isNullOrBlank(" + subject.get() + ")";
            }
            if (condition.equals(subjectAsAccessor + " == null || " + subjectAsAccessor + ".isEmpty()")) {
                return "isNullOrEmpty(" + subject.get() + ")";
            }
        }
        return null;
    }

    private static boolean hasIdentifierBoundary(String source, int start, int end) {
        boolean leftBoundary = start == 0 || !Character.isJavaIdentifierPart(source.charAt(start - 1));
        boolean rightBoundary = end >= source.length() || !Character.isJavaIdentifierPart(source.charAt(end));
        return leftBoundary && rightBoundary;
    }

    private static int indexOfWord(String source, String word, int start, int limit) {
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

    private static boolean startsWithWord(String source, int index, String word) {
        return index >= 0
                && index + word.length() <= source.length()
                && source.startsWith(word, index)
                && hasIdentifierBoundary(source, index, index + word.length());
    }

    private static int skipWhitespace(String source, int index, int limit) {
        int current = index;
        while (current < limit && Character.isWhitespace(source.charAt(current))) {
            current++;
        }
        return current;
    }

    private static int findMatching(String source, int openIndex, char open, char close) {
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

    private static String normalizeWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private record Entrypoint(String classQualifiedName, String methodName) {
        private static Entrypoint parse(String value) {
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

        private String packageName() {
            int separator = classQualifiedName.lastIndexOf('.');
            return separator < 0 ? "" : classQualifiedName.substring(0, separator);
        }

        private String classSimpleName() {
            int separator = classQualifiedName.lastIndexOf('.');
            return separator < 0 ? classQualifiedName : classQualifiedName.substring(separator + 1);
        }

        private String normalized() {
            return classQualifiedName + "." + methodName;
        }
    }

    private record SourceFile(String relativePath, String source, String maskedSource) {
        private boolean containsType(String expectedPackageName, String expectedSimpleName) {
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

        private int lineOf(int offset) {
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

    private record MethodRange(int methodNameStart, int bodyStart, int bodyEnd) {
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

    private record ThrowStatement(String exceptionType, String message) {
    }
}
