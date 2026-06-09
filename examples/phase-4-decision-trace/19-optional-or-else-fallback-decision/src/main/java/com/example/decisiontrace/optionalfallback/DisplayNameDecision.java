package com.example.decisiontrace.optionalfallback;

import java.util.Optional;

public class DisplayNameDecision {
    public String resolve(Profile profile) {
        String displayName = Optional.ofNullable(profile.name())
                .map(String::trim)
                .orElse("anonymous");
        return displayName;
    }
}
