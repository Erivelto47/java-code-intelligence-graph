package com.codeatlas.core.decision;

import java.util.List;

public record DecisionBranch(
        String kind,
        String condition,
        int order,
        List<DecisionOutcome> outcomes,
        List<String> labels
) {
    public DecisionBranch {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
        labels = labels == null ? List.of() : List.copyOf(labels);
    }

    public DecisionBranch(
            String kind,
            String condition,
            int order,
            List<DecisionOutcome> outcomes
    ) {
        this(kind, condition, order, outcomes, List.of());
    }
}
