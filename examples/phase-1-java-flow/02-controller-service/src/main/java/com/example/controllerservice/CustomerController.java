package com.example.controllerservice;

public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public void register() {
        customerService.register();
    }
}
