package com.example.decisiontrace.ternaryreturn;

public class StatusDecision {
    public String resolve(StatusRequest request) {
        return request.active() ? "active" : "inactive";
    }
}
