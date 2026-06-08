package com.example.decisiontrace.comparisonpredicate;

public class EligibilityDecision {
    public String resolve(EligibilityRequest request) {
        if (request.age() >= 18 && request.name() != null && !request.name().isBlank() && request.email().contains("@")) {
            return "eligible";
        }

        return "manual-review";
    }
}
