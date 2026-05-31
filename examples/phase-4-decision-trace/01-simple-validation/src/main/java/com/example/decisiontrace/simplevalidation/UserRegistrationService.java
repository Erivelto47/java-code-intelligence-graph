package com.example.decisiontrace.simplevalidation;

public class UserRegistrationService {
    public void register(RegisterRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidUserNameException("Name is required");
        }

        saveUser(request);
    }

    private void saveUser(RegisterRequest request) {
    }

    public record RegisterRequest(String name, String email) {
    }

    static class InvalidUserNameException extends RuntimeException {
        InvalidUserNameException(String message) {
            super(message);
        }
    }
}
