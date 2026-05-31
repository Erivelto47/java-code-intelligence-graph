package com.example.decisiontrace.localcall;

public class UserRegistration {
    public void create(CreateUserRequest request) {
        validateName(request);
    }

    private void validateName(CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
    }
}
