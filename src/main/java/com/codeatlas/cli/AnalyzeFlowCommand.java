package com.codeatlas.cli;

import com.codeatlas.adapter.source.SourceTextFlowAnalyzer;
import com.codeatlas.core.analyzer.FlowAnalyzer;
import com.codeatlas.core.analyzer.StubFlowAnalyzer;
import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.output.context.ContextPackWriter;
import com.codeatlas.output.handoff.AgentHandoffWriter;
import com.codeatlas.output.index.FlowsIndexMarkdownWriter;
import com.codeatlas.output.index.ProjectIndexWriter;
import com.codeatlas.output.json.JsonFlowWriter;
import com.codeatlas.output.markdown.MarkdownFlowWriter;
import com.codeatlas.output.mermaid.MermaidFlowWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AnalyzeFlowCommand {
    private static final String REPOSITORY = "Erivelto47/java-code-intelligence-graph";

    private final FlowAnalyzer flowAnalyzer;
    private final JsonFlowWriter jsonFlowWriter;
    private final MarkdownFlowWriter markdownFlowWriter;
    private final MermaidFlowWriter mermaidFlowWriter;
    private final ContextPackWriter contextPackWriter;
    private final ProjectIndexWriter projectIndexWriter;
    private final FlowsIndexMarkdownWriter flowsIndexMarkdownWriter;
    private final AgentHandoffWriter agentHandoffWriter;

    public AnalyzeFlowCommand() {
        this(
                new SourceTextFlowAnalyzer(),
                new JsonFlowWriter(),
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
            JsonFlowWriter jsonFlowWriter,
            MarkdownFlowWriter markdownFlowWriter,
            MermaidFlowWriter mermaidFlowWriter,
            ContextPackWriter contextPackWriter,
            ProjectIndexWriter projectIndexWriter,
            FlowsIndexMarkdownWriter flowsIndexMarkdownWriter,
            AgentHandoffWriter agentHandoffWriter
    ) {
        this.flowAnalyzer = flowAnalyzer;
        this.jsonFlowWriter = jsonFlowWriter;
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
        return run(args, System.err);
    }

    int run(String[] args, PrintStream errorStream) {
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
            FlowAnalyzer selectedAnalyzer = arguments.useStub() ? new StubFlowAnalyzer() : flowAnalyzer;
            FlowGraph graph = selectedAnalyzer.analyze(arguments.projectPath(), arguments.entrypoint());
            jsonFlowWriter.write(graph, arguments.outputDirectory());
            markdownFlowWriter.write(graph, arguments.outputDirectory());
            mermaidFlowWriter.write(graph, arguments.outputDirectory());
            contextPackWriter.write(graph, arguments.outputDirectory());
            agentHandoffWriter.write(
                    graph,
                    arguments.projectPath(),
                    arguments.outputDirectory(),
                    REPOSITORY,
                    arguments.outputExplicit(),
                    arguments.useStub()
            );
            if (!arguments.outputExplicit()) {
                projectIndexWriter.write(graph, arguments.projectPath(), arguments.outputDirectory());
                flowsIndexMarkdownWriter.write(graph, arguments.projectPath(), arguments.outputDirectory());
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

    private static void printUsage(PrintStream errorStream) {
        errorStream.println("Usage: analyze-flow --project <path> --entrypoint <qualified.class.method> [--output <path>] [--stub]");
    }

    private record Arguments(
            Path projectPath,
            String entrypoint,
            Path outputDirectory,
            boolean outputExplicit,
            boolean useStub
    ) {
        private static Arguments parse(String[] args, PrintStream errorStream) {
            Path projectPath = null;
            String entrypoint = null;
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
            if (!isValidEntrypoint(entrypoint)) {
                errorStream.println("Missing or invalid required argument: --entrypoint");
                return null;
            }

            Path resolvedOutputDirectory = outputDirectory == null
                    ? defaultOutputDirectory(projectPath, entrypoint)
                    : outputDirectory;

            return new Arguments(projectPath, entrypoint, resolvedOutputDirectory, outputExplicit, useStub);
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
}
