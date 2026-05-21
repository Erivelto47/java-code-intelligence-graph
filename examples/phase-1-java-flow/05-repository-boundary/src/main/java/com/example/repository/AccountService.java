package com.example.repository;

public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void openAccount() {
        accountRepository.findByDocument();
        accountRepository.save();
    }
}
