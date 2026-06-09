package com.example.decisiontrace.streamanymatch;

import java.util.List;

public class InvalidItemDecision {
    public boolean resolve(List<Item> items) {
        if (items.stream().anyMatch(item -> item.invalid())) {
            return false;
        }
        return true;
    }
}
