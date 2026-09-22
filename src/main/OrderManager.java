package main;

import java.util.Set;
import java.util.UUID;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;

public class OrderManager {
    private final Account account;
    private final Asset asset;
    private final LinkedHashSet<Order> orders = new LinkedHashSet<Order>();

    public OrderManager(Account account, Asset asset, Order order) {
        this.account = account;
        this.asset = asset;
        this.orders.add(order);
    }

    public Order createOrder() {
        // TODO: implement createOrder
        throw new UnsupportedOperationException("createOrder() not implemented");
    }

    public void updateStatus() {
        // TODO: implement updateStatus
        throw new UnsupportedOperationException("updateStatus() not implemented");
    }

    public void updateExecution() {
        // TODO: implement updateExecution
        throw new UnsupportedOperationException("updateExecution() not implemented");
    }
}
