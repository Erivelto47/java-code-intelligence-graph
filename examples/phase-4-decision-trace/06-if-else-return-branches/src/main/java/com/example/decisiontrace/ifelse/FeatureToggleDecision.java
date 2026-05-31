package com.example.decisiontrace.ifelse;

public class FeatureToggleDecision {
    public boolean resolve(FeatureRequest request) {
        if (request.enabled()) {
            return true;
        } else {
            return false;
        }
    }
}
