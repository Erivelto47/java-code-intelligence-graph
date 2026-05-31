package com.example.decisiontrace.ifelsemixed;

public class AccessDecision {
    public boolean resolve(AccessRequest request) {
        if (!request.allowed()) {
            throw new IllegalStateException("Access denied");
        } else {
            return true;
        }
    }
}
