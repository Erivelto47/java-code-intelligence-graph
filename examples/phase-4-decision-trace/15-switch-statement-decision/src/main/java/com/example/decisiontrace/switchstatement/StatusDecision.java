package com.example.decisiontrace.switchstatement;

public class StatusDecision {
    public String resolve(StatusRequest request) {
        switch (request.status()) {
            case "PENDING":
                return "queued";
            case "APPROVED", "ACTIVE":
                return "open";
            default:
                return "closed";
        }
    }
}
