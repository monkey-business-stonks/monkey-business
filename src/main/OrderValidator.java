package main;

import java.math.BigDecimal;

public class OrderValidator {

    private final PricingEngine pricingEngine;

    public static Boolean isValidTrade(Order order, User user, Account account) {
        if (!user.isActive()) {
            return false;
        }

        if (order.getTicker() == null || order.getTicker().isEmpty()) {
            return false;
        }

        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            return false;
        }

        String action = order.getAction();
        if (action == null || (!action.equalsIgnoreCase("BUY") && !action.equalsIgnoreCase("SELL"))) {
            return false;
        }

        // TODO: Move the following into Account to be validated 
        if (action.equalsIgnoreCase("BUY")) {
            BigDecimal requiredCash = BigDecimal.valueOf(order.getQuantity() * order.getSubmittedValue());
            if (account.getCashBalance().compareTo(requiredCash) < 0) {
                return false;
            }
        } else if (action.equalsIgnoreCase("SELL")) {
            Asset asset = account.getAsset(order.getTicker());
            if (asset == null || asset.quantity() < order.getQuantity()) {
                return false;
            }
        }

        return true;
    }

    public static Order acceptTrade(Order order, User user, Account account) {
        // TODO: Update stub to match actual use of getCurrentPrice()
        if (isValidTrade(order, user, account)) {
            order.updateExecution(
                ZonedDateTime.now(),
                pricingEngine.getCurrentPrice(), 
                Order.OrderStatus.SUCCEEDED
            );
            return order;
        }
    }

    public static Order rejectTrade(Order order) {
        // TODO: Update stub to match actual use of getCurrentPrice()
        order.updateExecution(
            ZonedDateTime.now(),
            pricingEngine.getCurrentPrice(),
            Order.OrderStatus.REJECTED
        );
        return order;
    }
}
