package com.example.direct;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void create() {
        orderService.createOrder();
    }
}
