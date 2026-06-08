package com.example.decisiontrace.switchfallthrough;

public class StatusDecision {
    public String resolve(StatusRequest request) {
        switch (request.status()) {
            case "PENDING":
            case "QUEUED":
                return "waiting";
            case "DONE":
                return "complete";
            default:
                return "unknown";
        }
    }
}
