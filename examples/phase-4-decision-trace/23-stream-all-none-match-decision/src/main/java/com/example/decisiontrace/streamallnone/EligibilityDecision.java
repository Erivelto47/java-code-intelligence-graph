package com.example.decisiontrace.streamallnone;

import java.util.List;

public class EligibilityDecision {
    public boolean resolve(List<Account> accounts) {
        boolean allActive = accounts.stream().allMatch(account -> account.active());
        boolean noneBlocked = accounts.stream().noneMatch(account -> account.blocked());
        return allActive && noneBlocked;
    }
}
