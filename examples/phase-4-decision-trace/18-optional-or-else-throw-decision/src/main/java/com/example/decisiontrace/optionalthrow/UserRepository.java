package com.example.decisiontrace.optionalthrow;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
}
