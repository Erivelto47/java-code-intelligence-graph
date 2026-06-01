package com.codeatlas.core.decision;

import java.util.List;

public record DecisionBranch(
        String kind,
        String condition,
        int order,
        List<DecisionOutcome> outcomes
) {
    public DecisionBranch {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }
}
