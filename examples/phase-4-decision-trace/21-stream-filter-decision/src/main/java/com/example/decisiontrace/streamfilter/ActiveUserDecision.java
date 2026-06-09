package com.example.decisiontrace.streamfilter;

import java.util.List;

public class ActiveUserDecision {
    public List<User> resolve(List<User> users) {
        return users.stream()
                .filter(user -> user.active())
                .toList();
    }
}
