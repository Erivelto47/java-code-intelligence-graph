package com.codeatlas.core.decision;

public record DecisionOutcome(
        String when,
        DecisionOutcomeAction action,
        String target,
        String exceptionType,
        String message,
        String meaning
) {
}
