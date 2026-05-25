package com.codeatlas.adapter.source;

import com.codeatlas.core.analyzer.FlowAnalyzer;
import com.codeatlas.core.model.BoundarySymbol;
import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphEdge;
import com.codeatlas.core.model.GraphNode;
import com.codeatlas.core.model.Resolution;
import com.codeatlas.core.model.UnresolvedSymbol;
import com.codeatlas.core.project.ImplementationDescriptor;
import com.codeatlas.core.project.ProjectIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final int MAX_TRAVERSAL_DEPTH = 24;
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "\\bpackage\\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\s*;"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "\\bimport\\s+(static\\s+)?([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*(?:\\.\\*)?)\\s*;"
    );
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );
    private static final Pattern LOCAL_VARIABLE_PATTERN = Pattern.compile(
            "\\b(var|[A-Za-z_$][A-Za-z0-9_$.]*(?:\\s*<[^;=(){}]+>)?(?:\\s*\\[\\])?)\\s+"
                    + "([a-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:new\\s+([A-Za-z_$][A-Za-z0-9_$.]*))?"
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
    private static final Set<String> TYPE_MODIFIERS = Set.of(
            "abstract",
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
    private static final Set<String> BEAN_ANNOTATIONS = Set.of(
            "Component",
            "Repository",
            "Service"
    );

    @Override
    public FlowGraph analyze(Path projectPath, String entrypoint) {
        return analyze(projectPath, entrypoint, null, "none");
    }

    public FlowGraph analyze(Path projectPath, String entrypoint, ProjectIndex projectIndex, String projectIndexSource) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        Entrypoint parsedEntrypoint = Entrypoint.parse(entrypoint);
        Path normalizedProjectPath = projectPath.toAbsolutePath().normalize();
        ProjectSymbolIndex index = ProjectSymbolIndex.build(normalizedProjectPath, projectIndex);

        JavaType entryType = index.type(parsedEntrypoint.classQualifiedName());
        if (entryType == null) {
            throw new IllegalArgumentException("Could not find Java type "
                    + parsedEntrypoint.classQualifiedName()
                    + " under " + normalizedProjectPath);
        }

        JavaMethod entryMethod = entryType.method(parsedEntrypoint.methodName());
        if (entryMethod == null || !entryMethod.hasBody()) {
            throw new IllegalArgumentException("Could not find method " + parsedEntrypoint.methodName()
                    + " in " + parsedEntrypoint.classQualifiedName()
                    + " at " + entryType.sourceFile.relativePath);
        }

        FlowBuildContext context = new FlowBuildContext(index);
        context.addTypeNode(entryType);
        String entrypointNodeId = context.addMethodNode(entryType, entryMethod, "local", true, null);
        context.addDeclaresEdge(entryType, entrypointNodeId, entryMethod.line);

        Deque<MethodRef> worklist = new ArrayDeque<>();
        worklist.add(new MethodRef(entryType, entryMethod, entrypointNodeId, 0));
        Set<String> visited = new LinkedHashSet<>();

        while (!worklist.isEmpty()) {
            MethodRef current = worklist.removeFirst();
            if (!visited.add(current.nodeId()) || !current.method().hasBody()) {
                continue;
            }
            if (current.depth() >= MAX_TRAVERSAL_DEPTH) {
                context.addUnresolved(
                        current.type().qualifiedName + "." + current.method().name,
                        current.nodeId(),
                        "MAX_DEPTH_REACHED",
                        List.of(),
                        "HIGH",
                        Map.of("maxDepth", MAX_TRAVERSAL_DEPTH)
                );
                continue;
            }

            List<CallExpression> calls = findCalls(
                    current.type().sourceFile.source,
                    current.type().sourceFile.strippedSource,
                    current.type().sourceFile.lineMap,
                    current.method()
            );
            int ordinal = 1;
            for (CallExpression call : calls) {
                resolveAndApplyCall(context, current, call, ordinal, worklist);
                ordinal++;
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analyzer", "source-text-flow-analyzer");
        metadata.put("phase", "phase-1-mvp");
        metadata.put("deterministic", true);
        metadata.put("source", "source-text");
        metadata.put("indexedJavaSourceFiles", index.sourceFileCount());
        metadata.put("indexedTypes", index.typeCount());
        metadata.put("maxTraversalDepth", MAX_TRAVERSAL_DEPTH);
        metadata.put("projectIndexAssisted", projectIndex != null);
        metadata.put("projectIndexSource", projectIndex == null ? "none" : projectIndexSource);
        metadata.put("projectIndexImplementations", projectIndex == null ? 0 : projectIndex.implementations().size());

        return new FlowGraph(
                SCHEMA_VERSION,
                parsedEntrypoint.normalized(),
                DETERMINISTIC_GENERATED_AT,
                context.nodes(),
                context.edges(),
                metadata,
                context.unresolved(),
                context.boundaries(),
                context.resolutions()
        );
    }

    private static void resolveAndApplyCall(
            FlowBuildContext context,
            MethodRef current,
            CallExpression call,
            int ordinal,
            Deque<MethodRef> worklist
    ) {
        if (call.receiverName() == null || "this".equals(call.receiverName())) {
            JavaMethod localMethod = current.type().method(call.methodName());
            if (localMethod == null) {
                context.addUnresolved(
                        current.type().qualifiedName + "." + call.methodName(),
                        current.nodeId(),
                        "METHOD_NOT_FOUND",
                        current.type().methodNames(),
                        "HIGH",
                        callAttributes(call)
                );
                return;
            }

            String targetNodeId = context.addMethodNode(current.type(), localMethod, "local", false, null);
            context.addDeclaresEdge(current.type(), targetNodeId, localMethod.line);
            context.addCallEdge(current.nodeId(), targetNodeId, call, ordinal, "FACT", "HIGH");
            if (localMethod.hasBody()) {
                worklist.add(new MethodRef(current.type(), localMethod, targetNodeId, current.depth() + 1));
            }
            return;
        }

        ReceiverResolution receiver = resolveReceiver(context.index(), current, call);
        if (receiver == null) {
            context.addUnresolved(
                    call.receiverName() + "." + call.methodName(),
                    current.nodeId(),
                    "RECEIVER_TYPE_NOT_RESOLVED",
                    List.of(),
                    "MEDIUM",
                    callAttributes(call)
            );
            return;
        }
        if (receiver.skip()) {
            return;
        }

        JavaType declaredType = context.index().type(receiver.qualifiedTypeName());
        if (declaredType == null) {
            addBoundaryCall(
                    context,
                    current,
                    call,
                    ordinal,
                    receiver.qualifiedTypeName(),
                    boundaryKind(receiver.qualifiedTypeName(), call.methodName()),
                    "EXTERNAL_SYMBOL"
            );
            return;
        }

        if (isRepositoryBoundary(declaredType)) {
            context.addTypeNode(declaredType);
            addBoundaryCall(
                    context,
                    current,
                    call,
                    ordinal,
                    declaredType.qualifiedName,
                    "REPOSITORY",
                    "REPOSITORY_BOUNDARY"
            );
            return;
        }
        if ("interface".equals(declaredType.kind)
                && isExternalClientBoundary(declaredType)
                && context.index().implementations(declaredType.qualifiedName).isEmpty()) {
            context.addTypeNode(declaredType);
            addBoundaryCall(
                    context,
                    current,
                    call,
                    ordinal,
                    declaredType.qualifiedName,
                    boundaryKind(declaredType.qualifiedName, call.methodName()),
                    "NO_LOCAL_IMPLEMENTATION"
            );
            return;
        }

        JavaMethod declaredMethod = declaredType.method(call.methodName());
        if (declaredMethod == null && hasExternalSuperType(declaredType)) {
            addBoundaryCall(
                    context,
                    current,
                    call,
                    ordinal,
                    declaredType.qualifiedName,
                    boundaryKind(declaredType.qualifiedName, call.methodName()),
                    "EXTERNAL_SUPERTYPE"
            );
            return;
        }
        if (declaredMethod == null) {
            context.addTypeNode(declaredType);
            context.addUnresolved(
                    declaredType.qualifiedName + "." + call.methodName(),
                    current.nodeId(),
                    "METHOD_NOT_FOUND",
                    declaredType.methodNames(),
                    "HIGH",
                    callAttributes(call)
            );
            return;
        }

        context.addTypeNode(declaredType);
        String declaredMethodNodeId = context.addMethodNode(
                declaredType,
                declaredMethod,
                receiver.source(),
                false,
                call.receiverName()
        );
        context.addDeclaresEdge(declaredType, declaredMethodNodeId, declaredMethod.line);
        context.addCallEdge(current.nodeId(), declaredMethodNodeId, call, ordinal, "FACT", "HIGH");

        if ("interface".equals(declaredType.kind)) {
            resolveInterfaceDispatch(context, declaredType, declaredMethod, declaredMethodNodeId, current, call, worklist);
            return;
        }

        if (declaredMethod.hasBody()) {
            worklist.add(new MethodRef(declaredType, declaredMethod, declaredMethodNodeId, current.depth() + 1));
        }
    }

    private static ReceiverResolution resolveReceiver(
            ProjectSymbolIndex index,
            MethodRef current,
            CallExpression call
    ) {
        String receiverName = call.receiverName();
        if (receiverName == null) {
            return null;
        }

        FieldInfo field = current.type().fields.get(receiverName);
        if (field != null) {
            String qualifiedType = index.resolveTypeName(field.typeName, current.type().sourceFile);
            return new ReceiverResolution(qualifiedType, "field", false);
        }

        if ("log".equals(receiverName) && current.type().annotations.contains("Slf4j")) {
            return new ReceiverResolution("org.slf4j.Logger", "lombok-slf4j-field", false);
        }

        if (current.method().parameters.containsKey(receiverName)) {
            return new ReceiverResolution(
                    index.resolveTypeName(current.method().parameters.get(receiverName), current.type().sourceFile),
                    "parameter",
                    true
            );
        }

        if (current.method().localVariables.containsKey(receiverName)) {
            return new ReceiverResolution(
                    index.resolveTypeName(current.method().localVariables.get(receiverName), current.type().sourceFile),
                    "local-variable",
                    true
            );
        }

        if (!receiverName.isBlank() && Character.isUpperCase(receiverName.charAt(0))) {
            return new ReceiverResolution(
                    index.resolveTypeName(receiverName, current.type().sourceFile),
                    "static-type",
                    false
            );
        }

        return null;
    }

    private static void resolveInterfaceDispatch(
            FlowBuildContext context,
            JavaType interfaceType,
            JavaMethod interfaceMethod,
            String interfaceMethodNodeId,
            MethodRef current,
            CallExpression call,
            Deque<MethodRef> worklist
    ) {
        List<JavaType> implementations = context.index().implementations(interfaceType.qualifiedName);
        if (implementations.isEmpty()) {
            if (isExternalClientBoundary(interfaceType)) {
                addBoundaryCall(
                        context,
                        current,
                        call,
                        0,
                        interfaceType.qualifiedName,
                        boundaryKind(interfaceType.qualifiedName, call.methodName()),
                        "NO_LOCAL_IMPLEMENTATION"
                );
            } else {
                context.addUnresolved(
                        interfaceType.qualifiedName + "." + interfaceMethod.name,
                        interfaceMethodNodeId,
                        "NO_IMPLEMENTATION",
                        List.of(),
                        "HIGH",
                        callAttributes(call)
                );
            }
            return;
        }

        List<JavaType> eligibleImplementations = eligibleImplementations(implementations);
        if (eligibleImplementations.size() != 1) {
            List<String> candidates = implementations.stream()
                    .map(type -> type.qualifiedName + "." + interfaceMethod.name)
                    .toList();
            for (JavaType implementation : implementations) {
                context.addTypeNode(implementation);
                context.addImplementsEdge(implementation, interfaceType);
                JavaMethod implementationMethod = implementation.method(interfaceMethod.name);
                if (implementationMethod != null) {
                    String implementationMethodNodeId = context.addMethodNode(
                            implementation,
                            implementationMethod,
                            "candidate-implementation",
                            false,
                            null
                    );
                    context.addDeclaresEdge(implementation, implementationMethodNodeId, implementationMethod.line);
                }
            }
            context.addUnresolved(
                    interfaceType.qualifiedName + "." + interfaceMethod.name,
                    interfaceMethodNodeId,
                    "MULTIPLE_IMPLEMENTATIONS",
                    candidates,
                    "HIGH",
                    callAttributes(call)
            );
            return;
        }

        JavaType implementation = eligibleImplementations.get(0);
        JavaMethod implementationMethod = implementation.method(interfaceMethod.name);
        context.addTypeNode(implementation);
        context.addImplementsEdge(implementation, interfaceType);
        if (implementationMethod == null) {
            context.addUnresolved(
                    implementation.qualifiedName + "." + interfaceMethod.name,
                    interfaceMethodNodeId,
                    "METHOD_NOT_FOUND",
                    implementation.methodNames(),
                    "HIGH",
                    callAttributes(call)
            );
            return;
        }

        String implementationMethodNodeId = context.addMethodNode(
                implementation,
                implementationMethod,
                "single-implementation",
                false,
                null
        );
        context.addDeclaresEdge(implementation, implementationMethodNodeId, implementationMethod.line);
        context.addResolutionEdge(interfaceMethodNodeId, implementationMethodNodeId, call.line());
        if (implementationMethod.hasBody()) {
            worklist.add(new MethodRef(implementation, implementationMethod, implementationMethodNodeId, current.depth() + 1));
        }
    }

    private static List<JavaType> eligibleImplementations(List<JavaType> implementations) {
        if (implementations.size() == 1) {
            return implementations;
        }
        List<JavaType> beanImplementations = implementations.stream()
                .filter(SourceTextFlowAnalyzer::isSpringBean)
                .toList();
        if (beanImplementations.size() == 1) {
            return beanImplementations;
        }
        return implementations;
    }

    private static boolean isSpringBean(JavaType type) {
        if (type.index.isProjectIndexSpringBean(type.qualifiedName)) {
            return true;
        }
        for (String annotation : type.annotations) {
            if (BEAN_ANNOTATIONS.contains(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static void addBoundaryCall(
            FlowBuildContext context,
            MethodRef current,
            CallExpression call,
            int ordinal,
            String ownerTypeName,
            String boundaryKind,
            String reason
    ) {
        String qualifiedName = ownerTypeName + "." + call.methodName();
        String boundaryNodeId = "boundary:" + qualifiedName;
        context.addBoundaryNode(boundaryNodeId, qualifiedName, call.methodName(), ownerTypeName, call.line(), boundaryKind, reason);
        if (ordinal > 0) {
            context.addCallEdge(current.nodeId(), boundaryNodeId, call, ordinal, "FACT", "HIGH");
        }
        context.addBoundary(
                qualifiedName,
                boundaryNodeId,
                current.nodeId(),
                boundaryKind,
                reason,
                "HIGH",
                callAttributes(call)
        );
    }

    private static List<CallExpression> findCalls(
            String rawSource,
            String strippedSource,
            LineMap lineMap,
            JavaMethod method
    ) {
        List<CallExpression> calls = new ArrayList<>();
        List<SourceRange> lambdaRanges = lambdaRanges(strippedSource, method);
        int index = method.bodyStart;

        while (index < method.bodyEnd) {
            SourceRange ignoredRange = containingRange(lambdaRanges, index);
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
            if (isContinuationOfIdentifierOrMemberAccess(strippedSource, method.bodyStart, firstStart)) {
                index = firstEnd;
                continue;
            }

            String firstName = strippedSource.substring(firstStart, firstEnd);
            if (RESERVED_CALL_WORDS.contains(firstName)) {
                index = firstEnd;
                continue;
            }

            int afterFirst = skipWhitespace(strippedSource, firstEnd, method.bodyEnd);
            if (afterFirst < method.bodyEnd && strippedSource.charAt(afterFirst) == '.') {
                MemberCallScanResult scanResult = scanMemberCall(
                        rawSource,
                        strippedSource,
                        lineMap,
                        method,
                        firstName,
                        firstStart,
                        afterFirst
                );
                if (scanResult.call() != null) {
                    calls.add(scanResult.call());
                    index = Math.max(firstEnd, firstStart + 1);
                    continue;
                }
                index = scanResult.nextIndex();
                continue;
            }
            if (afterFirst < method.bodyEnd && strippedSource.charAt(afterFirst) == '(') {
                CallExpression call = scanLocalCall(rawSource, strippedSource, lineMap, method, firstName, firstStart, afterFirst);
                if (call != null) {
                    calls.add(call);
                    index = Math.max(firstEnd, firstStart + 1);
                    continue;
                }
            }

            index = firstEnd;
        }

        return calls;
    }

    private static List<SourceRange> lambdaRanges(String strippedSource, JavaMethod method) {
        List<SourceRange> ranges = new ArrayList<>();
        int index = method.bodyStart;
        while (index + 1 < method.bodyEnd) {
            if (strippedSource.charAt(index) == '-' && strippedSource.charAt(index + 1) == '>') {
                int bodyStart = skipWhitespace(strippedSource, index + 2, method.bodyEnd);
                if (bodyStart < method.bodyEnd && strippedSource.charAt(bodyStart) == '{') {
                    int bodyClose = findMatching(strippedSource, bodyStart, '{', '}');
                    if (bodyClose > bodyStart && bodyClose <= method.bodyEnd) {
                        ranges.add(new SourceRange(bodyStart, bodyClose + 1));
                        index = bodyClose + 1;
                        continue;
                    }
                } else if (bodyStart < method.bodyEnd) {
                    int bodyEnd = expressionLambdaEnd(strippedSource, bodyStart, method.bodyEnd);
                    ranges.add(new SourceRange(bodyStart, bodyEnd));
                    index = bodyEnd;
                    continue;
                }
            }
            index++;
        }
        return ranges;
    }

    private static int expressionLambdaEnd(String source, int start, int limit) {
        int parenDepth = 0;
        int bracketDepth = 0;
        for (int index = start; index < limit; index++) {
            char current = source.charAt(index);
            if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                if (parenDepth == 0) {
                    return index;
                }
                parenDepth--;
            } else if (current == '[') {
                bracketDepth++;
            } else if (current == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            } else if ((current == ',' || current == ';') && parenDepth == 0 && bracketDepth == 0) {
                return index;
            }
        }
        return limit;
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
        int methodNameStart = skipWhitespace(strippedSource, dotIndex + 1, method.bodyEnd);
        if (methodNameStart >= method.bodyEnd
                || "super".equals(receiverName)
                || !Character.isJavaIdentifierStart(strippedSource.charAt(methodNameStart))) {
            return new MemberCallScanResult(null, dotIndex + 1);
        }

        int methodNameEnd = readIdentifierEnd(strippedSource, methodNameStart);
        String methodName = strippedSource.substring(methodNameStart, methodNameEnd);
        if (RESERVED_CALL_WORDS.contains(methodName)) {
            return new MemberCallScanResult(null, methodNameEnd);
        }

        int parenOpen = skipWhitespace(strippedSource, methodNameEnd, method.bodyEnd);
        if (parenOpen >= method.bodyEnd || strippedSource.charAt(parenOpen) != '(') {
            return new MemberCallScanResult(null, methodNameEnd);
        }

        int parenClose = findMatching(strippedSource, parenOpen, '(', ')');
        if (!isAcceptedCall(method, parenClose)) {
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
        if (!isAcceptedCall(method, parenClose)
                || previousWord(strippedSource, method.bodyStart, methodNameStart).equals("new")) {
            return null;
        }

        String expression = normalizeExpression(rawSource.substring(methodNameStart, parenClose + 1));
        return new CallExpression(null, methodName, expression, lineMap.lineOf(methodNameStart), methodNameStart, parenClose + 1);
    }

    private static boolean isAcceptedCall(JavaMethod method, int parenClose) {
        return parenClose >= 0 && parenClose < method.bodyEnd;
    }

    private static boolean isRepositoryBoundary(JavaType type) {
        if (!"interface".equals(type.kind)) {
            return false;
        }
        if (type.index.isProjectIndexRepository(type.qualifiedName)) {
            return true;
        }
        if (type.simpleName.endsWith("Repository") || type.annotations.contains("Repository")) {
            return true;
        }
        for (String extendedType : type.extendedTypes) {
            if (extendedType.endsWith("Repository")
                    || extendedType.contains(".repository.")
                    || extendedType.contains("JpaRepository")
                    || extendedType.contains("CrudRepository")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExternalClientBoundary(JavaType type) {
        if (type.index.isProjectIndexClient(type.qualifiedName)) {
            return true;
        }
        return type.simpleName.endsWith("Client")
                || type.simpleName.endsWith("Gateway")
                || type.simpleName.endsWith("Port");
    }

    private static boolean hasExternalSuperType(JavaType type) {
        for (String extendedType : type.extendedTypes) {
            if (!type.index.typeExists(extendedType)) {
                return true;
            }
        }
        return false;
    }

    private static String boundaryKind(String ownerTypeName, String methodName) {
        if (ownerTypeName.endsWith("Repository")
                || ownerTypeName.contains(".repository.")
                || methodName.startsWith("find")
                || "save".equals(methodName)) {
            return "REPOSITORY";
        }
        if (ownerTypeName.endsWith("RestClient")
                || ownerTypeName.endsWith("HttpClient")
                || ownerTypeName.contains("web.client")
                || "post".equals(methodName)
                || "retrieve".equals(methodName)) {
            return "HTTP_CLIENT";
        }
        if (ownerTypeName.startsWith("org.springframework.")
                || ownerTypeName.startsWith("jakarta.")
                || ownerTypeName.startsWith("javax.")
                || ownerTypeName.startsWith("org.slf4j.")
                || ownerTypeName.startsWith("io.github.")) {
            return "FRAMEWORK";
        }
        if (ownerTypeName.endsWith("Client") || ownerTypeName.endsWith("Gateway")) {
            return "EXTERNAL_CLIENT";
        }
        return "EXTERNAL_SYMBOL";
    }

    private static Map<String, Object> callAttributes(CallExpression call) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("line", call.line());
        attributes.put("expression", call.expression());
        if (call.receiverName() != null) {
            attributes.put("receiverName", call.receiverName());
        }
        attributes.put("methodName", call.methodName());
        attributes.put("source", "source-text");
        attributes.put("deterministic", true);
        return attributes;
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

    private static Map<String, String> importsBySimpleName(String strippedSource) {
        Map<String, String> imports = new LinkedHashMap<>();
        Matcher matcher = IMPORT_PATTERN.matcher(strippedSource);
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

    private static List<String> wildcardImports(String strippedSource) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(strippedSource);
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

    private static List<TypeRegion> findTypeRegions(String strippedSource, LineMap lineMap) {
        List<TypeRegion> regions = new ArrayList<>();
        Matcher matcher = TYPE_PATTERN.matcher(strippedSource);
        while (matcher.find()) {
            if (isAnnotationName(strippedSource, 0, matcher.start())) {
                continue;
            }
            int bodyOpen = strippedSource.indexOf('{', matcher.end());
            if (bodyOpen < 0) {
                continue;
            }
            int bodyClose = findMatching(strippedSource, bodyOpen, '{', '}');
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
                    strippedSource.substring(matcher.end(), bodyOpen)
            ));
            matcher.region(bodyClose + 1, strippedSource.length());
        }
        return regions;
    }

    private static Map<String, FieldInfo> findFields(String strippedSource, LineMap lineMap, TypeRegion typeRegion) {
        Map<String, FieldInfo> fields = new LinkedHashMap<>();
        int index = typeRegion.bodyStart();
        int statementStart = index;
        int depth = 1;

        while (index < typeRegion.bodyEnd()) {
            char current = strippedSource.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 1) {
                    statementStart = index + 1;
                }
            } else if (current == ';' && depth == 1) {
                String statement = strippedSource.substring(statementStart, index).trim();
                String declarationPart = statement;
                int equals = declarationPart.indexOf('=');
                if (equals >= 0) {
                    declarationPart = declarationPart.substring(0, equals);
                }
                if (!declarationPart.contains("(")) {
                    for (FieldInfo field : parseFieldStatement(statement, lineMap.lineOf(statementStart))) {
                        fields.putIfAbsent(field.name, field);
                    }
                }
                statementStart = index + 1;
            }
            index++;
        }
        return fields;
    }

    private static List<FieldInfo> parseFieldStatement(String statement, int line) {
        String cleaned = stripAnnotations(statement)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        if (cleaned.isBlank()) {
            return List.of();
        }

        int equals = cleaned.indexOf('=');
        if (equals >= 0) {
            cleaned = cleaned.substring(0, equals).trim();
        }
        List<String> declarators = splitTopLevel(cleaned, ',');
        if (declarators.isEmpty()) {
            return List.of();
        }

        ParsedField firstField = parseFieldDeclarator(declarators.get(0), null);
        if (firstField == null) {
            return List.of();
        }

        List<FieldInfo> fields = new ArrayList<>();
        fields.add(new FieldInfo(firstField.name(), firstField.typeName(), line));
        for (int i = 1; i < declarators.size(); i++) {
            ParsedField field = parseFieldDeclarator(declarators.get(i), firstField.typeName());
            if (field != null) {
                fields.add(new FieldInfo(field.name(), field.typeName(), line));
            }
        }
        return fields;
    }

    private static ParsedField parseFieldDeclarator(String declarator, String inheritedType) {
        List<String> tokens = tokens(declarator);
        if (tokens.isEmpty()) {
            return null;
        }
        String fieldName = tokens.get(tokens.size() - 1);
        if (!isIdentifier(fieldName)) {
            return null;
        }

        if (inheritedType != null) {
            return new ParsedField(fieldName, inheritedType);
        }

        List<String> typeTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size() - 1; i++) {
            String token = tokens.get(i);
            if (!TYPE_MODIFIERS.contains(token)) {
                typeTokens.add(token);
            }
        }
        if (typeTokens.isEmpty()) {
            return null;
        }
        return new ParsedField(fieldName, String.join(" ", typeTokens));
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
                if (isAnnotationName(strippedSource, typeRegion.bodyStart(), nameStart)
                        || isContinuationOfIdentifierOrMemberAccess(strippedSource, typeRegion.bodyStart(), nameStart)) {
                    index = nameEnd;
                    continue;
                }
                String methodName = strippedSource.substring(nameStart, nameEnd);
                if (methodName.equals(className) || RESERVED_CALL_WORDS.contains(methodName)) {
                    index = nameEnd;
                    continue;
                }

                int parenOpen = skipWhitespace(strippedSource, nameEnd, typeRegion.bodyEnd());
                if (parenOpen < typeRegion.bodyEnd() && strippedSource.charAt(parenOpen) == '(') {
                    int parenClose = findMatching(strippedSource, parenOpen, '(', ')');
                    if (parenClose > 0 && parenClose < typeRegion.bodyEnd()) {
                        DeclarationEnd declarationEnd = findDeclarationEnd(strippedSource, parenClose + 1, typeRegion.bodyEnd());
                        if (declarationEnd != null) {
                            Map<String, String> parameters = parseParameters(strippedSource.substring(parenOpen + 1, parenClose));
                            JavaMethod javaMethod = new JavaMethod(
                                    methodName,
                                    nameStart,
                                    declarationEnd.bodyStart(),
                                    declarationEnd.bodyEnd(),
                                    lineMap.lineOf(nameStart),
                                    parameters
                            );
                            javaMethod.localVariables.putAll(findLocalVariables(strippedSource, javaMethod));
                            methods.putIfAbsent(methodName, javaMethod);
                            index = declarationEnd.nextIndex();
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

    private static DeclarationEnd findDeclarationEnd(String strippedSource, int start, int limit) {
        int index = skipWhitespace(strippedSource, start, limit);
        while (index < limit) {
            char current = strippedSource.charAt(index);
            if (current == '{') {
                int bodyClose = findMatching(strippedSource, index, '{', '}');
                if (bodyClose > index && bodyClose <= limit) {
                    return new DeclarationEnd(index + 1, bodyClose, bodyClose + 1);
                }
                return null;
            }
            if (current == ';') {
                return new DeclarationEnd(-1, -1, index + 1);
            }
            if (current == '=') {
                return null;
            }
            index++;
        }
        return null;
    }

    private static Map<String, String> parseParameters(String parameterList) {
        Map<String, String> parameters = new LinkedHashMap<>();
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
            String name = tokens.get(tokens.size() - 1);
            if (!isIdentifier(name)) {
                continue;
            }
            parameters.put(name, String.join(" ", tokens.subList(0, tokens.size() - 1)));
        }
        return parameters;
    }

    private static Map<String, String> findLocalVariables(String strippedSource, JavaMethod method) {
        if (!method.hasBody()) {
            return Map.of();
        }
        Map<String, String> localVariables = new LinkedHashMap<>();
        Matcher matcher = LOCAL_VARIABLE_PATTERN.matcher(strippedSource);
        matcher.region(method.bodyStart, method.bodyEnd);
        while (matcher.find()) {
            if (previousWord(strippedSource, method.bodyStart, matcher.start()).equals("new")) {
                continue;
            }
            String declaredType = matcher.group(1);
            String variableName = matcher.group(2);
            String constructorType = matcher.group(3);
            if ("var".equals(declaredType) && constructorType != null) {
                localVariables.putIfAbsent(variableName, constructorType);
            } else if (!"var".equals(declaredType)) {
                localVariables.putIfAbsent(variableName, declaredType);
            }
        }
        return localVariables;
    }

    private static List<String> annotationsBefore(String rawSource, LineMap lineMap, int typeStart) {
        List<String> annotations = new ArrayList<>();
        int line = lineMap.lineOf(typeStart) - 1;
        while (line >= 1) {
            String text = rawSource.substring(lineMap.lineStart(line), lineMap.lineEnd(line)).trim();
            if (text.isBlank()) {
                line--;
                continue;
            }
            if (!text.startsWith("@")) {
                break;
            }
            int end = 1;
            while (end < text.length() && (Character.isJavaIdentifierPart(text.charAt(end)) || text.charAt(end) == '.')) {
                end++;
            }
            annotations.add(0, simpleName(text.substring(1, end)));
            line--;
        }
        return List.copyOf(annotations);
    }

    private static List<String> parseHeaderTypes(String header, String keyword) {
        Pattern pattern = Pattern.compile("\\b" + keyword + "\\s+(.+?)(?=\\b(?:extends|implements)\\b|$)");
        Matcher matcher = pattern.matcher(header);
        if (!matcher.find()) {
            return List.of();
        }
        return splitTopLevel(matcher.group(1), ',').stream()
                .map(SourceTextFlowAnalyzer::cleanTypeName)
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

    private static String stripAnnotations(String value) {
        return value.replaceAll("@[A-Za-z_$][A-Za-z0-9_$.]*(?:\\([^)]*\\))?\\s*", " ");
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int angleDepth = 0;
        int parenDepth = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '<') {
                angleDepth++;
            } else if (current == '>') {
                angleDepth = Math.max(0, angleDepth - 1);
            } else if (current == '(') {
                parenDepth++;
            } else if (current == ')') {
                parenDepth = Math.max(0, parenDepth - 1);
            } else if (current == delimiter && angleDepth == 0 && parenDepth == 0) {
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

    private static List<String> tokens(String value) {
        return Arrays.stream(value.trim().split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static boolean isIdentifier(String value) {
        if (value == null || value.isBlank() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String simpleName(String qualifiedName) {
        int separator = qualifiedName.lastIndexOf('.');
        return separator < 0 ? qualifiedName : qualifiedName.substring(separator + 1);
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

    private static final class FlowBuildContext {
        private final ProjectSymbolIndex index;
        private final LinkedHashMap<String, GraphNode> nodesById = new LinkedHashMap<>();
        private final List<GraphEdge> edges = new ArrayList<>();
        private final Set<String> edgeIds = new LinkedHashSet<>();
        private final List<UnresolvedSymbol> unresolved = new ArrayList<>();
        private final Set<String> unresolvedIds = new LinkedHashSet<>();
        private final List<BoundarySymbol> boundaries = new ArrayList<>();
        private final Set<String> boundaryIds = new LinkedHashSet<>();
        private final List<Resolution> resolutions = new ArrayList<>();
        private final Set<String> resolutionIds = new LinkedHashSet<>();

        private FlowBuildContext(ProjectSymbolIndex index) {
            this.index = index;
        }

        private ProjectSymbolIndex index() {
            return index;
        }

        private List<GraphNode> nodes() {
            return List.copyOf(nodesById.values());
        }

        private List<GraphEdge> edges() {
            return List.copyOf(edges);
        }

        private List<UnresolvedSymbol> unresolved() {
            return List.copyOf(unresolved);
        }

        private List<BoundarySymbol> boundaries() {
            return List.copyOf(boundaries);
        }

        private List<Resolution> resolutions() {
            return List.copyOf(resolutions);
        }

        private String addTypeNode(JavaType type) {
            String nodeId = typeNodeId(type);
            if (!nodesById.containsKey(nodeId)) {
                Map<String, Object> attributes = baseAttributes(type.sourceFile, type.line);
                attributes.put("packageName", type.sourceFile.declaredPackage);
                attributes.put("className", type.simpleName);
                attributes.put("typeKind", type.kind);
                attributes.put("annotations", type.annotations);
                if (!type.implementedInterfaces.isEmpty()) {
                    attributes.put("implementedInterfaces", type.implementedInterfaces);
                }
                if (!type.extendedTypes.isEmpty()) {
                    attributes.put("extendedTypes", type.extendedTypes);
                }
                nodesById.put(nodeId, new GraphNode(
                        nodeId,
                        type.kind.toUpperCase(),
                        type.qualifiedName,
                        type.simpleName,
                        attributes
                ));
            }
            return nodeId;
        }

        private String addMethodNode(
                JavaType type,
                JavaMethod method,
                String resolution,
                boolean entrypoint,
                String receiverName
        ) {
            String nodeId = methodNodeId(type, method.name);
            if (!nodesById.containsKey(nodeId)) {
                Map<String, Object> attributes = baseAttributes(type.sourceFile, method.line);
                attributes.put("methodName", method.name);
                attributes.put("packageName", type.sourceFile.declaredPackage);
                attributes.put("className", type.simpleName);
                attributes.put("ownerType", type.qualifiedName);
                attributes.put("ownerKind", type.kind);
                attributes.put("resolution", resolution);
                attributes.put("declarationOnly", !method.hasBody());
                if (receiverName != null) {
                    attributes.put("receiverName", receiverName);
                }
                if (entrypoint) {
                    attributes.put("entrypoint", true);
                }
                nodesById.put(nodeId, new GraphNode(
                        nodeId,
                        "METHOD",
                        type.qualifiedName + "." + method.name,
                        method.name,
                        attributes
                ));
            }
            return nodeId;
        }

        private void addBoundaryNode(
                String nodeId,
                String qualifiedName,
                String displayName,
                String declaredType,
                int line,
                String boundaryKind,
                String reason
        ) {
            if (!nodesById.containsKey(nodeId)) {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("line", line);
                attributes.put("source", "source-text");
                attributes.put("deterministic", true);
                attributes.put("declaredType", declaredType);
                attributes.put("boundaryKind", boundaryKind);
                attributes.put("reason", reason);
                JavaType type = index.type(declaredType);
                if (type != null) {
                    attributes.put("sourceFile", type.sourceFile.relativePath);
                    attributes.put("packageName", type.sourceFile.declaredPackage);
                    addTypeNode(type);
                    addEdge(
                            new GraphEdge(
                                    "declares:" + typeNodeId(type) + "->" + nodeId,
                                    "DECLARES",
                                    typeNodeId(type),
                                    nodeId,
                                    edgeAttributes(line, null, "FACT", "HIGH")
                            )
                    );
                }
                nodesById.put(nodeId, new GraphNode(nodeId, "BOUNDARY", qualifiedName, displayName, attributes));
            }
        }

        private void addDeclaresEdge(JavaType type, String methodNodeId, int line) {
            addEdge(new GraphEdge(
                    "declares:" + typeNodeId(type) + "->" + methodNodeId,
                    "DECLARES",
                    typeNodeId(type),
                    methodNodeId,
                    edgeAttributes(line, null, "FACT", "HIGH")
            ));
        }

        private void addImplementsEdge(JavaType implementation, JavaType interfaceType) {
            addTypeNode(implementation);
            addTypeNode(interfaceType);
            addEdge(new GraphEdge(
                    "implements:" + typeNodeId(implementation) + "->" + typeNodeId(interfaceType),
                    "IMPLEMENTS",
                    typeNodeId(implementation),
                    typeNodeId(interfaceType),
                    edgeAttributes(implementation.line, null, "FACT", "HIGH")
            ));
        }

        private void addCallEdge(
                String sourceNodeId,
                String targetNodeId,
                CallExpression call,
                int ordinal,
                String evidence,
                String confidence
        ) {
            addEdge(new GraphEdge(
                    "calls:" + sourceNodeId + "->" + targetNodeId + ":" + call.line() + ":" + ordinal,
                    "CALLS",
                    sourceNodeId,
                    targetNodeId,
                    edgeAttributes(call.line(), call.expression(), evidence, confidence)
            ));
        }

        private void addResolutionEdge(String sourceNodeId, String targetNodeId, int line) {
            String edgeId = "resolves:" + sourceNodeId + "->" + targetNodeId;
            addEdge(new GraphEdge(
                    edgeId,
                    "RESOLVES_TO",
                    sourceNodeId,
                    targetNodeId,
                    edgeAttributes(line, null, "INFERRED", "HIGH")
            ));
            if (resolutionIds.add(edgeId)) {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("line", line);
                attributes.put("reason", "SINGLE_IMPLEMENTATION");
                attributes.put("source", "source-text");
                attributes.put("deterministic", true);
                resolutions.add(new Resolution(
                        sourceNodeId,
                        targetNodeId,
                        "INTERFACE_SINGLE_IMPLEMENTATION",
                        "INFERRED",
                        "HIGH",
                        attributes
                ));
            }
        }

        private void addEdge(GraphEdge edge) {
            if (edgeIds.add(edge.id())) {
                edges.add(edge);
            }
        }

        private void addUnresolved(
                String symbol,
                String fromNodeId,
                String reason,
                List<String> candidates,
                String confidence,
                Map<String, Object> attributes
        ) {
            String id = fromNodeId + ":" + reason + ":" + symbol;
            if (unresolvedIds.add(id)) {
                unresolved.add(new UnresolvedSymbol(
                        symbol,
                        fromNodeId,
                        reason,
                        List.copyOf(candidates),
                        confidence,
                        new LinkedHashMap<>(attributes)
                ));
            }
        }

        private void addBoundary(
                String symbol,
                String nodeId,
                String fromNodeId,
                String kind,
                String reason,
                String confidence,
                Map<String, Object> attributes
        ) {
            String id = fromNodeId + ":" + nodeId;
            if (boundaryIds.add(id)) {
                boundaries.add(new BoundarySymbol(
                        symbol,
                        nodeId,
                        fromNodeId,
                        kind,
                        reason,
                        confidence,
                        new LinkedHashMap<>(attributes)
                ));
            }
        }

        private static Map<String, Object> baseAttributes(JavaSourceFile sourceFile, int line) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("source", "source-text");
            attributes.put("sourceFile", sourceFile.relativePath);
            attributes.put("line", line);
            attributes.put("deterministic", true);
            return attributes;
        }

        private static Map<String, Object> edgeAttributes(int line, String expression, String evidence, String confidence) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("line", line);
            if (expression != null) {
                attributes.put("expression", expression);
            }
            attributes.put("evidence", evidence);
            attributes.put("confidence", confidence);
            attributes.put("source", "source-text");
            attributes.put("deterministic", true);
            return attributes;
        }
    }

    private static final class ProjectSymbolIndex {
        private final Path projectPath;
        private final Map<String, JavaType> typesByQualifiedName = new LinkedHashMap<>();
        private final Map<String, List<JavaType>> typesBySimpleName = new LinkedHashMap<>();
        private final Map<String, List<JavaType>> implementationsByInterface = new LinkedHashMap<>();
        private final Set<String> projectIndexSpringBeans = new LinkedHashSet<>();
        private final Set<String> projectIndexRepositories = new LinkedHashSet<>();
        private final Set<String> projectIndexClients = new LinkedHashSet<>();
        private int sourceFileCount;

        private ProjectSymbolIndex(Path projectPath) {
            this.projectPath = projectPath;
        }

        private static ProjectSymbolIndex build(Path projectPath) {
            return build(projectPath, null);
        }

        private static ProjectSymbolIndex build(Path projectPath, ProjectIndex projectIndex) {
            if (!Files.isDirectory(projectPath)) {
                throw new IllegalArgumentException("Project path must be an existing directory: " + projectPath);
            }

            ProjectSymbolIndex index = new ProjectSymbolIndex(projectPath);
            List<Path> sourceFiles = javaSourceFiles(projectPath);
            index.sourceFileCount = sourceFiles.size();
            for (Path sourcePath : sourceFiles) {
                index.indexSourceFile(sourcePath);
            }
            index.resolveTypeReferences();
            index.indexImplementations();
            index.applyProjectIndex(projectIndex);
            return index;
        }

        private void applyProjectIndex(ProjectIndex projectIndex) {
            if (projectIndex == null) {
                return;
            }

            projectIndex.springBeans().forEach(bean -> projectIndexSpringBeans.add(bean.beanType()));
            projectIndex.repositories().forEach(projectIndexRepositories::add);
            projectIndex.clients().forEach(projectIndexClients::add);

            for (ImplementationDescriptor implementation : projectIndex.implementations()) {
                JavaType interfaceType = typesByQualifiedName.get(implementation.interfaceName());
                if (interfaceType == null || !"interface".equals(interfaceType.kind)) {
                    continue;
                }
                List<JavaType> targetImplementations = implementationsByInterface.computeIfAbsent(
                        implementation.interfaceName(),
                        ignored -> new ArrayList<>()
                );
                for (String implementationName : implementation.implementations()) {
                    JavaType implementationType = typesByQualifiedName.get(implementationName);
                    if (implementationType == null || !implementationType.isConcrete()) {
                        continue;
                    }
                    if (targetImplementations.stream().noneMatch(existing ->
                            existing.qualifiedName.equals(implementationType.qualifiedName))) {
                        targetImplementations.add(implementationType);
                    }
                }
                targetImplementations.sort((left, right) -> left.qualifiedName.compareTo(right.qualifiedName));
            }
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

        private void indexSourceFile(Path sourcePath) {
            String source = readSource(sourcePath);
            String strippedSource = stripCommentsAndStrings(source);
            LineMap lineMap = LineMap.from(source);
            JavaSourceFile sourceFile = new JavaSourceFile(
                    sourcePath,
                    relativePath(projectPath, sourcePath),
                    source,
                    strippedSource,
                    declaredPackage(strippedSource),
                    importsBySimpleName(strippedSource),
                    wildcardImports(strippedSource),
                    lineMap
            );
            for (TypeRegion region : findTypeRegions(strippedSource, lineMap)) {
                JavaType type = new JavaType(
                        this,
                        sourceFile,
                        region.kind(),
                        region.name(),
                        sourceFile.declaredPackage.isBlank()
                                ? region.name()
                                : sourceFile.declaredPackage + "." + region.name(),
                        region.line(),
                        annotationsBefore(source, lineMap, region.typeStart()),
                        region.header().contains(" abstract ")
                                || region.header().startsWith("abstract ")
                                || region.header().contains("\nabstract ")
                );
                type.rawImplementedTypes.addAll(parseHeaderTypes(region.header(), "implements"));
                type.rawExtendedTypes.addAll(parseHeaderTypes(region.header(), "extends"));
                type.fields.putAll(findFields(strippedSource, lineMap, region));
                type.methods.putAll(findMethods(strippedSource, lineMap, region, type.simpleName));
                typesByQualifiedName.putIfAbsent(type.qualifiedName, type);
                typesBySimpleName.computeIfAbsent(type.simpleName, ignored -> new ArrayList<>()).add(type);
            }
        }

        private void resolveTypeReferences() {
            for (JavaType type : typesByQualifiedName.values()) {
                for (String rawType : type.rawImplementedTypes) {
                    type.implementedInterfaces.add(resolveTypeName(rawType, type.sourceFile));
                }
                for (String rawType : type.rawExtendedTypes) {
                    type.extendedTypes.add(resolveTypeName(rawType, type.sourceFile));
                }
            }
        }

        private void indexImplementations() {
            for (JavaType type : typesByQualifiedName.values()) {
                if (!type.isConcrete()) {
                    continue;
                }
                for (String interfaceName : type.implementedInterfaces) {
                    implementationsByInterface.computeIfAbsent(interfaceName, ignored -> new ArrayList<>()).add(type);
                }
            }
            for (List<JavaType> implementations : implementationsByInterface.values()) {
                implementations.sort((left, right) -> left.qualifiedName.compareTo(right.qualifiedName));
            }
        }

        private JavaType type(String qualifiedName) {
            return typesByQualifiedName.get(qualifiedName);
        }

        private boolean typeExists(String qualifiedName) {
            return typesByQualifiedName.containsKey(qualifiedName);
        }

        private List<JavaType> implementations(String interfaceName) {
            return implementationsByInterface.getOrDefault(interfaceName, List.of());
        }

        private boolean isProjectIndexSpringBean(String qualifiedName) {
            return projectIndexSpringBeans.contains(qualifiedName);
        }

        private boolean isProjectIndexRepository(String qualifiedName) {
            return projectIndexRepositories.contains(qualifiedName);
        }

        private boolean isProjectIndexClient(String qualifiedName) {
            return projectIndexClients.contains(qualifiedName);
        }

        private int sourceFileCount() {
            return sourceFileCount;
        }

        private int typeCount() {
            return typesByQualifiedName.size();
        }

        private String resolveTypeName(String rawTypeName, JavaSourceFile context) {
            String cleaned = cleanTypeName(rawTypeName);
            if (cleaned.isBlank()) {
                return rawTypeName;
            }
            if (cleaned.contains(".")) {
                return cleaned;
            }
            String imported = context.importsBySimpleName.get(cleaned);
            if (imported != null) {
                return imported;
            }
            String samePackage = context.declaredPackage.isBlank() ? cleaned : context.declaredPackage + "." + cleaned;
            if (typesByQualifiedName.containsKey(samePackage)) {
                return samePackage;
            }
            List<JavaType> candidates = typesBySimpleName.get(cleaned);
            if (candidates != null && candidates.size() == 1) {
                return candidates.get(0).qualifiedName;
            }
            for (String wildcardImport : context.wildcardImports) {
                String wildcardCandidate = wildcardImport + "." + cleaned;
                if (typesByQualifiedName.containsKey(wildcardCandidate)) {
                    return wildcardCandidate;
                }
            }
            if ("String".equals(cleaned)
                    || "Long".equals(cleaned)
                    || "Integer".equals(cleaned)
                    || "Boolean".equals(cleaned)
                    || "Double".equals(cleaned)
                    || "Float".equals(cleaned)
                    || "Object".equals(cleaned)
                    || "Throwable".equals(cleaned)
                    || "RuntimeException".equals(cleaned)) {
                return "java.lang." + cleaned;
            }
            return cleaned;
        }
    }

    private static final class JavaSourceFile {
        private final Path path;
        private final String relativePath;
        private final String source;
        private final String strippedSource;
        private final String declaredPackage;
        private final Map<String, String> importsBySimpleName;
        private final List<String> wildcardImports;
        private final LineMap lineMap;

        private JavaSourceFile(
                Path path,
                String relativePath,
                String source,
                String strippedSource,
                String declaredPackage,
                Map<String, String> importsBySimpleName,
                List<String> wildcardImports,
                LineMap lineMap
        ) {
            this.path = path;
            this.relativePath = relativePath;
            this.source = source;
            this.strippedSource = strippedSource;
            this.declaredPackage = declaredPackage;
            this.importsBySimpleName = importsBySimpleName;
            this.wildcardImports = wildcardImports;
            this.lineMap = lineMap;
        }
    }

    private static final class JavaType {
        private final ProjectSymbolIndex index;
        private final JavaSourceFile sourceFile;
        private final String kind;
        private final String simpleName;
        private final String qualifiedName;
        private final int line;
        private final List<String> annotations;
        private final boolean abstractType;
        private final List<String> rawImplementedTypes = new ArrayList<>();
        private final List<String> rawExtendedTypes = new ArrayList<>();
        private final List<String> implementedInterfaces = new ArrayList<>();
        private final List<String> extendedTypes = new ArrayList<>();
        private final Map<String, FieldInfo> fields = new LinkedHashMap<>();
        private final Map<String, JavaMethod> methods = new LinkedHashMap<>();

        private JavaType(
                ProjectSymbolIndex index,
                JavaSourceFile sourceFile,
                String kind,
                String simpleName,
                String qualifiedName,
                int line,
                List<String> annotations,
                boolean abstractType
        ) {
            this.index = index;
            this.sourceFile = sourceFile;
            this.kind = kind;
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.line = line;
            this.annotations = annotations;
            this.abstractType = abstractType;
        }

        private JavaMethod method(String methodName) {
            return methods.get(methodName);
        }

        private List<String> methodNames() {
            return List.copyOf(methods.keySet());
        }

        private boolean isConcrete() {
            return ("class".equals(kind) || "record".equals(kind)) && !abstractType;
        }
    }

    private static final class JavaMethod {
        private final String name;
        private final int nameStart;
        private final int bodyStart;
        private final int bodyEnd;
        private final int line;
        private final Map<String, String> parameters;
        private final Map<String, String> localVariables = new LinkedHashMap<>();

        private JavaMethod(
                String name,
                int nameStart,
                int bodyStart,
                int bodyEnd,
                int line,
                Map<String, String> parameters
        ) {
            this.name = name;
            this.nameStart = nameStart;
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
            this.line = line;
            this.parameters = parameters;
        }

        private boolean hasBody() {
            return bodyStart >= 0 && bodyEnd >= bodyStart;
        }
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

    private record FieldInfo(String name, String typeName, int line) {
    }

    private record ParsedField(String name, String typeName) {
    }

    private record DeclarationEnd(int bodyStart, int bodyEnd, int nextIndex) {
    }

    private record ReceiverResolution(String qualifiedTypeName, String source, boolean skip) {
    }

    private record MethodRef(JavaType type, JavaMethod method, String nodeId, int depth) {
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

        private int lineStart(int line) {
            return lineStarts[Math.max(0, line - 1)];
        }

        private int lineEnd(int line) {
            if (line < lineStarts.length) {
                return Math.max(lineStart(line), lineStarts[line] - 1);
            }
            return sourceLength;
        }
    }

    private static String typeNodeId(JavaType type) {
        return ("interface".equals(type.kind) ? "interface:" : "class:") + type.qualifiedName;
    }

    private static String methodNodeId(JavaType type, String methodName) {
        return "method:" + type.qualifiedName + "." + methodName;
    }
}
