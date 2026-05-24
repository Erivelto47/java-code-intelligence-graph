package com.codeatlas.adapter.source;

import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.SourceLocation;
import com.codeatlas.core.project.ImplementationDescriptor;
import com.codeatlas.core.project.JavaMethodDescriptor;
import com.codeatlas.core.project.JavaTypeDescriptor;
import com.codeatlas.core.project.ProjectDescriptor;
import com.codeatlas.core.project.ProjectIndex;
import com.codeatlas.core.project.ProjectIndexer;
import com.codeatlas.core.project.SpringBeanDescriptor;

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

public final class SourceTextProjectIndexer implements ProjectIndexer {
    private static final String SCHEMA_VERSION = "1.0";
    private static final Instant DETERMINISTIC_GENERATED_AT = Instant.EPOCH;
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "\\bpackage\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\s*;"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "\\bimport\\s+(static\\s+)?([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*(?:\\.\\*)?)\\s*;"
    );
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );
    private static final Set<String> TYPE_MODIFIERS = Set.of(
            "abstract",
            "default",
            "final",
            "native",
            "private",
            "protected",
            "public",
            "static",
            "strictfp",
            "synchronized",
            "transient",
            "volatile"
    );
    private static final Set<String> RESERVED_WORDS = Set.of(
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
            "throw",
            "try",
            "while"
    );
    private static final Set<String> SPRING_BEAN_ANNOTATIONS = Set.of(
            "Component",
            "Service",
            "Repository",
            "Controller",
            "RestController",
            "Configuration"
    );
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of("Controller", "RestController");
    private static final Set<String> CLIENT_ANNOTATIONS = Set.of("FeignClient");

    @Override
    public ProjectIndex index(Path projectPath) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedProjectPath)) {
            throw new IllegalArgumentException("Project path must be an existing directory: " + normalizedProjectPath);
        }

        ParsedProject parsedProject = ParsedProject.build(normalizedProjectPath);
        List<EntrypointDescriptor> entrypoints = new SourceTextSpringEntrypointDiscoverer()
                .discover(normalizedProjectPath)
                .entrypoints();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analyzer", "source-text-project-indexer");
        metadata.put("phase", "phase-3-project-index");
        metadata.put("deterministic", true);
        metadata.put("source", "source-text");
        metadata.put("indexedJavaSourceFiles", parsedProject.sourceFileCount());
        metadata.put("indexedTypes", parsedProject.types().size());

        return new ProjectIndex(
                SCHEMA_VERSION,
                DETERMINISTIC_GENERATED_AT,
                new ProjectDescriptor(portablePath(normalizedProjectPath)),
                "Java",
                frameworks(parsedProject, entrypoints),
                parsedProject.sourceRoots(),
                parsedProject.classes(),
                parsedProject.interfaces(),
                parsedProject.implementations(),
                parsedProject.springBeans(),
                parsedProject.controllers(),
                parsedProject.repositories(),
                parsedProject.clients(),
                entrypoints,
                List.of(),
                metadata
        );
    }

    private static List<String> frameworks(ParsedProject parsedProject, List<EntrypointDescriptor> entrypoints) {
        boolean hasSpring = !entrypoints.isEmpty()
                || parsedProject.types().stream().anyMatch(type ->
                type.annotations.stream().anyMatch(SPRING_BEAN_ANNOTATIONS::contains));
        return hasSpring ? List.of("Spring") : List.of();
    }

    private static final class ParsedProject {
        private final Path projectPath;
        private final List<String> sourceRoots;
        private final List<ParsedType> types = new ArrayList<>();
        private final Map<String, ParsedType> typesByQualifiedName = new LinkedHashMap<>();
        private final Map<String, List<ParsedType>> typesBySimpleName = new LinkedHashMap<>();
        private final Map<String, List<String>> implementationsByInterface = new LinkedHashMap<>();
        private int sourceFileCount;

        private ParsedProject(Path projectPath, List<String> sourceRoots) {
            this.projectPath = projectPath;
            this.sourceRoots = sourceRoots;
        }

        private static ParsedProject build(Path projectPath) {
            List<String> sourceRoots = SourceTextProjectIndexer.sourceRoots(projectPath);
            ParsedProject parsedProject = new ParsedProject(projectPath, sourceRoots);
            List<Path> sourceFiles = javaSourceFiles(projectPath);
            parsedProject.sourceFileCount = sourceFiles.size();
            for (Path sourceFile : sourceFiles) {
                parsedProject.indexSourceFile(sourceFile);
            }
            parsedProject.resolveImplementedInterfaces();
            parsedProject.indexImplementations();
            return parsedProject;
        }

        private void indexSourceFile(Path sourcePath) {
            String source = readSource(sourcePath);
            String commentStrippedSource = stripCommentsKeepStrings(source);
            String structureSource = stripStrings(commentStrippedSource);
            LineMap lineMap = LineMap.from(source);
            String packageName = declaredPackage(structureSource);
            SourceFile sourceFile = new SourceFile(
                    sourcePath,
                    relativePath(projectPath, sourcePath),
                    packageName,
                    importsBySimpleName(structureSource),
                    wildcardImports(structureSource)
            );

            for (TypeRegion region : findTypeRegions(structureSource, lineMap)) {
                int declarationStart = declarationStart(structureSource, region.typeStart());
                List<String> annotations = leadingAnnotations(
                        commentStrippedSource,
                        lineMap,
                        declarationStart,
                        region.typeStart()
                ).stream().map(AnnotationUse::name).distinct().toList();

                String qualifiedName = packageName.isBlank()
                        ? region.name()
                        : packageName + "." + region.name();
                ParsedType parsedType = new ParsedType(
                        sourceFile,
                        region.kind().toUpperCase(),
                        region.name(),
                        qualifiedName,
                        region.line(),
                        annotations,
                        isAbstract(region.header())
                );
                parsedType.rawImplementedTypes.addAll(parseHeaderTypes(region.header(), "implements"));
                parsedType.rawExtendedTypes.addAll(parseHeaderTypes(region.header(), "extends"));
                parsedType.methods.addAll(findMethods(
                        structureSource,
                        commentStrippedSource,
                        lineMap,
                        region,
                        sourceFile.relativePath()
                ));
                types.add(parsedType);
                typesByQualifiedName.putIfAbsent(parsedType.qualifiedName, parsedType);
                typesBySimpleName.computeIfAbsent(parsedType.simpleName, ignored -> new ArrayList<>()).add(parsedType);
            }
        }

        private void resolveImplementedInterfaces() {
            for (ParsedType type : types) {
                for (String rawImplementedType : type.rawImplementedTypes) {
                    String resolved = resolveTypeName(rawImplementedType, type.sourceFile);
                    ParsedType target = typesByQualifiedName.get(resolved);
                    if (target != null && "INTERFACE".equals(target.kind)) {
                        type.implementedInterfaces.add(resolved);
                    }
                }
            }
        }

        private void indexImplementations() {
            for (ParsedType type : types) {
                if (!type.isConcrete()) {
                    continue;
                }
                for (String interfaceName : type.implementedInterfaces) {
                    implementationsByInterface
                            .computeIfAbsent(interfaceName, ignored -> new ArrayList<>())
                            .add(type.qualifiedName);
                }
            }
            for (List<String> implementations : implementationsByInterface.values()) {
                implementations.sort(String::compareTo);
            }
        }

        private String resolveTypeName(String rawTypeName, SourceFile context) {
            String cleaned = cleanTypeName(rawTypeName);
            if (cleaned.isBlank()) {
                return rawTypeName;
            }
            if (cleaned.contains(".")) {
                return cleaned;
            }
            String imported = context.importsBySimpleName().get(cleaned);
            if (imported != null) {
                return imported;
            }
            String samePackage = context.packageName().isBlank() ? cleaned : context.packageName() + "." + cleaned;
            if (typesByQualifiedName.containsKey(samePackage)) {
                return samePackage;
            }
            List<ParsedType> candidates = typesBySimpleName.get(cleaned);
            if (candidates != null && candidates.size() == 1) {
                return candidates.get(0).qualifiedName;
            }
            for (String wildcardImport : context.wildcardImports()) {
                String wildcardCandidate = wildcardImport + "." + cleaned;
                if (typesByQualifiedName.containsKey(wildcardCandidate)) {
                    return wildcardCandidate;
                }
            }
            return cleaned;
        }

        private List<String> sourceRoots() {
            return sourceRoots;
        }

        private int sourceFileCount() {
            return sourceFileCount;
        }

        private List<ParsedType> types() {
            return List.copyOf(types);
        }

        private List<JavaTypeDescriptor> classes() {
            return types.stream()
                    .filter(type -> !"INTERFACE".equals(type.kind))
                    .map(ParsedType::descriptor)
                    .toList();
        }

        private List<JavaTypeDescriptor> interfaces() {
            return types.stream()
                    .filter(type -> "INTERFACE".equals(type.kind))
                    .map(ParsedType::descriptor)
                    .toList();
        }

        private List<ImplementationDescriptor> implementations() {
            List<ImplementationDescriptor> implementations = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : implementationsByInterface.entrySet()) {
                implementations.add(new ImplementationDescriptor(entry.getKey(), List.copyOf(entry.getValue())));
            }
            implementations.sort((left, right) -> left.interfaceName().compareTo(right.interfaceName()));
            return List.copyOf(implementations);
        }

        private List<SpringBeanDescriptor> springBeans() {
            return types.stream()
                    .filter(type -> type.annotations.stream().anyMatch(SPRING_BEAN_ANNOTATIONS::contains))
                    .map(type -> new SpringBeanDescriptor(
                            beanId(type.simpleName),
                            springBeanKind(type.annotations),
                            type.qualifiedName,
                            List.copyOf(type.annotations),
                            type.sourceFile.relativePath(),
                            type.sourceLocation()
                    ))
                    .toList();
        }

        private List<String> controllers() {
            return types.stream()
                    .filter(type -> type.annotations.stream().anyMatch(CONTROLLER_ANNOTATIONS::contains))
                    .map(type -> type.qualifiedName)
                    .toList();
        }

        private List<String> repositories() {
            return types.stream()
                    .filter(ParsedProject::isRepository)
                    .map(type -> type.qualifiedName)
                    .toList();
        }

        private List<String> clients() {
            return types.stream()
                    .filter(type -> type.annotations.stream().anyMatch(CLIENT_ANNOTATIONS::contains)
                            || type.simpleName.endsWith("Client"))
                    .map(type -> type.qualifiedName)
                    .toList();
        }

        private static boolean isRepository(ParsedType type) {
            return type.annotations.contains("Repository")
                    || type.simpleName.endsWith("Repository")
                    || type.rawExtendedTypes.stream().map(SourceTextProjectIndexer::cleanTypeName)
                    .anyMatch(value -> value.endsWith("Repository")
                            || value.equals("JpaRepository")
                            || value.equals("CrudRepository"));
        }
    }

    private static List<String> sourceRoots(Path projectPath) {
        Path mainJava = projectPath.resolve("src/main/java");
        if (Files.isDirectory(mainJava)) {
            return List.of("src/main/java");
        }
        return List.of();
    }

    private static List<Path> javaSourceFiles(Path projectPath) {
        Path mainJava = projectPath.resolve("src/main/java");
        if (!Files.isDirectory(mainJava)) {
            return List.of();
        }

        List<Path> sourceFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(mainJava)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .forEach(sourceFiles::add);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to scan project path " + projectPath + ": "
                    + exception.getMessage(), exception);
        }
        return sourceFiles.stream().sorted().toList();
    }

    private static List<JavaMethodDescriptor> findMethods(
            String structureSource,
            String annotationSource,
            LineMap lineMap,
            TypeRegion typeRegion,
            String relativePath
    ) {
        List<JavaMethodDescriptor> methods = new ArrayList<>();
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
                if (methodName.equals(typeRegion.name()) || RESERVED_WORDS.contains(methodName)) {
                    index = nameEnd;
                    continue;
                }

                int parenOpen = skipWhitespace(structureSource, nameEnd, typeRegion.bodyEnd());
                if (parenOpen < typeRegion.bodyEnd() && structureSource.charAt(parenOpen) == '(') {
                    int parenClose = findMatching(structureSource, parenOpen, '(', ')');
                    if (parenClose > 0 && parenClose < typeRegion.bodyEnd()) {
                        DeclarationEnd declarationEnd = findDeclarationEnd(structureSource, parenClose + 1, typeRegion.bodyEnd());
                        if (declarationEnd != null) {
                            List<String> annotations = leadingAnnotations(
                                    annotationSource,
                                    lineMap,
                                    memberStart,
                                    nameStart
                            ).stream().map(AnnotationUse::name).distinct().toList();
                            String declarationPrefix = structureSource.substring(memberStart, nameStart);
                            String returnType = returnType(declarationPrefix);
                            List<String> parameterTypes = parameterTypes(
                                    structureSource.substring(parenOpen + 1, parenClose)
                            );
                            int line = lineMap.lineOf(nameStart);
                            methods.add(new JavaMethodDescriptor(
                                    methodName,
                                    methodName + "(" + String.join(", ", parameterTypes) + ")",
                                    visibility(declarationPrefix),
                                    returnType,
                                    annotations,
                                    new SourceLocation(relativePath, line)
                            ));
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

        return List.copyOf(methods);
    }

    private static String returnType(String declarationPrefix) {
        List<String> tokens = declarationTokens(declarationPrefix);
        List<String> typeTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!TYPE_MODIFIERS.contains(token)) {
                typeTokens.add(token);
            }
        }
        if (!typeTokens.isEmpty() && typeTokens.get(0).startsWith("<")) {
            typeTokens.remove(0);
        }
        if (typeTokens.isEmpty()) {
            return "void";
        }
        return String.join(" ", typeTokens);
    }

    private static String visibility(String declarationPrefix) {
        List<String> tokens = declarationTokens(declarationPrefix);
        if (tokens.contains("public")) {
            return "PUBLIC";
        }
        if (tokens.contains("protected")) {
            return "PROTECTED";
        }
        if (tokens.contains("private")) {
            return "PRIVATE";
        }
        return "PACKAGE_PRIVATE";
    }

    private static List<String> declarationTokens(String declarationPrefix) {
        String cleaned = stripAnnotations(declarationPrefix)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        return tokens(cleaned);
    }

    private static List<String> parameterTypes(String parameterList) {
        List<String> parameterTypes = new ArrayList<>();
        for (String parameter : splitTopLevel(parameterList, ',')) {
            String cleaned = stripAnnotations(parameter)
                    .replace("final ", "")
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .trim();
            List<String> tokens = tokens(cleaned);
            if (tokens.size() < 2) {
                continue;
            }
            parameterTypes.add(cleanTypeName(String.join(" ", tokens.subList(0, tokens.size() - 1))));
        }
        return List.copyOf(parameterTypes);
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
                    matcher.group(1),
                    matcher.group(2),
                    matcher.start(),
                    bodyOpen + 1,
                    bodyClose,
                    lineMap.lineOf(matcher.start()),
                    structureSource.substring(matcher.end(), bodyOpen)
            ));
            matcher.region(bodyClose + 1, structureSource.length());
        }
        return regions;
    }

    private static List<AnnotationUse> leadingAnnotations(String source, LineMap lineMap, int start, int limit) {
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
        return List.copyOf(annotations);
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
        int afterName = skipWhitespace(source, nameEnd, limit);
        if (afterName < limit && source.charAt(afterName) == '(') {
            int parenClose = findMatching(source, afterName, '(', ')');
            if (parenClose < 0 || parenClose >= limit) {
                return null;
            }
            annotationEnd = parenClose + 1;
        }
        return new AnnotationUse(simpleName(source.substring(nameStart, nameEnd)), lineMap.lineOf(atIndex), annotationEnd);
    }

    private static String declaredPackage(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static Map<String, String> importsBySimpleName(String source) {
        Map<String, String> imports = new LinkedHashMap<>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                continue;
            }
            String imported = matcher.group(2);
            if (!imported.endsWith(".*")) {
                imports.put(simpleName(imported), imported);
            }
        }
        return imports;
    }

    private static List<String> wildcardImports(String source) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                continue;
            }
            String imported = matcher.group(2);
            if (imported.endsWith(".*")) {
                imports.add(imported.substring(0, imported.length() - 2));
            }
        }
        return List.copyOf(imports);
    }

    private static List<String> parseHeaderTypes(String header, String keyword) {
        Pattern pattern = Pattern.compile("\\b" + keyword + "\\s+(.+?)(?=\\b(?:extends|implements)\\b|$)");
        Matcher matcher = pattern.matcher(header);
        if (!matcher.find()) {
            return List.of();
        }
        return splitTopLevel(matcher.group(1), ',').stream()
                .map(SourceTextProjectIndexer::cleanTypeName)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static String cleanTypeName(String typeName) {
        String cleaned = stripAnnotations(typeName)
                .replace("...", "")
                .replace("[]", "")
                .trim();
        int genericStart = cleaned.indexOf('<');
        if (genericStart >= 0) {
            cleaned = cleaned.substring(0, genericStart);
        }
        List<String> tokens = tokens(cleaned);
        List<String> typeTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!TYPE_MODIFIERS.contains(token)) {
                typeTokens.add(token);
            }
        }
        if (typeTokens.isEmpty()) {
            return cleaned;
        }
        return typeTokens.get(typeTokens.size() - 1);
    }

    private static String springBeanKind(List<String> annotations) {
        if (annotations.contains("RestController")) {
            return "REST_CONTROLLER";
        }
        if (annotations.contains("Controller")) {
            return "CONTROLLER";
        }
        if (annotations.contains("Service")) {
            return "SERVICE";
        }
        if (annotations.contains("Repository")) {
            return "REPOSITORY";
        }
        if (annotations.contains("Configuration")) {
            return "CONFIGURATION";
        }
        return "COMPONENT";
    }

    private static String beanId(String simpleName) {
        if (simpleName.isBlank()) {
            return simpleName;
        }
        if (simpleName.length() == 1) {
            return simpleName.toLowerCase();
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private static boolean isAbstract(String header) {
        return (" " + header + " ").contains(" abstract ");
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

    private static String stripAnnotations(String value) {
        return value.replaceAll("@[A-Za-z_$][A-Za-z0-9_$.]*(?:\\([^)]*\\))?\\s*", " ");
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int angleDepth = 0;
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
            } else if (current == '<') {
                angleDepth++;
            } else if (current == '>') {
                angleDepth = Math.max(0, angleDepth - 1);
            } else if (current == '{') {
                braceDepth++;
            } else if (current == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth = Math.max(0, parenDepth - 1);
            } else if (current == delimiter && angleDepth == 0 && braceDepth == 0 && parenDepth == 0) {
                parts.add(value.substring(start, index).trim());
                start = index + 1;
            }
        }
        String last = value.substring(start).trim();
        if (!last.isBlank()) {
            parts.add(last);
        }
        return List.copyOf(parts);
    }

    private static List<String> tokens(String value) {
        return Arrays.stream(value.trim().split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
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

    private static boolean isAnnotationName(String source, int lowerBound, int index) {
        int previous = previousNonWhitespace(source, lowerBound, index);
        return previous >= lowerBound && source.charAt(previous) == '@';
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
            return portablePath(projectPath.relativize(sourceFile.toAbsolutePath().normalize()));
        } catch (IllegalArgumentException exception) {
            return portablePath(sourceFile);
        }
    }

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String simpleName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
    }

    private record SourceFile(
            Path path,
            String relativePath,
            String packageName,
            Map<String, String> importsBySimpleName,
            List<String> wildcardImports
    ) {
    }

    private static final class ParsedType {
        private final SourceFile sourceFile;
        private final String kind;
        private final String simpleName;
        private final String qualifiedName;
        private final int line;
        private final List<String> annotations;
        private final boolean abstractType;
        private final List<String> rawImplementedTypes = new ArrayList<>();
        private final List<String> rawExtendedTypes = new ArrayList<>();
        private final List<String> implementedInterfaces = new ArrayList<>();
        private final List<JavaMethodDescriptor> methods = new ArrayList<>();

        private ParsedType(
                SourceFile sourceFile,
                String kind,
                String simpleName,
                String qualifiedName,
                int line,
                List<String> annotations,
                boolean abstractType
        ) {
            this.sourceFile = sourceFile;
            this.kind = kind;
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.line = line;
            this.annotations = annotations;
            this.abstractType = abstractType;
        }

        private JavaTypeDescriptor descriptor() {
            return new JavaTypeDescriptor(
                    qualifiedName,
                    sourceFile.packageName(),
                    simpleName,
                    kind,
                    sourceFile.relativePath(),
                    List.copyOf(annotations),
                    List.copyOf(methods),
                    sourceLocation()
            );
        }

        private SourceLocation sourceLocation() {
            return new SourceLocation(sourceFile.relativePath(), line);
        }

        private boolean isConcrete() {
            return ("CLASS".equals(kind) || "RECORD".equals(kind)) && !abstractType;
        }
    }

    private record TypeRegion(
            String kind,
            String name,
            int typeStart,
            int bodyStart,
            int bodyEnd,
            int line,
            String header
    ) {
    }

    private record AnnotationUse(String name, int line, int endIndex) {
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
