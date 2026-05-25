package com.codeatlas.core.decision;

import java.util.List;

public record DecisionNode(
        String id,
        DecisionKind kind,
        DecisionCategory category,
        String method,
        DecisionSource source,
        DecisionSourceLocation sourceLocation,
        DecisionCondition expression,
        List<DecisionSubject> subjects,
        List<DecisionOutcome> outcomes,
        DecisionEvidence evidence,
        DecisionLinks links,
        String confidence
) {
}
