package com.example.decisiontrace.ternaryassignment;

public class StatusDecision {
    public String resolve(StatusRequest request) {
        String label = request.active() ? "active" : "inactive";
        return label;
    }
}
