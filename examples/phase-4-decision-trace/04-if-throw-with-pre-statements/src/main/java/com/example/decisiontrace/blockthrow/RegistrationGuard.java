package com.example.decisiontrace.blockthrow;

public class RegistrationGuard {
    public void validate(CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            logInvalidName(request);
            throw new IllegalArgumentException("Name is required");
        }
    }

    private void logInvalidName(CreateUserRequest request) {
        // no-op fixture helper
    }

    public record CreateUserRequest(String name) {
    }
}
