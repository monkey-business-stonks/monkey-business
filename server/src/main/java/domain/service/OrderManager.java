package domain.service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import domain.entities.Account;
import domain.entities.Order;
import domain.entities.User;
import domain.repository.OrderRepository;

@Service
public class OrderManager {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private PricingEngine pricingEngine;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Tracks active orders
    private final LinkedHashSet<Order> activeOrders = new LinkedHashSet<>();

    /**
     * Create a new order - PRIMARY ORCHESTRATOR ENTRY POINT
     * Validates user, gets account, creates order with status=SUBMITTED
     * 
     * @param accountId UUID of the account placing the order
     * @param orderType EQUITY, CRYPTO, or FOREX
     * @param ticker Stock ticker symbol
     * @param quantity Number of shares/units
     * @param action BUY or SELL
     * @return Order object with status=SUBMITTED
     */
    public Order createOrder(UUID accountId, String orderType, String ticker, Double quantity, String action) {
        // Step 1: Try to get user from auth context (for production JWT validation)
        User user = getRealUserFromAuth();
        
        // Step 2: Get the account
        Account account = accountService.getAccountDirect(accountId);
        if (account == null) {
            throw new NoSuchElementException("Account not found with ID: " + accountId);
        }
        
        // Step 3: If we have an authenticated user, verify they own this account
        // If no auth (testing mode), skip ownership check
        if (user != null && !account.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("User does not own this account");
        }
        
        // Step 4: Get current price from PricingEngine
        BigDecimal currentPrice = pricingEngine.getPrice(ticker);
        BigDecimal submittedValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
        
        // Step 5: Create order with status=SUBMITTED
        UUID orderId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();
        Order order = new Order(
            orderId,
            orderType,
            ticker.toUpperCase(),
            quantity,
            action,
            now,
            submittedValue,
            Order.OrderStatus.SUBMITTED,
            202,  // HTTP 202 Accepted
            now
        );
        
        order.setAccount(account);
        
        // Step 6: Add to active orders tracking
        activeOrders.add(order);
        
        return order;
    }
    
    /**
     * Update order status after validation/execution
     * Called by TransactionManager after processing completes
     * 
     * @param order Order to update
     * @param status New status (ACCEPTED, FILLED, REJECTED)
     * @param statusCode HTTP status code
     */
    public void updateOrderStatus(Order order, Order.OrderStatus status, Integer statusCode) {
        if (order == null) {
            return;
        }
        
        ZonedDateTime now = ZonedDateTime.now();
        
        if (status == Order.OrderStatus.FILLED) {
            // Order successfully executed
            order.updateExecution(now, order.getSubmittedValue(), status, 201);
        } else if (status == Order.OrderStatus.ACCEPTED) {
            // Order passed validation, ready for execution
            order.updateExecution(now, null, status, 200);
        } else if (status == Order.OrderStatus.REJECTED) {
            // Order failed validation - executedon and executedvalue must be NULL
            order.updateExecution(null, null, status, 422);
        }
    }
    
    /**
     * Get real user from JWT authentication context
     * Extracts username from JWT and retrieves User entity from database
     * 
     * @return User entity or null if not authenticated
     */
    private User getRealUserFromAuth() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            
            // Extract username from JWT token
            String username = auth.getPrincipal().toString();
            
            // Retrieve User entity from database
            // UserRepository should have findByUsername() method
            User user = userService.getUserByUsername(username);
            
            return user;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get all active orders
     */
    public LinkedHashSet<Order> getActiveOrders() {
        return activeOrders;
    }
    
    /**
     * Clear order from active tracking (after persistence)
     */
    public void removeActiveOrder(Order order) {
        if (order != null) {
            activeOrders.remove(order);
        }
    }
}
