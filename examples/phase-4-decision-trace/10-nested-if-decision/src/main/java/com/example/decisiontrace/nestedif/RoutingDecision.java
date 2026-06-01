package com.example.decisiontrace.nestedif;

public class RoutingDecision {
    public String resolve(RouteRequest request) {
        if (request.internal()) {
            if (request.priority()) {
                return "priority";
            }
            return "internal";
        } else {
            if (request.blocked()) {
                throw new IllegalStateException("Route blocked");
            }
            return "external";
        }
    }
}
