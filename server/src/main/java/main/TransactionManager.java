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
        try {
            if (account == null || order == null) {
                throw new IllegalArgumentException("Account and Order cannot be null");
            }
            
            String action = order.getAction();
            String ticker = order.getTicker();
            Double quantity = order.getQuantity();
            BigDecimal submittedValue = order.getSubmittedValue();  // Total cost, not unit price
            
            if (action == null || action.isEmpty()) {
                throw new IllegalArgumentException("Order action cannot be null or empty");
            }
            if (ticker == null || ticker.isEmpty()) {
                throw new IllegalArgumentException("Ticker cannot be null or empty");
            }
            if (submittedValue == null || submittedValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Submitted value must be greater than zero");
            }
            
            if (action.equalsIgnoreCase("BUY")) {
                // Calculate unit price
                BigDecimal unitPrice = submittedValue.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP);
                
                // Get existing asset or create new one
                Asset existing = account.getAsset(ticker);
                
                if (existing == null) {
                    // Create new asset with calculated unit price
                    Asset newAsset = new Asset(ticker, quantity, unitPrice);
                    account.updateAsset(newAsset);
                } else {
                    // Update existing asset with new average
                    Asset updated = existing.withBoughtAverage(quantity, unitPrice);
                    account.updateAsset(updated);
                }
                
                // Deduct cash from account (submittedValue is already the total cost)
                BigDecimal newCash = account.getCashBalance().subtract(submittedValue);
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
                    
                    // Add cash to account from sale (submittedValue is already the total proceeds)
                    BigDecimal newCash = account.getCashBalance().add(submittedValue);
                    account.setCashBalance(newCash);
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR in TransactionManager.updateAccount(): " + e.getMessage());
            e.printStackTrace();
            throw e;
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
