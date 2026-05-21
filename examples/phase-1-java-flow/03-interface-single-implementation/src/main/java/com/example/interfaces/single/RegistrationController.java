package com.example.interfaces.single;

public class RegistrationController {
    private final RegistrationUseCase registrationUseCase;

    public RegistrationController(RegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    public void register() {
        registrationUseCase.create();
    }
}
