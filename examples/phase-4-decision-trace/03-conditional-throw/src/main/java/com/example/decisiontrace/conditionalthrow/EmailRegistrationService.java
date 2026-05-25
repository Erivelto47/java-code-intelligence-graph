package com.example.decisiontrace.conditionalthrow;

public class EmailRegistrationService {
    private final UserRepository userRepository;

    public EmailRegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        userRepository.save(request.email());
    }

    public record RegisterRequest(String email) {
    }

    interface UserRepository {
        boolean existsByEmail(String email);

        void save(String email);
    }

    static class EmailAlreadyExistsException extends RuntimeException {
        EmailAlreadyExistsException(String email) {
            super("E-mail already exists: " + email);
        }
    }
}
