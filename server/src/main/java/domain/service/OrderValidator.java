package domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import domain.entities.Account;
import domain.entities.Order;

@Service
public class OrderValidator {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalTime US_EQUITY_OPEN = LocalTime.of(13, 30);
    private static final LocalTime US_EQUITY_CLOSE = LocalTime.of(20, 0);
    private static final LocalTime INDIA_EQUITY_OPEN = LocalTime.of(14, 15);
    private static final LocalTime INDIA_EQUITY_CLOSE = LocalTime.of(22, 30);
    private static final LocalTime FOREX_OPEN = LocalTime.of(17, 0);

    private Clock clock = Clock.systemUTC();

    @Autowired
    private PricingEngine pricingEngine;

    @Autowired
    private AssetService assetService;

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

        if (!isWithinTradingHours(order.getOrderType(), order.getTicker())) {
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
            var asset = assetService.getAsset(account, order.getTicker());
            if (asset == null || asset.quantity() < order.getQuantity()) {
                return false;
            }
        }

        return true;
    }

    boolean isWithinTradingHours(String assetType, String ticker) {
        String normalizedAssetType = assetType.toUpperCase();

        if (normalizedAssetType.equals("CRYPTO")) {
            return true;
        }

        ZonedDateTime nowUtc = ZonedDateTime.now(clock.withZone(UTC));
        DayOfWeek day = nowUtc.getDayOfWeek();

        if (normalizedAssetType.equals("EQUITY")) {
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return false;
            }

            boolean indiaEquity = ticker.toUpperCase().endsWith(".NS")
                || ticker.toUpperCase().endsWith(".BO");
            LocalTime open = indiaEquity ? INDIA_EQUITY_OPEN : US_EQUITY_OPEN;
            LocalTime close = indiaEquity ? INDIA_EQUITY_CLOSE : US_EQUITY_CLOSE;
            LocalTime time = nowUtc.toLocalTime();
            return !time.isBefore(open) && time.isBefore(close);
        }

        if (normalizedAssetType.equals("FOREX") || normalizedAssetType.equals("FOREIGN EXCHANGE")) {
            ZonedDateTime nowNewYork = ZonedDateTime.now(clock.withZone(NEW_YORK));
            return switch (nowNewYork.getDayOfWeek()) {
                case SUNDAY -> !nowNewYork.toLocalTime().isBefore(FOREX_OPEN);
                case MONDAY, TUESDAY, WEDNESDAY, THURSDAY -> true;
                case FRIDAY -> nowNewYork.toLocalTime().isBefore(FOREX_OPEN);
                case SATURDAY -> false;
            };
        }

        return false;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Check if account type is compatible with asset type
    * CRYPTO accounts can only hold CRYPTO assets
    * FOREX accounts can only hold FOREX assets
    * ROTH_IRA and BROKERAGE accounts can hold EQUITY only
    * 401K accounts cannot trade
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
                return false;

            case ROTH_IRA:
                return normalizedAssetType.equals("EQUITY");
                
            case BROKERAGE:
                return normalizedAssetType.equals("EQUITY");
                
            default:
                return false;
        }
    }
}
