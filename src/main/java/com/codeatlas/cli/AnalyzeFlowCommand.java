package com.codeatlas.cli;

import com.codeatlas.adapter.source.IfThrowDecisionExtractor;
import com.codeatlas.adapter.source.SourceTextFlowAnalyzer;
import com.codeatlas.adapter.source.SourceTextProjectIndexer;
import com.codeatlas.adapter.source.SourceTextSpringEntrypointDiscoverer;
import com.codeatlas.core.analyzer.FlowAnalyzer;
import com.codeatlas.core.analyzer.StubFlowAnalyzer;
import com.codeatlas.core.decision.DecisionTrace;
import com.codeatlas.core.decision.DecisionTraceExtractor;
import com.codeatlas.core.entrypoint.EntrypointAnnotations;
import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.EntrypointDiscoverer;
import com.codeatlas.core.entrypoint.EntrypointIndex;
import com.codeatlas.core.entrypoint.EntrypointKind;
import com.codeatlas.core.entrypoint.SourceLocation;
import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.project.ProjectIndex;
import com.codeatlas.core.project.ProjectIndexHints;
import com.codeatlas.core.project.ProjectIndexUsage;
import com.codeatlas.core.project.ProjectIndexer;
import com.codeatlas.output.context.ContextPackWriter;
import com.codeatlas.output.handoff.AgentHandoffWriter;
import com.codeatlas.output.index.FlowsIndexMarkdownWriter;
import com.codeatlas.output.index.ProjectIndexWriter;
import com.codeatlas.output.json.DecisionTraceJsonWriter;
import com.codeatlas.output.json.EntrypointJsonWriter;
import com.codeatlas.output.json.JsonFlowWriter;
import com.codeatlas.output.json.ProjectIndexJsonReader;
import com.codeatlas.output.json.ProjectIndexJsonReader.ReadResult;
import com.codeatlas.output.json.ProjectIndexJsonReader.ReadStatus;
import com.codeatlas.output.markdown.DecisionTraceMarkdownWriter;
import com.codeatlas.output.markdown.MarkdownFlowWriter;
import com.codeatlas.output.mermaid.DecisionTraceMermaidWriter;
import com.codeatlas.output.mermaid.MermaidFlowWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AnalyzeFlowCommand {
    private static final String REPOSITORY = "Erivelto47/java-code-intelligence-graph";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final FlowAnalyzer flowAnalyzer;
    private final EntrypointDiscoverer entrypointDiscoverer;
    private final ProjectIndexer projectIndexer;
    private final JsonFlowWriter jsonFlowWriter;
    private final EntrypointJsonWriter entrypointJsonWriter;
    private final MarkdownFlowWriter markdownFlowWriter;
    private final MermaidFlowWriter mermaidFlowWriter;
    private final ContextPackWriter contextPackWriter;
    private final ProjectIndexWriter projectIndexWriter;
    private final ProjectIndexJsonReader projectIndexJsonReader;
    private final FlowsIndexMarkdownWriter flowsIndexMarkdownWriter;
    private final AgentHandoffWriter agentHandoffWriter;
    private final DecisionTraceExtractor decisionTraceExtractor;
    private final DecisionTraceJsonWriter decisionTraceJsonWriter;
    private final DecisionTraceMarkdownWriter decisionTraceMarkdownWriter;
    private final DecisionTraceMermaidWriter decisionTraceMermaidWriter;

    public AnalyzeFlowCommand() {
        this(
                new SourceTextFlowAnalyzer(),
                new SourceTextSpringEntrypointDiscoverer(),
                new SourceTextProjectIndexer(),
                new JsonFlowWriter(),
                new EntrypointJsonWriter(),
                new MarkdownFlowWriter(),
                new MermaidFlowWriter(),
                new ContextPackWriter(),
                new ProjectIndexWriter(),
                new ProjectIndexJsonReader(),
                new FlowsIndexMarkdownWriter(),
                new AgentHandoffWriter(),
                new IfThrowDecisionExtractor(),
                new DecisionTraceJsonWriter(),
                new DecisionTraceMarkdownWriter(),
                new DecisionTraceMermaidWriter()
        );
    }

    AnalyzeFlowCommand(
            FlowAnalyzer flowAnalyzer,
            EntrypointDiscoverer entrypointDiscoverer,
            ProjectIndexer projectIndexer,
            JsonFlowWriter jsonFlowWriter,
            EntrypointJsonWriter entrypointJsonWriter,
            MarkdownFlowWriter markdownFlowWriter,
            MermaidFlowWriter mermaidFlowWriter,
            ContextPackWriter contextPackWriter,
            ProjectIndexWriter projectIndexWriter,
            ProjectIndexJsonReader projectIndexJsonReader,
            FlowsIndexMarkdownWriter flowsIndexMarkdownWriter,
            AgentHandoffWriter agentHandoffWriter,
            DecisionTraceExtractor decisionTraceExtractor,
            DecisionTraceJsonWriter decisionTraceJsonWriter,
            DecisionTraceMarkdownWriter decisionTraceMarkdownWriter,
            DecisionTraceMermaidWriter decisionTraceMermaidWriter
    ) {
        this.flowAnalyzer = flowAnalyzer;
        this.entrypointDiscoverer = entrypointDiscoverer;
        this.projectIndexer = projectIndexer;
        this.jsonFlowWriter = jsonFlowWriter;
        this.entrypointJsonWriter = entrypointJsonWriter;
        this.markdownFlowWriter = markdownFlowWriter;
        this.mermaidFlowWriter = mermaidFlowWriter;
        this.contextPackWriter = contextPackWriter;
        this.projectIndexWriter = projectIndexWriter;
        this.projectIndexJsonReader = projectIndexJsonReader;
        this.flowsIndexMarkdownWriter = flowsIndexMarkdownWriter;
        this.agentHandoffWriter = agentHandoffWriter;
        this.decisionTraceExtractor = decisionTraceExtractor;
        this.decisionTraceJsonWriter = decisionTraceJsonWriter;
        this.decisionTraceMarkdownWriter = decisionTraceMarkdownWriter;
        this.decisionTraceMermaidWriter = decisionTraceMermaidWriter;
    }

    public static void main(String[] args) {
        int exitCode = new AnalyzeFlowCommand().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args) {
        return run(args, System.out, System.err);
    }

    int run(String[] args, PrintStream errorStream) {
        return run(args, System.out, errorStream);
    }

    int run(String[] args, PrintStream outputStream, PrintStream errorStream) {
        if (args.length > 0 && args[0].equals("index-project")) {
            return runIndexProjectCommand(args, outputStream, errorStream);
        }
        if (args.length > 0 && (args[0].equals("list-endpoints") || args[0].equals("list-entrypoints"))) {
            return runListEntrypointsCommand(args, outputStream, errorStream);
        }
        if (args.length > 0 && args[0].equals("analyze-flow")) {
            String[] remainingArgs = new String[args.length - 1];
            System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);
            return runAnalyzeFlowCommand(remainingArgs, errorStream);
        }
        if (args.length > 0 && args[0].equals("analyze-decisions")) {
            return runAnalyzeDecisionsCommand(args, outputStream, errorStream);
        }
        return runAnalyzeFlowCommand(args, errorStream);
    }

    private int runAnalyzeDecisionsCommand(String[] args, PrintStream outputStream, PrintStream errorStream) {
        DecisionArguments arguments = DecisionArguments.parse(args, errorStream);
        if (arguments == null) {
            printUsage(errorStream);
            return 2;
        }

        if (!Files.isDirectory(arguments.projectPath())) {
            errorStream.println("Project path must be an existing directory: " + arguments.projectPath());
            return 2;
        }

        try {
            DecisionTrace trace = decisionTraceExtractor.analyze(arguments.projectPath(), arguments.entrypoint());
            decisionTraceJsonWriter.write(trace, arguments.outputDirectory());
            decisionTraceMarkdownWriter.write(trace, arguments.outputDirectory());
            decisionTraceMermaidWriter.write(trace, arguments.outputDirectory());
            outputStream.println("Analyzed decisions: " + trace.decisions().size());
            outputStream.println("Decision artifacts: " + arguments.outputDirectory());
            return 0;
        } catch (IllegalArgumentException exception) {
            errorStream.println(exception.getMessage());
            return 2;
        } catch (IOException exception) {
            errorStream.println("Failed to write decision outputs: " + exception.getMessage());
            return 1;
        }
    }

    private int runAnalyzeFlowCommand(String[] args, PrintStream errorStream) {
        Arguments arguments = Arguments.parse(args, errorStream);
        if (arguments == null) {
            printUsage(errorStream);
            return 2;
        }

        if (!Files.isDirectory(arguments.projectPath())) {
            errorStream.println("Project path must be an existing directory: " + arguments.projectPath());
            return 2;
        }

        try {
            String javaEntrypoint = arguments.entrypoint();
            if (arguments.endpoint() != null) {
                javaEntrypoint = resolveEndpoint(arguments.projectPath(), arguments.endpoint()).javaEntrypoint();
            }

            Path outputDirectory = arguments.resolvedOutputDirectory(javaEntrypoint);
            FlowAnalyzer selectedAnalyzer = arguments.useStub() ? new StubFlowAnalyzer() : flowAnalyzer;
            FlowGraph graph = analyze(arguments.projectPath(), javaEntrypoint, selectedAnalyzer);
            jsonFlowWriter.write(graph, outputDirectory);
            markdownFlowWriter.write(graph, outputDirectory);
            mermaidFlowWriter.write(graph, outputDirectory);
            contextPackWriter.write(graph, outputDirectory);
            agentHandoffWriter.write(
                    graph,
                    arguments.projectPath(),
                    outputDirectory,
                    REPOSITORY,
                    arguments.outputExplicit(),
                    arguments.useStub()
            );
            if (!arguments.outputExplicit()) {
                writeProjectIndexOutputs(arguments.projectPath());
            }
            return 0;
        } catch (IllegalArgumentException exception) {
            errorStream.println(exception.getMessage());
            return 2;
        } catch (IOException exception) {
            errorStream.println("Failed to write flow outputs: " + exception.getMessage());
            return 1;
        }
    }

    private EntrypointDescriptor resolveEndpoint(Path projectPath, Endpoint endpoint) throws IOException {
        Optional<EntrypointIndex> existingIndex = readEntrypointsIndex(projectPath);
        EntrypointIndex index;
        if (existingIndex.isPresent()) {
            index = existingIndex.get();
        } else {
            ProjectIndex projectIndex = projectIndexer.index(projectPath);
            index = entrypointIndex(projectIndex);
            entrypointJsonWriter.write(index, projectPath.resolve(".code-atlas"));
        }

        List<EntrypointDescriptor> matches = index.entrypoints().stream()
                .filter(entrypoint -> entrypoint.httpMethod().equals(endpoint.httpMethod()))
                .filter(entrypoint -> entrypoint.path().equals(endpoint.path()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Endpoint not found: " + endpoint.display()
                    + availableEndpointsMessage(index));
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous endpoint: " + endpoint.display()
                    + candidatesMessage(matches));
        }
        return matches.get(0);
    }

    private int runIndexProjectCommand(String[] args, PrintStream outputStream, PrintStream errorStream) {
        ProjectArguments arguments = ProjectArguments.parse(args, errorStream);
        if (arguments == null) {
            printUsage(errorStream);
            return 2;
        }

        if (!Files.isDirectory(arguments.projectPath())) {
            errorStream.println("Project path must be an existing directory: " + arguments.projectPath());
            return 2;
        }

        try {
            ProjectIndex index = writeProjectIndexOutputs(arguments.projectPath());
            outputStream.println("Indexed project: " + index.project().root());
            outputStream.println("Java types: " + (index.classes().size() + index.interfaces().size()));
            outputStream.println("HTTP endpoints: " + index.entrypoints().size());
            return 0;
        } catch (IllegalArgumentException exception) {
            errorStream.println(exception.getMessage());
            return 2;
        } catch (IOException exception) {
            errorStream.println("Failed to write project index outputs: " + exception.getMessage());
            return 1;
        }
    }

    private ProjectIndex writeProjectIndexOutputs(Path projectPath) throws IOException {
        ProjectIndex index = projectIndexer.index(projectPath);
        projectIndexWriter.write(index, projectPath);
        entrypointJsonWriter.write(entrypointIndex(index), projectPath.resolve(".code-atlas"));
        flowsIndexMarkdownWriter.write(index, projectPath);
        return index;
    }

    private FlowGraph analyze(Path projectPath, String javaEntrypoint, FlowAnalyzer selectedAnalyzer) {
        if (selectedAnalyzer instanceof SourceTextFlowAnalyzer sourceTextAnalyzer) {
            ProjectIndexContext projectIndexContext = loadOrBuildProjectIndex(projectPath);
            return sourceTextAnalyzer.analyze(
                    projectPath,
                    javaEntrypoint,
                    projectIndexContext.hints(),
                    projectIndexContext.usage()
            );
        }
        return selectedAnalyzer.analyze(projectPath, javaEntrypoint);
    }

    private ProjectIndexContext loadOrBuildProjectIndex(Path projectPath) {
        ReadResult readResult = projectIndexJsonReader.readResult(projectPath);
        if (readResult.index().isPresent()) {
            ProjectIndexHints hints = ProjectIndexHints.from(readResult.index().get());
            return new ProjectIndexContext(
                    hints,
                    ProjectIndexUsage.loadedFromJson(
                            hints.implementationMappingCount(),
                            readResult.diagnostics(),
                            readResult.staleSuspected(),
                            readResult.staleReasons()
                    )
            );
        }

        ProjectIndexHints hints = ProjectIndexHints.from(projectIndexer.index(projectPath));
        List<String> diagnostics = new ArrayList<>(readResult.diagnostics());
        diagnostics.add("Using in-memory ProjectIndex fallback");
        ProjectIndexUsage usage = readResult.status() == ReadStatus.INVALID
                ? ProjectIndexUsage.fallbackMemoryInvalidJson(
                        hints.implementationMappingCount(),
                        diagnostics,
                        readResult.staleSuspected(),
                        readResult.staleReasons()
                )
                : ProjectIndexUsage.fallbackMemoryMissingJson(hints.implementationMappingCount(), diagnostics);
        return new ProjectIndexContext(hints, usage);
    }

    private static EntrypointIndex entrypointIndex(ProjectIndex index) {
        Map<String, Object> metadata = new LinkedHashMap<>(index.metadata());
        metadata.put("artifact", "entrypoints");
        return new EntrypointIndex(
                index.schemaVersion(),
                index.project().root(),
                index.generatedAt(),
                index.entrypoints(),
                metadata
        );
    }

    private record ProjectIndexContext(ProjectIndexHints hints, ProjectIndexUsage usage) {
    }

    private Optional<EntrypointIndex> readEntrypointsIndex(Path projectPath) throws IOException {
        Path entrypointsJson = projectPath.resolve(".code-atlas/entrypoints.json");
        if (!Files.isRegularFile(entrypointsJson)) {
            return Optional.empty();
        }

        JsonNode root = OBJECT_MAPPER.readTree(entrypointsJson.toFile());
        String schemaVersion = text(root.get("schemaVersion"), "1.0");
        String project = text(root.get("project"), projectPath.toAbsolutePath().normalize().toString().replace('\\', '/'));
        Instant generatedAt = instant(root.get("generatedAt"));
        List<EntrypointDescriptor> entrypoints = new ArrayList<>();
        JsonNode entrypointsNode = root.get("entrypoints");
        if (entrypointsNode != null && entrypointsNode.isArray()) {
            for (JsonNode entrypointNode : entrypointsNode) {
                entrypoints.add(readEntrypoint(entrypointNode));
            }
        }
        return Optional.of(new EntrypointIndex(schemaVersion, project, generatedAt, List.copyOf(entrypoints), Map.of()));
    }

    private static EntrypointDescriptor readEntrypoint(JsonNode node) {
        String className = text(node.get("className"), text(node.get("controllerClass"), ""));
        String methodName = text(node.get("methodName"), "");
        String javaEntrypoint = text(node.get("javaEntrypoint"),
                className.isBlank() || methodName.isBlank() ? "" : className + "." + methodName);
        return new EntrypointDescriptor(
                text(node.get("id"), "http:" + text(node.get("httpMethod"), "") + ":" + text(node.get("path"), "")
                        + " -> " + javaEntrypoint),
                EntrypointKind.valueOf(text(node.get("kind"), "HTTP_ENDPOINT")),
                text(node.get("httpMethod"), ""),
                text(node.get("path"), ""),
                className,
                methodName,
                javaEntrypoint,
                readAnnotations(node.get("annotations")),
                readSourceLocation(node)
        );
    }

    private static EntrypointAnnotations readAnnotations(JsonNode annotationsNode) {
        if (annotationsNode == null || annotationsNode.isNull()) {
            return new EntrypointAnnotations(List.of(), List.of());
        }
        if (annotationsNode.isArray()) {
            List<String> annotations = new ArrayList<>();
            for (JsonNode annotation : annotationsNode) {
                annotations.add(annotation.asText());
            }
            return new EntrypointAnnotations(List.of(), List.copyOf(annotations));
        }

        List<String> classLevel = new ArrayList<>();
        List<String> methodLevel = new ArrayList<>();
        JsonNode classLevelNode = annotationsNode.get("classLevel");
        if (classLevelNode != null && classLevelNode.isArray()) {
            for (JsonNode annotation : classLevelNode) {
                classLevel.add(annotation.asText());
            }
        }
        JsonNode methodLevelNode = annotationsNode.get("methodLevel");
        if (methodLevelNode != null && methodLevelNode.isArray()) {
            for (JsonNode annotation : methodLevelNode) {
                methodLevel.add(annotation.asText());
            }
        }
        return new EntrypointAnnotations(List.copyOf(classLevel), List.copyOf(methodLevel));
    }

    private static SourceLocation readSourceLocation(JsonNode node) {
        JsonNode sourceLocation = node.get("sourceLocation");
        if (sourceLocation != null && sourceLocation.isObject()) {
            return new SourceLocation(
                    text(sourceLocation.get("file"), text(node.get("sourceFile"), "")),
                    sourceLocation.has("line") ? sourceLocation.get("line").asInt() : 0
            );
        }
        String sourceFile = text(node.get("sourceFile"), "");
        return sourceFile.isBlank() ? null : new SourceLocation(sourceFile, 0);
    }

    private static String text(JsonNode node, String fallback) {
        return node == null || node.isNull() ? fallback : node.asText();
    }

    private static Instant instant(JsonNode node) {
        if (node == null || node.isNull()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(node.asText());
        } catch (DateTimeParseException exception) {
            return Instant.EPOCH;
        }
    }

    private static String availableEndpointsMessage(EntrypointIndex index) {
        if (index.entrypoints().isEmpty()) {
            return "\nNo Spring HTTP endpoints were discovered.";
        }

        StringBuilder message = new StringBuilder("\nAvailable endpoints:");
        for (EntrypointDescriptor entrypoint : index.entrypoints()) {
            message.append("\n- ")
                    .append(entrypoint.httpMethod())
                    .append(" ")
                    .append(entrypoint.path())
                    .append(" -> ")
                    .append(entrypoint.javaEntrypoint());
        }
        return message.toString();
    }

    private static String candidatesMessage(List<EntrypointDescriptor> candidates) {
        StringBuilder message = new StringBuilder("\nCandidates:");
        for (EntrypointDescriptor entrypoint : candidates) {
            message.append("\n- ")
                    .append(entrypoint.httpMethod())
                    .append(" ")
                    .append(entrypoint.path())
                    .append(" -> ")
                    .append(entrypoint.javaEntrypoint());
        }
        return message.toString();
    }

    private int runListEntrypointsCommand(String[] args, PrintStream outputStream, PrintStream errorStream) {
        ListEntrypointsArguments arguments = ListEntrypointsArguments.parse(args, errorStream);
        if (arguments == null) {
            printUsage(errorStream);
            return 2;
        }

        if (!Files.isDirectory(arguments.projectPath())) {
            errorStream.println("Project path must be an existing directory: " + arguments.projectPath());
            return 2;
        }

        try {
            EntrypointIndex index = entrypointDiscoverer.discover(arguments.projectPath());
            entrypointJsonWriter.write(index, arguments.outputDirectory());
            String label = args[0].equals("list-endpoints") ? "endpoints" : "entrypoints";
            outputStream.println("Discovered " + label + ": " + index.entrypoints().size());
            if (index.entrypoints().isEmpty()) {
                outputStream.println("No Spring HTTP endpoints were discovered.");
            }
            for (EntrypointDescriptor entrypoint : index.entrypoints()) {
                outputStream.println(entrypoint.httpMethod()
                        + " "
                        + entrypoint.path()
                        + " -> "
                        + entrypoint.javaEntrypoint());
            }
            return 0;
        } catch (IllegalArgumentException exception) {
            errorStream.println(exception.getMessage());
            return 2;
        } catch (IOException exception) {
            errorStream.println("Failed to write entrypoint outputs: " + exception.getMessage());
            return 1;
        }
    }

    private static void printUsage(PrintStream errorStream) {
        errorStream.println("Usage:");
        errorStream.println("  index-project --project <path>");
        errorStream.println("  analyze-decisions --project <path> --entrypoint <qualified.class.method> [--output <path>]");
        errorStream.println("  analyze-flow --project <path> --entrypoint <qualified.class.method> [--output <path>] [--stub]");
        errorStream.println("  analyze-flow --project <path> --endpoint '<HTTP_METHOD> <path>' [--output <path>] [--stub]");
        errorStream.println("  --project <path> --entrypoint <qualified.class.method> [--output <path>] [--stub]");
        errorStream.println("  list-endpoints --project <path>");
        errorStream.println("  list-entrypoints --project <path> [--output <path>]");
    }

    private record DecisionArguments(
            Path projectPath,
            String entrypoint,
            Path outputDirectory
    ) {
        private static DecisionArguments parse(String[] args, PrintStream errorStream) {
            Path projectPath = null;
            String entrypoint = null;
            Path outputDirectory = null;

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--project" -> {
                        String value = Arguments.readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        projectPath = Path.of(value);
                    }
                    case "--entrypoint" -> {
                        String value = Arguments.readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        entrypoint = value.trim();
                    }
                    case "--output" -> {
                        String value = Arguments.readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        outputDirectory = Path.of(value);
                    }
                    default -> {
                        errorStream.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }

            if (projectPath == null) {
                errorStream.println("Missing required argument: --project");
                return null;
            }
            if (entrypoint == null || !Arguments.isValidEntrypoint(entrypoint)) {
                errorStream.println("Missing or invalid required argument: --entrypoint");
                return null;
            }

            Path resolvedOutputDirectory = outputDirectory == null
                    ? defaultOutputDirectory(projectPath, entrypoint)
                    : outputDirectory;
            return new DecisionArguments(projectPath, entrypoint, resolvedOutputDirectory);
        }

        private static Path defaultOutputDirectory(Path projectPath, String entrypoint) {
            int methodSeparator = entrypoint.lastIndexOf('.');
            String classQualifiedName = entrypoint.substring(0, methodSeparator);
            String methodName = entrypoint.substring(methodSeparator + 1);
            int classSeparator = classQualifiedName.lastIndexOf('.');
            String packageName = classQualifiedName.substring(0, classSeparator);
            String className = classQualifiedName.substring(classSeparator + 1);

            Path outputDirectory = projectPath.resolve(".code-atlas").resolve("decisions");
            for (String packageSegment : packageName.split("\\.")) {
                outputDirectory = outputDirectory.resolve(packageSegment);
            }
            return outputDirectory.resolve(className).resolve(methodName);
        }
    }

    private record Arguments(
            Path projectPath,
            String entrypoint,
            Endpoint endpoint,
            Path outputDirectory,
            boolean outputExplicit,
            boolean useStub
    ) {
        private static Arguments parse(String[] args, PrintStream errorStream) {
            Path projectPath = null;
            String entrypoint = null;
            Endpoint endpoint = null;
            Path outputDirectory = null;
            boolean outputExplicit = false;
            boolean useStub = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--project" -> {
                        String value = readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        projectPath = Path.of(value);
                    }
                    case "--entrypoint" -> {
                        String value = readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        entrypoint = value.trim();
                    }
                    case "--endpoint" -> {
                        String value = readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        endpoint = Endpoint.parse(value);
                        if (endpoint == null) {
                            errorStream.println("Missing or invalid argument: --endpoint");
                            return null;
                        }
                    }
                    case "--output" -> {
                        String value = readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        outputDirectory = Path.of(value);
                        outputExplicit = true;
                    }
                    case "--stub" -> useStub = true;
                    default -> {
                        errorStream.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }

            if (projectPath == null) {
                errorStream.println("Missing required argument: --project");
                return null;
            }
            if (entrypoint != null && endpoint != null) {
                errorStream.println("Use either --entrypoint or --endpoint, not both");
                return null;
            }
            if (entrypoint == null && endpoint == null) {
                errorStream.println("Missing required argument: --entrypoint or --endpoint");
                return null;
            }
            if (entrypoint != null && !isValidEntrypoint(entrypoint)) {
                errorStream.println("Missing or invalid required argument: --entrypoint");
                return null;
            }

            Path resolvedOutputDirectory = outputDirectory;
            if (entrypoint != null && outputDirectory == null) {
                resolvedOutputDirectory = defaultOutputDirectory(projectPath, entrypoint);
            }

            return new Arguments(projectPath, entrypoint, endpoint, resolvedOutputDirectory, outputExplicit, useStub);
        }

        private Path resolvedOutputDirectory(String javaEntrypoint) {
            return outputDirectory == null
                    ? defaultOutputDirectory(projectPath, javaEntrypoint)
                    : outputDirectory;
        }

        private static Path defaultOutputDirectory(Path projectPath, String entrypoint) {
            int methodSeparator = entrypoint.lastIndexOf('.');
            String classQualifiedName = entrypoint.substring(0, methodSeparator);
            String methodName = entrypoint.substring(methodSeparator + 1);
            int classSeparator = classQualifiedName.lastIndexOf('.');
            String packageName = classQualifiedName.substring(0, classSeparator);
            String className = classQualifiedName.substring(classSeparator + 1);

            Path outputDirectory = projectPath.resolve(".code-atlas").resolve("flows");
            for (String packageSegment : packageName.split("\\.")) {
                outputDirectory = outputDirectory.resolve(packageSegment);
            }
            return outputDirectory.resolve(className).resolve(methodName);
        }

        private static String readValue(String[] args, int index, String option, PrintStream errorStream) {
            if (index >= args.length || args[index].startsWith("--")) {
                errorStream.println("Missing value for " + option);
                return null;
            }
            return args[index];
        }

        private static boolean isValidEntrypoint(String entrypoint) {
            if (entrypoint == null || entrypoint.isBlank() || entrypoint.contains(" ")) {
                return false;
            }
            int firstSeparator = entrypoint.indexOf('.');
            int lastSeparator = entrypoint.lastIndexOf('.');
            return firstSeparator > 0
                    && lastSeparator > firstSeparator
                    && lastSeparator < entrypoint.length() - 1;
        }
    }

    private record ProjectArguments(Path projectPath) {
        private static ProjectArguments parse(String[] args, PrintStream errorStream) {
            Path projectPath = null;

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--project" -> {
                        String value = Arguments.readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        projectPath = Path.of(value);
                    }
                    default -> {
                        errorStream.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }

            if (projectPath == null) {
                errorStream.println("Missing required argument: --project");
                return null;
            }
            return new ProjectArguments(projectPath);
        }
    }

    private record Endpoint(String httpMethod, String path) {
        private static Endpoint parse(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isBlank()) {
                return null;
            }

            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length != 2) {
                return null;
            }

            String httpMethod = parts[0].trim().toUpperCase(Locale.ROOT);
            String path = normalizePath(parts[1].trim());
            if (!httpMethod.matches("[A-Z]+") || path.isBlank() || path.contains(" ")) {
                return null;
            }
            return new Endpoint(httpMethod, path);
        }

        private String display() {
            return httpMethod + " " + path;
        }

        private static String normalizePath(String value) {
            if (value == null) {
                return "";
            }
            String normalized = value.trim();
            if (normalized.isBlank()) {
                return "";
            }
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            normalized = normalized.replaceAll("/{2,}", "/");
            while (normalized.length() > 1 && normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        }
    }

    private record ListEntrypointsArguments(
            Path projectPath,
            Path outputDirectory
    ) {
        private static ListEntrypointsArguments parse(String[] args, PrintStream errorStream) {
            Path projectPath = null;
            Path outputDirectory = null;

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--project" -> {
                        String value = Arguments.readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        projectPath = Path.of(value);
                    }
                    case "--output" -> {
                        if (args[0].equals("list-endpoints")) {
                            errorStream.println("Unknown argument: " + arg);
                            return null;
                        }
                        String value = Arguments.readValue(args, ++i, arg, errorStream);
                        if (value == null) {
                            return null;
                        }
                        outputDirectory = Path.of(value);
                    }
                    default -> {
                        errorStream.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }

            if (projectPath == null) {
                errorStream.println("Missing required argument: --project");
                return null;
            }

            Path resolvedOutputDirectory = outputDirectory == null
                    ? projectPath.resolve(".code-atlas")
                    : outputDirectory;
            return new ListEntrypointsArguments(projectPath, resolvedOutputDirectory);
        }
    }
}
