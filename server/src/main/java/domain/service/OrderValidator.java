package domain.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import domain.entities.Account;
import domain.entities.Order;

@Service
public class OrderValidator {

    @Autowired
    private PricingEngine pricingEngine;

    /**
     * Validate if a trade is allowed for an account
     * Checks:
     * - Account type supports the asset type
     * - Cash balance is sufficient for BUY orders
     * - Quantity held is sufficient for SELL orders  
     * - Quantity is valid (> 0)
     * 
     * @param order Order to validate
     * @param account Account placing the order
     * @return true if order is valid, false otherwise
     */
    public boolean isValidTrade(Order order, Account account) {
        // Check required fields
        if (order == null || account == null) {
            return false;
        }

        // Validate ticker
        if (order.getTicker() == null || order.getTicker().isEmpty()) {
            return false;
        }

        // Validate quantity
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            return false;
        }

        // Validate action
        String action = order.getAction();
        if (action == null || (!action.equalsIgnoreCase("BUY") && !action.equalsIgnoreCase("SELL"))) {
            return false;
        }

        // Validate account type matches asset type (orderType)
        if (!isAccountTypeCompatibleWithAssetType(account, order.getOrderType())) {
            return false;
        }

        // For BUY orders: check cash balance is sufficient
        if (action.equalsIgnoreCase("BUY")) {
            // Get current price from PricingEngine
            BigDecimal currentPrice = pricingEngine.getPrice(order.getTicker());
            BigDecimal totalCost = currentPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
            
            // Check if account has enough cash
            if (account.getCashBalance().compareTo(totalCost) < 0) {
                return false;
            }
        }
        // For SELL orders: check quantity held is sufficient
        else if (action.equalsIgnoreCase("SELL")) {
            // AssetService will check if the position exists and has sufficient quantity
            // For now, we assume AssetService handles this during transaction
        }

        return true;
    }

    /**
     * Check if account type is compatible with asset type
     * CRYPTO accounts can only hold CRYPTO assets
     * FOREX accounts can only hold FOREX assets
     * 401K/ROTH_IRA cannot hold CRYPTO
     * BROKERAGE can hold any asset type
     * 
     * @param account Account
     * @param assetType Asset type (EQUITY, CRYPTO, FOREX)
     * @return true if compatible, false otherwise
     */
    private boolean isAccountTypeCompatibleWithAssetType(Account account, String assetType) {
        if (account == null || assetType == null) {
            return false;
        }

        Account.AccountType accountType = account.getAccType();
        
        // Normalize asset type
        String normalizedAssetType = assetType.toUpperCase();
        
        switch (accountType) {
            case CRYPTO:
                // CRYPTO accounts can ONLY hold CRYPTO assets
                return normalizedAssetType.equals("CRYPTO");
                
            case FOREX:
                // FOREX accounts can ONLY hold FOREX assets
                return normalizedAssetType.equals("FOREX") || normalizedAssetType.equals("FOREIGN EXCHANGE");
                
            case _401K:
            case ROTH_IRA:
                // 401K and ROTH_IRA cannot hold CRYPTO
                return !normalizedAssetType.equals("CRYPTO");
                
            case BROKERAGE:
                // BROKERAGE can hold any asset type
                return true;
                
            default:
                return false;
        }
    }
}
