package com.example.decisiontrace.optionalifpresent;

import java.util.Optional;

public class NotificationDecision {
    public void resolve(Optional<User> user) {
        user.ifPresentOrElse(
                value -> notify(value),
                () -> auditMissing());
    }

    private void notify(User user) {
    }

    private void auditMissing() {
    }
}
