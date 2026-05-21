package com.example.interfaces.single;

import com.example.support.Service;

@Service
public class RegistrationService implements RegistrationUseCase {
    private final UserRepository userRepository;

    public RegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void create() {
        validate();
        userRepository.save();
    }

    private void validate() {
    }
}
