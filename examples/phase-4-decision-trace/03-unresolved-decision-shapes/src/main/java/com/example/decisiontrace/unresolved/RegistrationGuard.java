package com.example.decisiontrace.unresolved;

public class RegistrationGuard {
    public void validate(RegisterRequest request) {
        if (request.email() == null) throw new IllegalArgumentException("Email is required");

        if (request.blocked()) {
            audit(request.email());
            throw new IllegalStateException("Blocked registration");
        }

        if (request.legacy()) {
            throw createLegacyException(request.email());
        }
    }

    private void audit(String email) {
    }

    private RuntimeException createLegacyException(String email) {
        return new IllegalStateException(email);
    }

    public record RegisterRequest(String email, boolean blocked, boolean legacy) {
    }
}
