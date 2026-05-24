package com.codeatlas.cli;

import com.codeatlas.adapter.source.SourceTextFlowAnalyzer;
import com.codeatlas.adapter.source.SourceTextSpringEntrypointDiscoverer;
import com.codeatlas.core.analyzer.FlowAnalyzer;
import com.codeatlas.core.analyzer.StubFlowAnalyzer;
import com.codeatlas.core.entrypoint.EntrypointDescriptor;
import com.codeatlas.core.entrypoint.EntrypointDiscoverer;
import com.codeatlas.core.entrypoint.EntrypointIndex;
import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.output.context.ContextPackWriter;
import com.codeatlas.output.handoff.AgentHandoffWriter;
import com.codeatlas.output.index.FlowsIndexMarkdownWriter;
import com.codeatlas.output.index.ProjectIndexWriter;
import com.codeatlas.output.json.EntrypointJsonWriter;
import com.codeatlas.output.json.JsonFlowWriter;
import com.codeatlas.output.markdown.MarkdownFlowWriter;
import com.codeatlas.output.mermaid.MermaidFlowWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class AnalyzeFlowCommand {
    private static final String REPOSITORY = "Erivelto47/java-code-intelligence-graph";

    private final FlowAnalyzer flowAnalyzer;
    private final EntrypointDiscoverer entrypointDiscoverer;
    private final JsonFlowWriter jsonFlowWriter;
    private final EntrypointJsonWriter entrypointJsonWriter;
    private final MarkdownFlowWriter markdownFlowWriter;
    private final MermaidFlowWriter mermaidFlowWriter;
    private final ContextPackWriter contextPackWriter;
    private final ProjectIndexWriter projectIndexWriter;
    private final FlowsIndexMarkdownWriter flowsIndexMarkdownWriter;
    private final AgentHandoffWriter agentHandoffWriter;

    public AnalyzeFlowCommand() {
        this(
                new SourceTextFlowAnalyzer(),
                new SourceTextSpringEntrypointDiscoverer(),
                new JsonFlowWriter(),
                new EntrypointJsonWriter(),
                new MarkdownFlowWriter(),
                new MermaidFlowWriter(),
                new ContextPackWriter(),
                new ProjectIndexWriter(),
                new FlowsIndexMarkdownWriter(),
                new AgentHandoffWriter()
        );
    }

    AnalyzeFlowCommand(
            FlowAnalyzer flowAnalyzer,
            EntrypointDiscoverer entrypointDiscoverer,
            JsonFlowWriter jsonFlowWriter,
            EntrypointJsonWriter entrypointJsonWriter,
            MarkdownFlowWriter markdownFlowWriter,
            MermaidFlowWriter mermaidFlowWriter,
            ContextPackWriter contextPackWriter,
            ProjectIndexWriter projectIndexWriter,
            FlowsIndexMarkdownWriter flowsIndexMarkdownWriter,
            AgentHandoffWriter agentHandoffWriter
    ) {
        this.flowAnalyzer = flowAnalyzer;
        this.entrypointDiscoverer = entrypointDiscoverer;
        this.jsonFlowWriter = jsonFlowWriter;
        this.entrypointJsonWriter = entrypointJsonWriter;
        this.markdownFlowWriter = markdownFlowWriter;
        this.mermaidFlowWriter = mermaidFlowWriter;
        this.contextPackWriter = contextPackWriter;
        this.projectIndexWriter = projectIndexWriter;
        this.flowsIndexMarkdownWriter = flowsIndexMarkdownWriter;
        this.agentHandoffWriter = agentHandoffWriter;
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
        if (args.length > 0 && args[0].equals("list-entrypoints")) {
            return runListEntrypointsCommand(args, outputStream, errorStream);
        }
        if (args.length > 0 && args[0].equals("analyze-flow")) {
            String[] remainingArgs = new String[args.length - 1];
            System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);
            return runAnalyzeFlowCommand(remainingArgs, errorStream);
        }
        return runAnalyzeFlowCommand(args, errorStream);
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
            FlowGraph graph = selectedAnalyzer.analyze(arguments.projectPath(), javaEntrypoint);
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
                projectIndexWriter.write(graph, arguments.projectPath(), outputDirectory);
                flowsIndexMarkdownWriter.write(graph, arguments.projectPath(), outputDirectory);
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
        EntrypointIndex index = entrypointDiscoverer.discover(projectPath);
        entrypointJsonWriter.write(index, projectPath.resolve(".code-atlas"));

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
            outputStream.println("Discovered entrypoints: " + index.entrypoints().size());
            for (EntrypointDescriptor entrypoint : index.entrypoints()) {
                outputStream.println("- "
                        + entrypoint.httpMethod()
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
        errorStream.println("  analyze-flow --project <path> --entrypoint <qualified.class.method> [--output <path>] [--stub]");
        errorStream.println("  analyze-flow --project <path> --endpoint '<HTTP_METHOD> <path>' [--output <path>] [--stub]");
        errorStream.println("  --project <path> --entrypoint <qualified.class.method> [--output <path>] [--stub]");
        errorStream.println("  list-entrypoints --project <path> [--output <path>]");
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
