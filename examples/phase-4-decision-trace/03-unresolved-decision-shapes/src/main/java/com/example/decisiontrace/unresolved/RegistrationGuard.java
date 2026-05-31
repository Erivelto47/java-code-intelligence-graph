package com.example.decisiontrace.unresolved;

public class RegistrationGuard {
    public void validate(RegisterRequest request) {
        if (request.email() == null) throw createInlineException(request.email());

        if (request.blocked()) {
            if (request.email().isBlank()) {
                throw new IllegalStateException("Blocked registration");
            }
            throw new IllegalStateException("Blocked registration");
        }

        if (request.legacy()) {
            throw createLegacyException(request.email());
        }

        if (request.name() == null) throw new IllegalArgumentException(buildMessage());

        if (request.missingCode()) throw new IllegalStateException();

        if (request.disabled()) throw new IllegalStateException("Disabled registration"); else allowDisabled();
    }

    private RuntimeException createInlineException(String email) {
        return new IllegalArgumentException(email);
    }

    private RuntimeException createLegacyException(String email) {
        return new IllegalStateException(email);
    }

    private String buildMessage() {
        return "Name is required";
    }

    private void allowDisabled() {
    }

    public record RegisterRequest(
            String email,
            String name,
            boolean blocked,
            boolean legacy,
            boolean missingCode,
            boolean disabled) {
    }
}
