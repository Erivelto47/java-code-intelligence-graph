package com.example.externalclient;

public class PaymentService {
    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentService(PaymentGatewayClient paymentGatewayClient) {
        this.paymentGatewayClient = paymentGatewayClient;
    }

    public void pay() {
        paymentGatewayClient.authorize();
    }
}
