package com.codeatlas.core.decision;

import java.util.List;

public record DecisionLinks(
        List<String> flowNodeIds,
        List<String> calledMethods,
        List<String> relatedBoundaries
) {
}
