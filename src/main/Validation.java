package main;

import java.util.Set;
import java.util.LinkedHashSet;

public class Validation {
    private final LinkedHashSet<Order> orders = new LinkedHashSet<>();
    private final Account account;
    private final Order order;

    public Validation(Order order, Account account) {
        this.order = order;
        this.account = account;
        this.orders.add(order);
    }

    public Set<Order> getOrders() { return orders; }
    public Order getOrder() { return order; }
    public Account getAccount() { return account; }

    public Boolean isValidTrade() {
        // TODO: implement validation logic
        throw new UnsupportedOperationException("isValidTrade() not implemented");
    }

    public Order acceptTrade() {
        // TODO: implement acceptTrade
        throw new UnsupportedOperationException("acceptTrade() not implemented");
    }

    public Order rejectTrade() {
        // TODO: implement rejectTrade
        throw new UnsupportedOperationException("rejectTrade() not implemented");
    }
}
