package main;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;

public class OrderManager {
    private final LinkedHashSet<Order> orders = new LinkedHashSet<Order>();

    public OrderManager(){}

    public Order createOrder(String ticker, Double quantity, String action, ZonedDateTime submittedOn, BigDecimal submittedValue) {
        // PricingEngine.getCurrentPrice(ticker)
        UUID id = UUID.randomUUID();
        ZonedDateTime zdt = ZonedDateTime.now();
        Order order = new Order(id, ticker, quantity, action, submittedOn, submittedValue, Order.OrderStatus.PENDING, zdt);
        orders.add(order);
        return order;
    }

    // Order doesn't support changing status alone, not sure if that functionality is necessary as well
    // public void updateStatus(Order order, Order.OrderStatus status) {
    //     order.updateExecution(null, null, status);
    // }

    public void updateExecution(Order order, ZonedDateTime exectutedOn, BigDecimal executedValue, Order.OrderStatus status) {
        order.updateExecution(exectutedOn, executedValue, status);
    }
}
