package main;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import main.Order.OrderStatus;

public class OrderExecutor {
    private OrderExecutor() {
    }

    public Order processTrade(Order order, Account account) {
        // 1. Get Current Price
        // getCurrentPrice() would work best as a static method of the class
        // if we commit to the singleton design
        // PricingEngine.getCurrentPrice(order.getTicker());
        BigDecimal currentPrice = new BigDecimal(0);
        // 2. Check if price is valid
        boolean isValid = (account.getCashBalance().compareTo(currentPrice.multiply(BigDecimal.valueOf(order.getQuantity()))) > 0) ? true : false;
        
        // 3. Get current time + status
        ZonedDateTime zdt = ZonedDateTime.now();
        OrderStatus status = (isValid) ? OrderStatus.SUCCEEDED : OrderStatus.REJECTED;
        
        // 4. Ping Transaction manager
        // TransactionManager.updateStatus()
        // Idk if this needs to return an Order object
        return order;
    }
}
