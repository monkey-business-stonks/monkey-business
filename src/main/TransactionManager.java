package main;

public class TransactionManager {
    private final Account account;
    private final Asset asset;
    private final Order order;

    public TransactionManager(Account account, Asset asset, Order order) {
        this.account = account;
        this.asset = asset;
        this.order = order;
    }

    public Account getAccount() { return account; }
    public Asset getAsset() { return asset; }
    public Order getOrder() { return order; }

    // This will be broken up into seperate methods
    public void updateAccount() {
        // TODO: implement updateAccount
        throw new UnsupportedOperationException("updateAccount() not implemented");
    }

    public void updateStatus() {
        // TODO: implement updateStatus
        throw new UnsupportedOperationException("updateStatus() not implemented");
    }
}
