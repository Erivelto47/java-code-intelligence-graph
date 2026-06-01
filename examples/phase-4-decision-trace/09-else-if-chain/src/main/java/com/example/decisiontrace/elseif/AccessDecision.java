package com.example.decisiontrace.elseif;

public class AccessDecision {
    public String resolve(AccessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        } else if (request.blocked()) {
            return "blocked";
        } else if (request.admin()) {
            return "admin";
        } else {
            return "standard";
        }
    }
}
