package domain.service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import domain.entities.Order;
import domain.entities.Order.OrderStatus;
import domain.entities.Account;

@Service
public class OrderExecutor {
    
    @Autowired
    private PricingEngine pricingEngine;

    /**
     * Execute a trade - verifies user can afford it and updates order status
     * 
     * @param order Order to execute
     * @param account Account executing the order
     * @return Order with updated status
     */
    public Order processTrade(Order order, Account account) {
        if (order == null || account == null) {
            return order;
        }
        
        try {
            // Step 1: Get current market price from PricingEngine
            BigDecimal currentPrice = pricingEngine.getPrice(order.getTicker());
            if (currentPrice == null || currentPrice.equals(BigDecimal.ZERO)) {
                // Price retrieval failed, mark order as rejected
                order.updateExecution(
                    null,  // executedon must be NULL for rejected orders
                    null,  // executedvalue must be NULL for rejected orders
                    OrderStatus.REJECTED,
                    503  // Service Unavailable
                );
                return order;
            }
            
            // Step 2: Calculate total cost
            BigDecimal quantity = BigDecimal.valueOf(order.getQuantity());
            BigDecimal totalCost = currentPrice.multiply(quantity);
            
            // Step 3: Verify user can afford the trade (for BUY orders)
            if (order.getAction().equalsIgnoreCase("BUY")) {
                if (account.getCashBalance().compareTo(totalCost) < 0) {
                    // Insufficient cash
                    order.updateExecution(
                        null,  // executedon must be NULL for rejected orders
                        null,  // executedvalue must be NULL for rejected orders
                        OrderStatus.REJECTED,
                        422  // Unprocessable Entity
                    );
                    return order;
                }
            }
            
            // Step 4: Update order with execution details
            ZonedDateTime now = ZonedDateTime.now();
            order.updateExecution(
                now,
                totalCost,
                OrderStatus.FILLED,
                201  // Created
            );
            
            return order;
            
        } catch (Exception e) {
            // Any exception during execution -> reject order
            order.updateExecution(
                null,  // executedon must be NULL for rejected orders
                null,  // executedvalue must be NULL for rejected orders
                OrderStatus.REJECTED,
                500  // Internal Server Error
            );
            return order;
        }
    }
}
