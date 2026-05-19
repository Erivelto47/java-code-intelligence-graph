package com.codeatlas.core.analyzer;

import com.codeatlas.core.model.FlowGraph;

import java.nio.file.Path;

public interface FlowAnalyzer {
    FlowGraph analyze(Path projectPath, String entrypoint);
}
