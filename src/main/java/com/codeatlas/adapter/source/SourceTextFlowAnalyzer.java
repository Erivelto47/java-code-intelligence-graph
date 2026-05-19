package com.codeatlas.adapter.source;

import com.codeatlas.core.analyzer.FlowAnalyzer;
import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SourceTextFlowAnalyzer implements FlowAnalyzer {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "\\bpackage\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\s*;"
    );
    private static final Set<String> RESERVED_CALL_WORDS = Set.of(
            "assert",
            "case",
            "catch",
            "class",
            "do",
            "else",
            "enum",
            "for",
            "if",
            "interface",
            "new",
            "record",
            "return",
            "switch",
            "synchronized",
            "throw",
            "try",
            "while"
    );

    @Override
    public FlowGraph analyze(Path projectPath, String entrypoint) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Entrypoint parsedEntrypoint = Entrypoint.parse(entrypoint);
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();
        JavaSourceFile sourceFile = findSourceFile(normalizedProjectPath, parsedEntrypoint);
        LineMap lineMap = LineMap.from(sourceFile.source());
        String strippedSource = stripCommentsAndStrings(sourceFile.source());

        TypeRegion typeRegion = findTypeRegion(strippedSource, lineMap, parsedEntrypoint);
        Map<String, JavaMethod> methods = findMethods(strippedSource, lineMap, typeRegion, parsedEntrypoint.className());
        JavaMethod entryMethod = methods.get(parsedEntrypoint.methodName());
        if (entryMethod == null) {
            throw new IllegalArgumentException("Could not find method " + parsedEntrypoint.methodName()
                    + " in " + parsedEntrypoint.classQualifiedName()
                    + " at " + sourceFile.relativePath());
        }

        String classNodeId = "class:" + parsedEntrypoint.classQualifiedName();
        String entrypointNodeId = "method:" + parsedEntrypoint.normalized();

        LinkedHashMap<String, GraphNode> nodesById = new LinkedHashMap<>();
        List<GraphEdge> edges = new ArrayList<>();

        nodesById.put(classNodeId, classNode(sourceFile, typeRegion, parsedEntrypoint, classNodeId));
        nodesById.put(entrypointNodeId, methodNode(
                sourceFile,
                parsedEntrypoint,
                entrypointNodeId,
                parsedEntrypoint.normalized(),
                parsedEntrypoint.methodName(),
                entryMethod.line(),
                "local",
                true,
                null
        ));

        edges.add(new GraphEdge(
                "declares:" + classNodeId + "->" + entrypointNodeId,
                "DECLARES",
                classNodeId,
                entrypointNodeId,
                edgeAttributes(entryMethod.line(), null)
        ));

        List<CallExpression> calls = findCalls(sourceFile.source(), strippedSource, lineMap, entryMethod);
        int callOrdinal = 1;
        for (CallExpression call : calls) {
            ResolvedCall resolvedCall = resolveCall(sourceFile, parsedEntrypoint, methods, call);
            nodesById.putIfAbsent(resolvedCall.nodeId(), methodNode(
                    sourceFile,
                    parsedEntrypoint,
                    resolvedCall.nodeId(),
                    resolvedCall.qualifiedName(),
                    resolvedCall.methodName(),
                    resolvedCall.line(),
                    resolvedCall.resolution(),
                    false,
                    resolvedCall.receiverName()
            ));

            edges.add(new GraphEdge(
                    "calls:" + entrypointNodeId + "->" + resolvedCall.nodeId() + ":" + callOrdinal,
                    "CALLS",
                    entrypointNodeId,
                    resolvedCall.nodeId(),
                    edgeAttributes(call.line(), call.expression())
            ));
            callOrdinal++;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analyzer", "source-text-flow-analyzer");
        metadata.put("phase", "phase-1-mvp");
        metadata.put("deterministic", true);
        metadata.put("source", "source-text");

        return new FlowGraph(
                SCHEMA_VERSION,
                parsedEntrypoint.normalized(),
                DETERMINISTIC_GENERATED_AT,
                List.copyOf(nodesById.values()),
                List.copyOf(edges),
                metadata
        );
    }

    private static JavaSourceFile findSourceFile(Path projectPath, Entrypoint entrypoint) {
        if (!Files.isDirectory(projectPath)) {
            throw new IllegalArgumentException("Project path must be an existing directory: " + projectPath);
        }

        List<Path> candidates;
        try (Stream<Path> paths = Files.walk(projectPath)) {
            candidates = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(entrypoint.className() + ".java"))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to scan project path " + projectPath + ": "
                    + exception.getMessage(), exception);
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Could not find Java source file "
                    + entrypoint.className() + ".java under " + projectPath);
        }

        List<String> observedPackages = new ArrayList<>();
        for (Path candidate : candidates) {
            String source = readSource(candidate);
            String strippedSource = stripCommentsAndStrings(source);
            String declaredPackage = declaredPackage(strippedSource);
            observedPackages.add(declaredPackage.isBlank() ? "<default>" : declaredPackage);
            if (entrypoint.packageName().equals(declaredPackage)) {
                return new JavaSourceFile(candidate, relativePath(projectPath, candidate), source, declaredPackage);
            }
        }

        throw new IllegalArgumentException("Could not find " + entrypoint.className()
                + ".java declaring package " + entrypoint.packageName()
                + " under " + projectPath
                + ". Observed packages: " + observedPackages);
    }

    private static String readSource(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read Java source file " + path + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static String relativePath(Path projectPath, Path sourceFile) {
        try {
            return projectPath.relativize(sourceFile).toString();
        } catch (IllegalArgumentException exception) {
            return sourceFile.toString();
        }
    }

    private static String declaredPackage(String strippedSource) {
        Matcher matcher = PACKAGE_PATTERN.matcher(strippedSource);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static TypeRegion findTypeRegion(String strippedSource, LineMap lineMap, Entrypoint entrypoint) {
        Pattern typePattern = Pattern.compile("\\b(class|interface|enum|record)\\s+"
                + Pattern.quote(entrypoint.className()) + "\\b");
        Matcher matcher = typePattern.matcher(strippedSource);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find class " + entrypoint.className()
                    + " in source file for " + entrypoint.classQualifiedName());
        }

        int bodyOpen = strippedSource.indexOf('{', matcher.end());
        if (bodyOpen < 0) {
            throw new IllegalArgumentException("Could not find body for class " + entrypoint.classQualifiedName());
        }
        int bodyClose = findMatching(strippedSource, bodyOpen, '{', '}');
        if (bodyClose < 0) {
            throw new IllegalArgumentException("Could not find closing brace for class "
                    + entrypoint.classQualifiedName());
        }

        return new TypeRegion(matcher.start(), bodyOpen + 1, bodyClose, lineMap.lineOf(matcher.start()));
    }

    private static Map<String, JavaMethod> findMethods(
            String strippedSource,
            LineMap lineMap,
            TypeRegion typeRegion,
            String className
    ) {
        Map<String, JavaMethod> methods = new LinkedHashMap<>();
        int index = typeRegion.bodyStart();
        int depth = 1;

        while (index < typeRegion.bodyEnd()) {
            char current = strippedSource.charAt(index);
            if (current == '{') {
                depth++;
                index++;
                continue;
            }
            if (current == '}') {
                depth--;
                index++;
                continue;
            }
            if (depth == 1 && Character.isJavaIdentifierStart(current)) {
                int nameStart = index;
                int nameEnd = readIdentifierEnd(strippedSource, nameStart);
                String methodName = strippedSource.substring(nameStart, nameEnd);
                if (methodName.equals(className) || RESERVED_CALL_WORDS.contains(methodName)) {
                    index = nameEnd;
                    continue;
                }

                int parenOpen = skipWhitespace(strippedSource, nameEnd, typeRegion.bodyEnd());
                if (parenOpen < typeRegion.bodyEnd() && strippedSource.charAt(parenOpen) == '(') {
                    int parenClose = findMatching(strippedSource, parenOpen, '(', ')');
                    if (parenClose > 0 && parenClose < typeRegion.bodyEnd()) {
                        int bodyOpen = findDeclarationBodyOpen(strippedSource, parenClose + 1, typeRegion.bodyEnd());
                        if (bodyOpen > 0) {
                            int bodyClose = findMatching(strippedSource, bodyOpen, '{', '}');
                            if (bodyClose > bodyOpen && bodyClose <= typeRegion.bodyEnd()) {
                                methods.putIfAbsent(methodName, new JavaMethod(
                                        methodName,
                                        nameStart,
                                        bodyOpen + 1,
                                        bodyClose,
                                        lineMap.lineOf(nameStart)
                                ));
                                index = bodyClose + 1;
                                continue;
                            }
                        }
                    }
                }
                index = nameEnd;
                continue;
            }
            index++;
        }

        return methods;
    }

    private static int findDeclarationBodyOpen(String strippedSource, int start, int limit) {
        int index = skipWhitespace(strippedSource, start, limit);
        while (index < limit) {
            char current = strippedSource.charAt(index);
            if (current == '{') {
                return index;
            }
            if (current == ';' || current == '=') {
                return -1;
            }
            index++;
        }
        return -1;
    }

    private static List<CallExpression> findCalls(
            String rawSource,
            String strippedSource,
            LineMap lineMap,
            JavaMethod method
    ) {
        List<CallExpression> calls = new ArrayList<>();
        List<SourceRange> lambdaBlockRanges = lambdaBlockRanges(strippedSource, method);
        int index = method.bodyStart();

        while (index < method.bodyEnd()) {
            SourceRange ignoredRange = containingRange(lambdaBlockRanges, index);
            if (ignoredRange != null) {
                index = ignoredRange.end();
                continue;
            }

            if (!Character.isJavaIdentifierStart(strippedSource.charAt(index))) {
                index++;
                continue;
            }

            int firstStart = index;
            int firstEnd = readIdentifierEnd(strippedSource, firstStart);
            if (isContinuationOfIdentifierOrMemberAccess(strippedSource, method.bodyStart(), firstStart)) {
                index = firstEnd;
                continue;
            }

            String firstName = strippedSource.substring(firstStart, firstEnd);
            if (RESERVED_CALL_WORDS.contains(firstName)) {
                index = firstEnd;
                continue;
            }

            int afterFirst = skipWhitespace(strippedSource, firstEnd, method.bodyEnd());
            if (afterFirst < method.bodyEnd() && strippedSource.charAt(afterFirst) == '.') {
                MemberCallScanResult scanResult = scanMemberCall(rawSource, strippedSource, lineMap, method, firstName, firstStart, afterFirst);
                if (scanResult.call() != null) {
                    calls.add(scanResult.call());
                    index = scanResult.nextIndex();
                    continue;
                }
            } else if (afterFirst < method.bodyEnd() && strippedSource.charAt(afterFirst) == '(') {
                CallExpression call = scanLocalCall(rawSource, strippedSource, lineMap, method, firstName, firstStart, afterFirst);
                if (call != null) {
                    calls.add(call);
                    index = call.endIndex();
                    continue;
                }
            }

            index = firstEnd;
        }

        return calls;
    }

    private static List<SourceRange> lambdaBlockRanges(String strippedSource, JavaMethod method) {
        List<SourceRange> ranges = new ArrayList<>();
        int index = method.bodyStart();
        while (index + 1 < method.bodyEnd()) {
            if (strippedSource.charAt(index) == '-' && strippedSource.charAt(index + 1) == '>') {
                int bodyOpen = skipWhitespace(strippedSource, index + 2, method.bodyEnd());
                if (bodyOpen < method.bodyEnd() && strippedSource.charAt(bodyOpen) == '{') {
                    int bodyClose = findMatching(strippedSource, bodyOpen, '{', '}');
                    if (bodyClose > bodyOpen && bodyClose <= method.bodyEnd()) {
                        ranges.add(new SourceRange(bodyOpen, bodyClose + 1));
                        index = bodyClose + 1;
                        continue;
                    }
                }
            }
            index++;
        }
        return ranges;
    }

    private static SourceRange containingRange(List<SourceRange> ranges, int index) {
        for (SourceRange range : ranges) {
            if (index >= range.start() && index < range.end()) {
                return range;
            }
        }
        return null;
    }

    private static MemberCallScanResult scanMemberCall(
            String rawSource,
            String strippedSource,
            LineMap lineMap,
            JavaMethod method,
            String receiverName,
            int receiverStart,
            int dotIndex
    ) {
        int methodNameStart = skipWhitespace(strippedSource, dotIndex + 1, method.bodyEnd());
        if (methodNameStart >= method.bodyEnd()
                || "super".equals(receiverName)
                || !Character.isJavaIdentifierStart(strippedSource.charAt(methodNameStart))) {
            return new MemberCallScanResult(null, dotIndex + 1);
        }

        int methodNameEnd = readIdentifierEnd(strippedSource, methodNameStart);
        String methodName = strippedSource.substring(methodNameStart, methodNameEnd);
        if (RESERVED_CALL_WORDS.contains(methodName)) {
            return new MemberCallScanResult(null, methodNameEnd);
        }

        int parenOpen = skipWhitespace(strippedSource, methodNameEnd, method.bodyEnd());
        if (parenOpen >= method.bodyEnd() || strippedSource.charAt(parenOpen) != '(') {
            return new MemberCallScanResult(null, methodNameEnd);
        }

        int parenClose = findMatching(strippedSource, parenOpen, '(', ')');
        if (!isAcceptedCall(strippedSource, method, receiverStart, parenClose)) {
            return new MemberCallScanResult(null, methodNameEnd);
        }

        String expression = normalizeExpression(rawSource.substring(receiverStart, parenClose + 1));
        return new MemberCallScanResult(
                new CallExpression(receiverName, methodName, expression, lineMap.lineOf(receiverStart), receiverStart, parenClose + 1),
                parenClose + 1
        );
    }

    private static CallExpression scanLocalCall(
            String rawSource,
            String strippedSource,
            LineMap lineMap,
            JavaMethod method,
            String methodName,
            int methodNameStart,
            int parenOpen
    ) {
        int parenClose = findMatching(strippedSource, parenOpen, '(', ')');
        if (!isAcceptedCall(strippedSource, method, methodNameStart, parenClose)
                || previousWord(strippedSource, method.bodyStart(), methodNameStart).equals("new")) {
            return null;
        }

        String expression = normalizeExpression(rawSource.substring(methodNameStart, parenClose + 1));
        return new CallExpression(null, methodName, expression, lineMap.lineOf(methodNameStart), methodNameStart, parenClose + 1);
    }

    private static boolean isAcceptedCall(String strippedSource, JavaMethod method, int callStart, int parenClose) {
        if (parenClose < 0 || parenClose >= method.bodyEnd()) {
            return false;
        }
        int next = skipWhitespace(strippedSource, parenClose + 1, method.bodyEnd());
        if (next < method.bodyEnd() && strippedSource.charAt(next) == '.') {
            return false;
        }
        return !statementContainsLambda(strippedSource, method, callStart, parenClose + 1);
    }

    private static boolean statementContainsLambda(String strippedSource, JavaMethod method, int callStart, int callEnd) {
        int statementStart = callStart;
        while (statementStart > method.bodyStart()) {
            char current = strippedSource.charAt(statementStart - 1);
            if (current == ';' || current == '{' || current == '}') {
                break;
            }
            statementStart--;
        }

        int statementEnd = callEnd;
        while (statementEnd < method.bodyEnd()) {
            char current = strippedSource.charAt(statementEnd);
            if (current == ';' || current == '{' || current == '}') {
                break;
            }
            statementEnd++;
        }

        return strippedSource.substring(statementStart, statementEnd).contains("->");
    }

    private static ResolvedCall resolveCall(
            JavaSourceFile sourceFile,
            Entrypoint entrypoint,
            Map<String, JavaMethod> methods,
            CallExpression call
    ) {
        JavaMethod localMethod = methods.get(call.methodName());
        if (call.receiverName() == null && localMethod != null) {
            String qualifiedName = entrypoint.classQualifiedName() + "." + call.methodName();
            return new ResolvedCall(
                    "method:" + qualifiedName,
                    qualifiedName,
                    call.methodName(),
                    null,
                    "local",
                    localMethod.line()
            );
        }

        if ("this".equals(call.receiverName())) {
            String qualifiedName = entrypoint.classQualifiedName() + "." + call.methodName();
            String resolution = localMethod == null ? "unresolved" : "local";
            int line = localMethod == null ? call.line() : localMethod.line();
            return new ResolvedCall(
                    "method:" + qualifiedName,
                    qualifiedName,
                    call.methodName(),
                    null,
                    resolution,
                    line
            );
        }

        if (call.receiverName() != null && !"super".equals(call.receiverName())) {
            String qualifiedName = call.receiverName() + "." + call.methodName();
            return new ResolvedCall(
                    "method:" + qualifiedName,
                    qualifiedName,
                    call.methodName(),
                    call.receiverName(),
                    "member-access",
                    call.line()
            );
        }

        String qualifiedName = call.methodName();
        return new ResolvedCall(
                "method:" + qualifiedName,
                qualifiedName,
                call.methodName(),
                call.receiverName(),
                "unresolved",
                call.line()
        );
    }

    private static GraphNode classNode(
            JavaSourceFile sourceFile,
            TypeRegion typeRegion,
            Entrypoint entrypoint,
            String nodeId
    ) {
        Map<String, Object> attributes = baseAttributes(sourceFile, typeRegion.line());
        attributes.put("packageName", entrypoint.packageName());
        attributes.put("className", entrypoint.className());

        return new GraphNode(
                nodeId,
                "CLASS",
                entrypoint.classQualifiedName(),
                entrypoint.className(),
                attributes
        );
    }

    private static GraphNode methodNode(
            JavaSourceFile sourceFile,
            Entrypoint entrypoint,
            String nodeId,
            String qualifiedName,
            String methodName,
            int line,
            String resolution,
            boolean entrypointNode,
            String receiverName
    ) {
        Map<String, Object> attributes = baseAttributes(sourceFile, line);
        attributes.put("methodName", methodName);
        attributes.put("resolution", resolution);
        if (qualifiedName.startsWith(entrypoint.classQualifiedName() + ".")) {
            attributes.put("packageName", entrypoint.packageName());
            attributes.put("className", entrypoint.className());
        }
        if (receiverName != null) {
            attributes.put("receiverName", receiverName);
        }
        if (entrypointNode) {
            attributes.put("entrypoint", true);
        }

        return new GraphNode(nodeId, "METHOD", qualifiedName, methodName, attributes);
    }

    private static Map<String, Object> baseAttributes(JavaSourceFile sourceFile, int line) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("source", "source-text");
        attributes.put("sourceFile", sourceFile.relativePath());
        attributes.put("line", line);
        attributes.put("deterministic", true);
        return attributes;
    }

    private static Map<String, Object> edgeAttributes(int line, String expression) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("line", line);
        if (expression != null) {
            attributes.put("expression", expression);
        }
        attributes.put("source", "source-text");
        attributes.put("deterministic", true);
        return attributes;
    }

    private static boolean isContinuationOfIdentifierOrMemberAccess(String source, int lowerBound, int index) {
        if (index > lowerBound && Character.isJavaIdentifierPart(source.charAt(index - 1))) {
            return true;
        }
        int previous = previousNonWhitespace(source, lowerBound, index);
        return previous >= lowerBound && source.charAt(previous) == '.';
    }

    private static int previousNonWhitespace(String source, int lowerBound, int index) {
        int current = index - 1;
        while (current >= lowerBound && Character.isWhitespace(source.charAt(current))) {
            current--;
        }
        return current;
    }

    private static String previousWord(String source, int lowerBound, int index) {
        int current = previousNonWhitespace(source, lowerBound, index);
        if (current < lowerBound || !Character.isJavaIdentifierPart(source.charAt(current))) {
            return "";
        }
        int end = current + 1;
        while (current >= lowerBound && Character.isJavaIdentifierPart(source.charAt(current))) {
            current--;
        }
        return source.substring(current + 1, end);
    }

    private static String normalizeExpression(String expression) {
        return expression.trim().replaceAll("\\s+", " ");
    }

    private static int skipWhitespace(String source, int start, int limit) {
        int index = start;
        while (index < limit && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int readIdentifierEnd(String source, int start) {
        int index = start + 1;
        while (index < source.length() && Character.isJavaIdentifierPart(source.charAt(index))) {
            index++;
        }
        return index;
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

    private static String stripCommentsAndStrings(String source) {
        StringBuilder stripped = new StringBuilder(source.length());
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';

            if (current == '/' && next == '/') {
                stripped.append(' ');
                stripped.append(' ');
                index++;
                while (index + 1 < source.length() && source.charAt(index + 1) != '\n') {
                    stripped.append(' ');
                    index++;
                }
            } else if (current == '/' && next == '*') {
                stripped.append(' ');
                stripped.append(' ');
                index++;
                while (index + 1 < source.length()) {
                    index++;
                    char blockCurrent = source.charAt(index);
                    char blockNext = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
                    stripped.append(blockCurrent == '\n' ? '\n' : ' ');
                    if (blockCurrent == '*' && blockNext == '/') {
                        stripped.append(' ');
                        index++;
                        break;
                    }
                }
            } else if (current == '"') {
                stripped.append(' ');
                index = stripQuotedLiteral(source, stripped, index + 1, '"');
            } else if (current == '\'') {
                stripped.append(' ');
                index = stripQuotedLiteral(source, stripped, index + 1, '\'');
            } else {
                stripped.append(current);
            }
        }
        return stripped.toString();
    }

    private static int stripQuotedLiteral(String source, StringBuilder stripped, int start, char quote) {
        int index = start;
        while (index < source.length()) {
            char current = source.charAt(index);
            stripped.append(current == '\n' ? '\n' : ' ');
            if (current == '\\' && index + 1 < source.length()) {
                index++;
                char escaped = source.charAt(index);
                stripped.append(escaped == '\n' ? '\n' : ' ');
            } else if (current == quote) {
                break;
            }
            index++;
        }
        return index;
    }

    private record Entrypoint(String packageName, String className, String methodName) {
        private static Entrypoint parse(String entrypoint) {
            if (entrypoint == null || entrypoint.isBlank() || entrypoint.contains(" ")) {
                throw new IllegalArgumentException("entrypoint must use format <package>.<Class>.<method>");
            }

            String normalized = entrypoint.trim();
            int methodSeparator = normalized.lastIndexOf('.');
            if (methodSeparator <= 0 || methodSeparator == normalized.length() - 1) {
                throw new IllegalArgumentException("entrypoint must use format <package>.<Class>.<method>");
            }

            String classQualifiedName = normalized.substring(0, methodSeparator);
            String methodName = normalized.substring(methodSeparator + 1);
            int classSeparator = classQualifiedName.lastIndexOf('.');
            if (classSeparator <= 0 || classSeparator == classQualifiedName.length() - 1) {
                throw new IllegalArgumentException("entrypoint must use format <package>.<Class>.<method>");
            }

            return new Entrypoint(
                    classQualifiedName.substring(0, classSeparator),
                    classQualifiedName.substring(classSeparator + 1),
                    methodName
            );
        }

        private String normalized() {
            return classQualifiedName() + "." + methodName;
        }

        private String classQualifiedName() {
            return packageName + "." + className;
        }
    }

    private record JavaSourceFile(Path path, String relativePath, String source, String declaredPackage) {
    }

    private record TypeRegion(int typeStart, int bodyStart, int bodyEnd, int line) {
    }

    private record JavaMethod(String name, int nameStart, int bodyStart, int bodyEnd, int line) {
    }

    private record CallExpression(
            String receiverName,
            String methodName,
            String expression,
            int line,
            int startIndex,
            int endIndex
    ) {
    }

    private record MemberCallScanResult(CallExpression call, int nextIndex) {
    }

    private record SourceRange(int start, int end) {
    }

    private record ResolvedCall(
            String nodeId,
            String qualifiedName,
            String methodName,
            String receiverName,
            String resolution,
            int line
    ) {
    }

    private record LineMap(int[] lineStarts) {
        private static LineMap from(String source) {
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            for (int index = 0; index < source.length(); index++) {
                if (source.charAt(index) == '\n' && index + 1 < source.length()) {
                    starts.add(index + 1);
                }
            }
            int[] lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
            return new LineMap(lineStarts);
        }

        private int lineOf(int index) {
            int result = Arrays.binarySearch(lineStarts, index);
            if (result >= 0) {
                return result + 1;
            }
            return -result - 1;
        }
    }
}
