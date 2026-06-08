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
        List<DecisionBranch> branches,
        List<DecisionChildDecision> children,
        DecisionParent parent,
        String confidence,
        String selector,
        String assignedTo
) {
    public DecisionNode {
        branches = branches == null ? List.of() : List.copyOf(branches);
        children = children == null ? List.of() : List.copyOf(children);
    }

    public DecisionNode(
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
            List<DecisionBranch> branches,
            List<DecisionChildDecision> children,
            DecisionParent parent,
            String confidence
    ) {
        this(
                id,
                kind,
                category,
                method,
                source,
                sourceLocation,
                expression,
                subjects,
                outcomes,
                evidence,
                links,
                branches,
                children,
                parent,
                confidence,
                null,
                null
        );
    }

    public DecisionNode(
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
        this(
                id,
                kind,
                category,
                method,
                source,
                sourceLocation,
                expression,
                subjects,
                outcomes,
                evidence,
                links,
                List.of(),
                List.of(),
                null,
                confidence,
                null,
                null
        );
    }
}
