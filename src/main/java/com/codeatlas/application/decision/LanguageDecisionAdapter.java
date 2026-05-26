package com.codeatlas.application.decision;

import com.codeatlas.core.decision.DecisionTrace;

public interface LanguageDecisionAdapter {
    boolean supports(DecisionAnalysisRequest request);

    DecisionTrace analyze(DecisionAnalysisRequest request);
}
