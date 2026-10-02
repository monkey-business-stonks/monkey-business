package domain.service;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;

import domain.entities.Order;

public class OrderManager {
    public final OrderManager OM = new OrderManager();
    private final LinkedHashSet<Order> orders = new LinkedHashSet<Order>();

    private OrderManager(){}

    public Order createOrder(String ticker, Double quantity, String action, ZonedDateTime submittedOn, BigDecimal submittedValue) {
        // PricingEngine.getCurrentPrice(ticker)
        UUID id = UUID.randomUUID();
        ZonedDateTime zdt = ZonedDateTime.now();
        Order order = new Order(
            id,
            "EQUITY", // orderType - TODO: determine from ticker/market data
            ticker,
            quantity,
            action,
            submittedOn,
            submittedValue,
            Order.OrderStatus.SUBMITTED,
            0, // statusCode
            zdt
        );
        orders.add(order);
        return order;
    }

    // Order doesn't support changing status alone, not sure if that functionality is necessary as well
    // public void updateStatus(Order order, Order.OrderStatus status) {
    //     order.updateExecution(null, null, status);
    // }

    public void updateExecution(Order order, ZonedDateTime executedOn, BigDecimal executedValue, Order.OrderStatus status, Integer statusCode) {
        order.updateExecution(executedOn, executedValue, status, statusCode);
    }
}
