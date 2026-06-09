package com.example.decisiontrace.ternarynested;

public class StatusDecision {
    public String resolve(StatusRequest request) {
        return request.active() ? (request.admin() ? "admin" : "active") : "inactive";
    }
}
