package com.codeatlas.adapter.java.source.decision;

import com.codeatlas.application.decision.DecisionAnalysisRequest;
import com.codeatlas.application.decision.LanguageDecisionAdapter;
import com.codeatlas.core.decision.DecisionTrace;

import java.util.Objects;

public final class JavaSourceDecisionAdapter implements LanguageDecisionAdapter {
    private final JavaIfThrowDecisionExtractor ifThrowDecisionExtractor;

    public JavaSourceDecisionAdapter() {
        this(new JavaIfThrowDecisionExtractor());
    }

    JavaSourceDecisionAdapter(JavaIfThrowDecisionExtractor ifThrowDecisionExtractor) {
        this.ifThrowDecisionExtractor = Objects.requireNonNull(
                ifThrowDecisionExtractor,
                "ifThrowDecisionExtractor must not be null"
        );
    }

    @Override
    public boolean supports(DecisionAnalysisRequest request) {
        return "java".equalsIgnoreCase(request.language());
    }

    @Override
    public DecisionTrace analyze(DecisionAnalysisRequest request) {
        return ifThrowDecisionExtractor.analyze(request.projectPath(), request.entrypoint());
    }
}
