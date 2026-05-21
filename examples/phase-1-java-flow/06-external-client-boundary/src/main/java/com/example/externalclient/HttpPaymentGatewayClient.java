package com.example.externalclient;

public class HttpPaymentGatewayClient implements PaymentGatewayClient {
    private final HttpClient httpClient;

    public HttpPaymentGatewayClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void authorize() {
        httpClient.post();
    }
}
