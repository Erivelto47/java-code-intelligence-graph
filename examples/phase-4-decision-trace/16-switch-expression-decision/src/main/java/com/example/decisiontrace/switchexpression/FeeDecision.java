package com.example.decisiontrace.switchexpression;

public class FeeDecision {
    public String resolve(FeeRequest request) {
        String fee = switch (request.type()) {
            case STANDARD -> "standard";
            case PREMIUM -> {
                yield "premium";
            }
            default -> "unknown";
        };
        return fee;
    }
}
