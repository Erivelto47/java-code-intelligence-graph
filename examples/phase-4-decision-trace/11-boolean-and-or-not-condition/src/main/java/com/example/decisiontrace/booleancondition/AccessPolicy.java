package com.example.decisiontrace.booleancondition;

public class AccessPolicy {
    public String resolve(AccessRequest request) {
        if (request.enabled() && (!request.locked() || request.admin())) {
            return "allowed";
        }

        return "denied";
    }
}
