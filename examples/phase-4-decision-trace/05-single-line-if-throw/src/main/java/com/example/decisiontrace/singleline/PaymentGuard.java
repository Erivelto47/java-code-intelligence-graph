package com.example.decisiontrace.singleline;

public class PaymentGuard {
    public void validate(PaymentRequest request) {
        if (request.amount() == null) throw new IllegalArgumentException("Amount is required");
    }
}
