package main;
import java.util.LinkedHashSet;
import java.util.Set;

public class Execution {
    private final LinkedHashSet<Order> orders = new LinkedHashSet<Order>();
    private final Account account;

    public Execution(Order order, Account account) {
        this.orders.add(order);
        this.account = account;
    }

    public Order processTrade() {
        // TODO: implement processTrade
        throw new UnsupportedOperationException("processTrade() not implemented");
    }
}
