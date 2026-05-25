package com.codeatlas.core.decision;

import java.nio.file.Path;

public interface DecisionTraceExtractor {
    DecisionTrace analyze(Path projectPath, String entrypoint);
}
