package com.example.decisiontrace.optionalthrow;

public class UserDecision {
    public User resolve(UserRepository repository, String id) {
        return repository.findById(id)
                .filter(user -> user.active())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
