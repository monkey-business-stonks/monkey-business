package main;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class TransactionManager {
    private final Account account;
    private final Order order;

    public TransactionManager(Account account, Order order) {
        this.account = account;
        this.order = order;
    }

    public Account getAccount() { return account; }
    public Order getOrder() { return order; }

    // Updates account holdings and cash based on order execution
    public void updateAccount() {
        if (account == null || order == null) {
            throw new IllegalArgumentException("Account and Order cannot be null");
        }
        
        String action = order.getAction();
        String ticker = order.getTicker();
        Double quantity = order.getQuantity();
        BigDecimal executedPrice = BigDecimal.valueOf(order.getExecutedValue());
        
        if (action == null || action.isEmpty()) {
            throw new IllegalArgumentException("Order action cannot be null or empty");
        }
        if (ticker == null || ticker.isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        if (executedPrice == null || executedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Executed price must be greater than zero");
        }
        
        if (action.equalsIgnoreCase("BUY")) {
            // Get existing asset or create new one
            Asset existing = account.getAsset(ticker);
            Asset updated;
            
            if (existing == null) {
                updated = new Asset(ticker, quantity, executedPrice);
            } else {
                updated = existing.withBoughtAverage(quantity, executedPrice);
            }
            
            account.updateAsset(updated);
            
            // Deduct cash from account
            BigDecimal cashSpent = executedPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal newCash = account.getCashBalance().subtract(cashSpent);
            account.setCashBalance(newCash);
            
        } else if (action.equalsIgnoreCase("SELL")) {
            // Remove asset or reduce quantity
            Asset existing = account.getAsset(ticker);
            if (existing != null) {
                Double newQuantity = existing.quantity() - quantity;
                
                if (newQuantity <= 0) {
                    account.removeAsset(existing);
                } else {
                    Asset updated = existing.withQuantity(newQuantity);
                    account.updateAsset(updated);
                }
                
                // Add cash to account from sale
                BigDecimal cashReceived = executedPrice.multiply(BigDecimal.valueOf(quantity));
                BigDecimal newCash = account.getCashBalance().add(cashReceived);
                account.setCashBalance(newCash);
            }
        }
    }

    //updates order status to succeeded
    public void updateStatus() {
        if (order == null) return;
        order.updateExecution(
            ZonedDateTime.now(),
            order.getExecutedValue(),
            Order.OrderStatus.SUCCEEDED
        );
    }
}
