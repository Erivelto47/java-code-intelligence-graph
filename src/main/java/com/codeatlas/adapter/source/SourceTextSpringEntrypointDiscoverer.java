package com.codeatlas.adapter.source;

import com.codeatlas.core.entrypoint.EntrypointAnnotations;
import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.EntrypointDiscoverer;
import com.codeatlas.core.entrypoint.EntrypointIndex;
import com.codeatlas.core.entrypoint.EntrypointKind;
import com.codeatlas.core.entrypoint.SourceLocation;

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

public final class SourceTextSpringEntrypointDiscoverer implements EntrypointDiscoverer {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "\\bpackage\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\s*;"
    );
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of("RestController", "Controller");
    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "RequestMapping",
            "GetMapping",
            "PostMapping",
            "PutMapping",
            "PatchMapping",
            "DeleteMapping"
    );
    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    @Override
    public EntrypointIndex discover(Path projectPath) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedProjectPath)) {
            throw new IllegalArgumentException("Project path must be an existing directory: " + normalizedProjectPath);
        }

        List<Path> sourceFiles = javaSourceFiles(normalizedProjectPath);
        List<EntrypointDescriptor> descriptors = new ArrayList<>();
        for (Path sourceFile : sourceFiles) {
            descriptors.addAll(discoverInSourceFile(normalizedProjectPath, sourceFile));
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analyzer", "source-text-spring-entrypoint-discoverer");
        metadata.put("phase", "phase-2-spring-entrypoints");
        metadata.put("deterministic", true);
        metadata.put("source", "source-text");
        metadata.put("indexedJavaSourceFiles", sourceFiles.size());
        metadata.put("requestMappingWithoutMethod", "ANY");

        return new EntrypointIndex(
                SCHEMA_VERSION,
                normalizedProjectPath.toString().replace('\\', '/'),
                DETERMINISTIC_GENERATED_AT,
                List.copyOf(descriptors),
                metadata
        );
    }

    private static List<EntrypointDescriptor> discoverInSourceFile(Path projectPath, Path sourcePath) {
        String source = readSource(sourcePath);
        String commentStrippedSource = stripCommentsKeepStrings(source);
        String structureSource = stripStrings(commentStrippedSource);
        LineMap lineMap = LineMap.from(source);
        String packageName = declaredPackage(structureSource);
        String relativePath = relativePath(projectPath, sourcePath);
        List<EntrypointDescriptor> descriptors = new ArrayList<>();

        for (TypeRegion typeRegion : findTypeRegions(structureSource, lineMap)) {
            int typeDeclarationStart = declarationStart(structureSource, typeRegion.typeStart());
            List<AnnotationUse> classAnnotations = leadingAnnotations(
                    commentStrippedSource,
                    lineMap,
                    typeDeclarationStart,
                    typeRegion.typeStart()
            );
            if (!isController(classAnnotations)) {
                continue;
            }

            String className = packageName.isBlank()
                    ? typeRegion.name()
                    : packageName + "." + typeRegion.name();
            List<String> classPaths = classAnnotations.stream()
                    .filter(annotation -> annotation.name().equals("RequestMapping"))
                    .findFirst()
                    .map(SourceTextSpringEntrypointDiscoverer::paths)
                    .orElse(List.of(""));
            List<String> classAnnotationSources = classAnnotations.stream()
                    .map(AnnotationUse::source)
                    .toList();

            for (JavaMethod method : findMethods(structureSource, commentStrippedSource, lineMap, typeRegion)) {
                List<AnnotationUse> methodMappings = method.annotations().stream()
                        .filter(annotation -> MAPPING_ANNOTATIONS.contains(annotation.name()))
                        .toList();
                for (AnnotationUse mapping : methodMappings) {
                    for (String classPath : classPaths) {
                        for (String methodPath : paths(mapping)) {
                            String normalizedPath = normalizePath(classPath, methodPath);
                            for (String httpMethod : httpMethods(mapping)) {
                                String javaEntrypoint = className + "." + method.name();
                                String id = "http:" + httpMethod + ":" + normalizedPath + " -> " + javaEntrypoint;
                                descriptors.add(new EntrypointDescriptor(
                                        id,
                                        EntrypointKind.HTTP_ENDPOINT,
                                        httpMethod,
                                        normalizedPath,
                                        className,
                                        method.name(),
                                        javaEntrypoint,
                                        new EntrypointAnnotations(
                                                List.copyOf(classAnnotationSources),
                                                method.annotations().stream().map(AnnotationUse::source).toList()
                                        ),
                                        new SourceLocation(relativePath, method.line())
                                ));
                            }
                        }
                    }
                }
            }
        }

        return descriptors;
    }

    private static boolean isController(List<AnnotationUse> annotations) {
        return annotations.stream().anyMatch(annotation -> CONTROLLER_ANNOTATIONS.contains(annotation.name()));
    }

    private static List<String> paths(AnnotationUse annotation) {
        String arguments = annotation.arguments();
        if (arguments == null || arguments.isBlank()) {
            return List.of("");
        }

        List<String> parts = splitTopLevel(arguments, ',');
        List<String> paths = new ArrayList<>();
        for (String part : parts) {
            Attribute attribute = parseAttribute(part);
            if (attribute != null && (attribute.name().equals("value") || attribute.name().equals("path"))) {
                paths.addAll(stringLiterals(attribute.value()));
            }
        }
        if (paths.isEmpty() && !parts.isEmpty() && parseAttribute(parts.get(0)) == null) {
            paths.addAll(stringLiterals(parts.get(0)));
        }
        if (paths.isEmpty()) {
            paths.add("");
        }
        return paths.stream().distinct().toList();
    }

    private static List<String> httpMethods(AnnotationUse annotation) {
        return switch (annotation.name()) {
            case "GetMapping" -> List.of("GET");
            case "PostMapping" -> List.of("POST");
            case "PutMapping" -> List.of("PUT");
            case "PatchMapping" -> List.of("PATCH");
            case "DeleteMapping" -> List.of("DELETE");
            case "RequestMapping" -> requestMappingHttpMethods(annotation.arguments());
            default -> List.of("ANY");
        };
    }

    private static List<String> requestMappingHttpMethods(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return List.of("ANY");
        }
        for (String part : splitTopLevel(arguments, ',')) {
            Attribute attribute = parseAttribute(part);
            if (attribute != null && attribute.name().equals("method")) {
                List<String> methods = enumConstants(attribute.value()).stream()
                        .filter(HTTP_METHODS::contains)
                        .distinct()
                        .toList();
                return methods.isEmpty() ? List.of("ANY") : methods;
            }
        }
        return List.of("ANY");
    }

    private static List<String> enumConstants(String value) {
        List<String> constants = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?:RequestMethod\\s*\\.\\s*)?([A-Z][A-Z0-9_]*)").matcher(value);
        while (matcher.find()) {
            constants.add(matcher.group(1));
        }
        return constants;
    }

    private static List<String> stringLiterals(String value) {
        List<String> literals = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(value);
        while (matcher.find()) {
            literals.add(unescapeJavaString(matcher.group(1)));
        }
        return literals;
    }

    private static String unescapeJavaString(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static Attribute parseAttribute(String part) {
        int equals = topLevelEquals(part);
        if (equals < 0) {
            return null;
        }
        String name = part.substring(0, equals).trim();
        String value = part.substring(equals + 1).trim();
        if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            return null;
        }
        return new Attribute(name, value);
    }

    private static int topLevelEquals(String value) {
        int braceDepth = 0;
        int parenDepth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth = Math.max(0, parenDepth - 1);
            } else if (current == '=' && braceDepth == 0 && parenDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static String normalizePath(String classPath, String methodPath) {
        String left = cleanPathSegment(classPath);
        String right = cleanPathSegment(methodPath);
        String combined;
        if (left.isBlank()) {
            combined = right;
        } else if (right.isBlank()) {
            combined = left;
        } else {
            combined = left + "/" + right;
        }
        combined = combined.replaceAll("/{2,}", "/");
        if (combined.isBlank() || combined.equals("/")) {
            return "/";
        }
        if (!combined.startsWith("/")) {
            combined = "/" + combined;
        }
        while (combined.length() > 1 && combined.endsWith("/")) {
            combined = combined.substring(0, combined.length() - 1);
        }
        return combined;
    }

    private static String cleanPathSegment(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private static List<JavaMethod> findMethods(
            String structureSource,
            String annotationSource,
            LineMap lineMap,
            TypeRegion typeRegion
    ) {
        List<JavaMethod> methods = new ArrayList<>();
        int index = typeRegion.bodyStart();
        int depth = 1;
        int memberStart = typeRegion.bodyStart();

        while (index < typeRegion.bodyEnd()) {
            char current = structureSource.charAt(index);
            if (current == '{') {
                depth++;
                index++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 1) {
                    memberStart = index + 1;
                }
                index++;
                continue;
            }
            if (current == ';' && depth == 1) {
                memberStart = index + 1;
                index++;
                continue;
            }
            if (depth == 1 && Character.isJavaIdentifierStart(current)) {
                int nameStart = index;
                int nameEnd = readIdentifierEnd(structureSource, nameStart);
                if (isAnnotationName(structureSource, typeRegion.bodyStart(), nameStart)
                        || isContinuationOfIdentifierOrMemberAccess(structureSource, typeRegion.bodyStart(), nameStart)) {
                    index = nameEnd;
                    continue;
                }
                String methodName = structureSource.substring(nameStart, nameEnd);
                if (methodName.equals(typeRegion.name()) || isReservedWord(methodName)) {
                    index = nameEnd;
                    continue;
                }
                int parenOpen = skipWhitespace(structureSource, nameEnd, typeRegion.bodyEnd());
                if (parenOpen < typeRegion.bodyEnd() && structureSource.charAt(parenOpen) == '(') {
                    int parenClose = findMatching(structureSource, parenOpen, '(', ')');
                    if (parenClose > 0 && parenClose < typeRegion.bodyEnd()) {
                        DeclarationEnd declarationEnd = findDeclarationEnd(structureSource, parenClose + 1, typeRegion.bodyEnd());
                        if (declarationEnd != null) {
                            List<AnnotationUse> annotations = leadingAnnotations(
                                    annotationSource,
                                    lineMap,
                                    memberStart,
                                    nameStart
                            );
                            methods.add(new JavaMethod(methodName, lineMap.lineOf(nameStart), annotations));
                            index = declarationEnd.nextIndex();
                            memberStart = declarationEnd.nextIndex();
                            continue;
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

    private static DeclarationEnd findDeclarationEnd(String source, int start, int limit) {
        int index = skipWhitespace(source, start, limit);
        while (index < limit) {
            char current = source.charAt(index);
            if (current == '{') {
                int bodyClose = findMatching(source, index, '{', '}');
                if (bodyClose > index && bodyClose <= limit) {
                    return new DeclarationEnd(bodyClose + 1);
                }
                return null;
            }
            if (current == ';') {
                return new DeclarationEnd(index + 1);
            }
            if (current == '=') {
                return null;
            }
            index++;
        }
        return null;
    }

    private static boolean isReservedWord(String value) {
        return switch (value) {
            case "if", "for", "while", "switch", "catch", "return", "throw", "new", "class", "interface", "enum", "record" -> true;
            default -> false;
        };
    }

    private static List<AnnotationUse> leadingAnnotations(
            String source,
            LineMap lineMap,
            int start,
            int limit
    ) {
        List<AnnotationUse> annotations = new ArrayList<>();
        int index = skipWhitespace(source, start, limit);
        while (index < limit && source.charAt(index) == '@') {
            AnnotationUse annotation = parseAnnotationAt(source, lineMap, index, limit);
            if (annotation == null) {
                break;
            }
            annotations.add(annotation);
            index = skipWhitespace(source, annotation.endIndex(), limit);
        }
        return annotations;
    }

    private static AnnotationUse parseAnnotationAt(String source, LineMap lineMap, int atIndex, int limit) {
        int nameStart = atIndex + 1;
        if (nameStart >= limit || !Character.isJavaIdentifierStart(source.charAt(nameStart))) {
            return null;
        }
        int nameEnd = nameStart + 1;
        while (nameEnd < limit
                && (Character.isJavaIdentifierPart(source.charAt(nameEnd)) || source.charAt(nameEnd) == '.')) {
            nameEnd++;
        }
        int annotationEnd = nameEnd;
        String arguments = null;
        int afterName = skipWhitespace(source, nameEnd, limit);
        if (afterName < limit && source.charAt(afterName) == '(') {
            int parenClose = findMatching(source, afterName, '(', ')');
            if (parenClose < 0 || parenClose >= limit) {
                return null;
            }
            arguments = source.substring(afterName + 1, parenClose);
            annotationEnd = parenClose + 1;
        }
        String qualifiedName = source.substring(nameStart, nameEnd);
        String text = normalizeAnnotationSource(source.substring(atIndex, annotationEnd));
        return new AnnotationUse(simpleName(qualifiedName), text, arguments, lineMap.lineOf(atIndex), annotationEnd);
    }

    private static String normalizeAnnotationSource(String source) {
        return source.trim().replaceAll("\\s+", " ");
    }

    private static int declarationStart(String source, int keywordStart) {
        int index = keywordStart - 1;
        while (index >= 0) {
            char current = source.charAt(index);
            if (current == ';' || current == '}' || current == '{') {
                return index + 1;
            }
            index--;
        }
        return 0;
    }

    private static List<TypeRegion> findTypeRegions(String structureSource, LineMap lineMap) {
        List<TypeRegion> regions = new ArrayList<>();
        Matcher matcher = TYPE_PATTERN.matcher(structureSource);
        while (matcher.find()) {
            if (isAnnotationName(structureSource, 0, matcher.start())) {
                continue;
            }
            int bodyOpen = structureSource.indexOf('{', matcher.end());
            if (bodyOpen < 0) {
                continue;
            }
            int bodyClose = findMatching(structureSource, bodyOpen, '{', '}');
            if (bodyClose < 0) {
                continue;
            }
            regions.add(new TypeRegion(
                    matcher.group(2),
                    matcher.start(),
                    bodyOpen + 1,
                    bodyClose,
                    lineMap.lineOf(matcher.start())
            ));
            matcher.region(bodyClose + 1, structureSource.length());
        }
        return regions;
    }

    private static String declaredPackage(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static List<Path> javaSourceFiles(Path projectPath) {
        Path mainJava = projectPath.resolve("src/main/java");
        List<Path> roots = Files.isDirectory(mainJava) ? List.of(mainJava) : List.of(projectPath);
        List<Path> sourceFiles = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .filter(path -> !isIgnoredPath(projectPath, path))
                        .sorted()
                        .forEach(sourceFiles::add);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Failed to scan project path " + projectPath + ": "
                        + exception.getMessage(), exception);
            }
        }
        return sourceFiles.stream().sorted().toList();
    }

    private static boolean isIgnoredPath(Path projectPath, Path path) {
        Path relative = projectPath.relativize(path.toAbsolutePath().normalize());
        for (Path segment : relative) {
            String name = segment.toString();
            if (name.equals(".git")
                    || name.equals(".gradle")
                    || name.equals(".code-atlas")
                    || name.equals("build")
                    || name.equals("target")) {
                return true;
            }
        }
        return false;
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
            return projectPath.relativize(sourceFile.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException exception) {
            return sourceFile.toString().replace('\\', '/');
        }
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int braceDepth = 0;
        int parenDepth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth = Math.max(0, parenDepth - 1);
            } else if (current == delimiter && braceDepth == 0 && parenDepth == 0) {
                parts.add(value.substring(start, index).trim());
                start = index + 1;
            }
        }
        String last = value.substring(start).trim();
        if (!last.isBlank()) {
            parts.add(last);
        }
        return parts;
    }

    private static String stripCommentsKeepStrings(String source) {
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
            } else {
                stripped.append(current);
            }
        }
        return stripped.toString();
    }

    private static String stripStrings(String source) {
        StringBuilder stripped = new StringBuilder(source.length());
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '"') {
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

    private static boolean isContinuationOfIdentifierOrMemberAccess(String source, int lowerBound, int index) {
        if (index > lowerBound && Character.isJavaIdentifierPart(source.charAt(index - 1))) {
            return true;
        }
        int previous = previousNonWhitespace(source, lowerBound, index);
        return previous >= lowerBound && source.charAt(previous) == '.';
    }

    private static boolean isAnnotationName(String source, int lowerBound, int index) {
        int previous = previousNonWhitespace(source, lowerBound, index);
        return previous >= lowerBound && source.charAt(previous) == '@';
    }

    private static int previousNonWhitespace(String source, int lowerBound, int index) {
        int current = index - 1;
        while (current >= lowerBound && Character.isWhitespace(source.charAt(current))) {
            current--;
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
            if (current == '"' || current == '\'') {
                index = skipQuotedLiteral(source, index + 1, current);
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

    private static int skipQuotedLiteral(String source, int start, char quote) {
        int index = start;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\\' && index + 1 < source.length()) {
                index += 2;
                continue;
            }
            if (current == quote) {
                return index;
            }
            index++;
        }
        return index;
    }

    private static String simpleName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
    }

    private record TypeRegion(String name, int typeStart, int bodyStart, int bodyEnd, int line) {
    }

    private record JavaMethod(String name, int line, List<AnnotationUse> annotations) {
    }

    private record AnnotationUse(String name, String source, String arguments, int line, int endIndex) {
    }

    private record Attribute(String name, String value) {
    }

    private record DeclarationEnd(int nextIndex) {
    }

    private record LineMap(int[] lineStarts, int sourceLength) {
        private static LineMap from(String source) {
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            for (int index = 0; index < source.length(); index++) {
                if (source.charAt(index) == '\n' && index + 1 < source.length()) {
                    starts.add(index + 1);
                }
            }
            int[] lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
            return new LineMap(lineStarts, source.length());
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
