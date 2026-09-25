package main.service;

import main.*;
import main.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private MarketDataService marketDataService;

    // In-memory storage for orders
    private final Map<UUID, Order> orderDatabase = new HashMap<>();
    private final Map<UUID, List<UUID>> accountOrderIndex = new HashMap<>();

    /**
     * Place an order - orchestrates validation, execution, and transaction
     */
    public OrderResponse placeOrder(UUID accountId, PlaceOrderRequest request) {
        // Step 1: Validate input
        validateOrderRequest(request);

        // Step 2: Get account
        Account account = accountService.getAccountDirect(accountId);
        if (account == null) {
            throw new NoSuchElementException("Account not found with ID: " + accountId);
        }

        // Step 3: Get current market price
        BigDecimal currentPrice = marketDataService.getPrice(request.getTicker());
        
        // Step 4: Create order object
        UUID orderId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();
        BigDecimal submittedValue = currentPrice.multiply(new BigDecimal(request.getQuantity()));

        Order order = new Order(
            orderId,
            request.getTicker().toUpperCase(),
            request.getQuantity(),
            request.getAction().toUpperCase(),
            now,
            submittedValue,
            Order.OrderStatus.PENDING,
            now
        );

        // Step 5: Create dummy user for validation (in production, get from auth)
        User dummyUser = new User(
            UUID.randomUUID(),
            "user",
            "user@test.com",
            "password",
            User.AccessLevel.User,
            true,
            new HashSet<>(),
            now.toLocalDateTime()
        );

        // Step 6: Validate order using OrderValidator
        Boolean isValid = OrderValidator.isValidTrade(order, dummyUser, account);
        
        if (!isValid) {
            // Order rejected
            order.updateExecution(now, null, Order.OrderStatus.REJECTED);
            orderDatabase.put(orderId, order);
            accountOrderIndex.computeIfAbsent(accountId, k -> new ArrayList<>()).add(orderId);
            
            throw new IllegalArgumentException("Order validation failed: insufficient funds or holdings");
        }

        // Step 7: Update order with execution price
        order.updateExecution(now, submittedValue, Order.OrderStatus.PENDING);

        // Step 8: Use TransactionManager to atomically update account and order status
        TransactionManager transactionManager = new TransactionManager(account, order);
        transactionManager.updateAccount();  // Updates balance and assets
        transactionManager.updateStatus();   // Marks order as SUCCEEDED

        // Step 9: Store order
        orderDatabase.put(orderId, order);
        accountOrderIndex.computeIfAbsent(accountId, k -> new ArrayList<>()).add(orderId);

        return toOrderResponse(order, accountId);
    }

    /**
     * Validate order request
     */
    private void validateOrderRequest(PlaceOrderRequest request) {
        if (request.getTicker() == null || request.getTicker().isEmpty()) {
            throw new IllegalArgumentException("Ticker is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (request.getAction() == null || request.getAction().isEmpty()) {
            throw new IllegalArgumentException("Action is required (BUY or SELL)");
        }
        if (request.getOrderType() == null || request.getOrderType().isEmpty()) {
            throw new IllegalArgumentException("Order type is required");
        }
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderDatabase.get(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }
        
        // Find account ID
        UUID accountId = accountOrderIndex.entrySet().stream()
            .filter(e -> e.getValue().contains(orderId))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Account not found for order: " + orderId));

        return toOrderResponse(order, accountId);
    }

    /**
     * List orders for an account
     */
    public List<OrderSummary> listOrdersForAccount(UUID accountId) {
        List<UUID> orderIds = accountOrderIndex.getOrDefault(accountId, new ArrayList<>());
        return orderIds.stream()
            .map(orderDatabase::get)
            .map(this::toOrderSummary)
            .toList();
    }

    /**
     * Convert Order to OrderResponse
     */
    private OrderResponse toOrderResponse(Order order, UUID accountId) {
        return new OrderResponse(
            order.getOrderID(),
            accountId,
            "EQUITY",  // Mock - in production, would be stored in Order
            order.getAction(),
            order.getTicker(),
            order.getQuantity(),
            order.getSubmittedValue(),
            order.getExecutedValue(),
            order.getStatus().toString(),
            order.getSubmittedOn(),
            order.getExecutedOn(),
            order.getCreatedOn()
        );
    }

    /**
     * Convert Order to OrderSummary
     */
    private OrderSummary toOrderSummary(Order order) {
        return new OrderSummary(
            order.getOrderID(),
            "EQUITY",  // Mock
            order.getAction(),
            order.getTicker(),
            order.getQuantity(),
            order.getStatus().toString(),
            order.getSubmittedOn(),
            order.getExecutedValue()
        );
    }

    /**
     * Get stored order (internal use)
     */
    public Order getOrderDirect(UUID orderId) {
        return orderDatabase.get(orderId);
    }
}
