package com.company;

public class FooService {
    private final OrderRepository repository = new OrderRepository();
    private final PaymentClient paymentClient = new PaymentClient();

    public OrderDto processOrder(Order order) {
        validate(order);
        repository.save(order);
        paymentClient.charge(order);
        return mapper(order);
    }

    private void validate(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order");
        }
    }

    private OrderDto mapper(Order order) {
        return new OrderDto();
    }
}

class Order {
}

class OrderDto {
}

class OrderRepository {
    void save(Order order) {
    }
}

class PaymentClient {
    void charge(Order order) {
    }
}
