package domain.service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import domain.entities.Account;
import domain.entities.Asset;
import domain.entities.Order;

@Service
public class OrderExecutionEngine {
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private PricingEngine pricingEngine;
    
    @Autowired
    private AccountService accountService;

    /**
     * Updates account after successful trade execution
     * - Updates holdings (via AssetService)
     * - Deducts/adds cash
     * - Calculates total balance (cash + asset values)
     * - Links order to account
     * 
     * @param account Account to update
     * @param order Executed order
     */
    public void updateAccount(Account account, Order order) {
        try {
            if (account == null || order == null) {
                throw new IllegalArgumentException("Account and Order cannot be null");
            }
            
            String action = order.getAction();
            String ticker = order.getTicker();
            Double quantity = order.getQuantity();
            BigDecimal executedValue = order.getExecutedValue();  // Total cost/proceeds
            
            // Validation
            if (action == null || action.isEmpty()) {
                throw new IllegalArgumentException("Order action cannot be null or empty");
            }
            if (ticker == null || ticker.isEmpty()) {
                throw new IllegalArgumentException("Ticker cannot be null or empty");
            }
            if (executedValue == null || executedValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Executed value cannot be negative");
            }
            
            // CRITICAL: Link order to account
            account.addOrder(order);
            
            // Update holdings via AssetService
            if (action.equalsIgnoreCase("BUY")) {
                // Calculate unit price for cost basis
                BigDecimal unitPrice = executedValue.divide(
                    BigDecimal.valueOf(quantity), 
                    10, 
                    java.math.RoundingMode.HALF_UP
                );
                
                // Use AssetService to update holdings (handles deduplication and DB persistence)
                assetService.updateAssetOnBuy(account, ticker, quantity, unitPrice);
                
                // Deduct cash from account
                BigDecimal newCash = account.getCashBalance().subtract(executedValue);
                account.setCashBalance(newCash);
                
            } else if (action.equalsIgnoreCase("SELL")) {
                // Use AssetService to update holdings
                assetService.updateAssetOnSell(account, ticker, quantity);
                
                // Add cash to account from sale proceeds
                BigDecimal newCash = account.getCashBalance().add(executedValue);
                account.setCashBalance(newCash);
            }
            
            // CRITICAL: Calculate total balance = cashBalance + sum(assets' current market values)
            BigDecimal totalAssetValue = calculateTotalAssetValue(account);
            BigDecimal totalBalance = account.getCashBalance().add(totalAssetValue);
            account.setBalance(totalBalance.doubleValue());  // Convert BigDecimal to double
            
            // Persist account changes
            accountService.saveAccount(account);
            
        } catch (Exception e) {
            System.err.println("ERROR in TransactionManager.updateAccount(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Calculate total value of all assets at current market prices
     * Iterates through each asset and calls PricingEngine.getPrice()
     * 
     * @param account Account with assets
     * @return Sum of (quantity × currentPrice) for all assets
     */
    private BigDecimal calculateTotalAssetValue(Account account) {
        if (account == null) {
            return BigDecimal.ZERO;
        }
        
        Set<Asset> assets = account.getAllAssets();  // Fixed: use getAllAssets()
        if (assets == null || assets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Asset asset : assets) {
            try {
                // Get current market price from PricingEngine
                BigDecimal currentPrice = pricingEngine.getPrice(asset.ticker());  // Fixed: use ticker()
                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal assetValue = currentPrice.multiply(BigDecimal.valueOf(asset.quantity()));
                    totalValue = totalValue.add(assetValue);
                }
            } catch (Exception e) {
                System.err.println("WARNING: Could not fetch price for " + asset.ticker() + ": " + e.getMessage());  // Fixed: use ticker()
                // Continue with other assets even if one fails
            }
        }
        
        return totalValue;
    }

    /**
     * Updates order status to FILLED
     * Called after account has been updated and persisted
     * 
     * @param order Order to update
     */
    public void updateStatus(Order order) {
        if (order == null) {
            return;
        }
        
        order.updateExecution(
            ZonedDateTime.now(),
            order.getExecutedValue(),
            Order.OrderStatus.FILLED,
            201  // HTTP 201 Created
        );
    }
}
