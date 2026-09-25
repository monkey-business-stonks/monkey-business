package main.service;

import main.*;
import main.dto.*;
import main.entity.AccountEntity;
import main.entity.OrderEntity;
import main.repository.OrderRepository;
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

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Place an order - orchestrates validation, execution, and transaction
     */
    public OrderResponse placeOrder(UUID accountId, PlaceOrderRequest request) {
        // Step 1: Validate input
        validateOrderRequest(request);

        // Step 2: Get account
        AccountEntity accountEntity = accountService.getAccountDirect(accountId);
        if (accountEntity == null) {
            throw new NoSuchElementException("Account not found with ID: " + accountId);
        }

        // Step 3: Get current market price
        BigDecimal currentPrice = marketDataService.getPrice(request.getTicker());
        
        // Step 4: Create order entity
        UUID orderId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();
        BigDecimal submittedValue = currentPrice.multiply(new BigDecimal(request.getQuantity()));

        OrderEntity orderEntity = new OrderEntity(
            orderId,
            accountEntity,
            request.getTicker().toUpperCase(),
            request.getQuantity(),
            request.getAction().toUpperCase(),
            now,
            submittedValue,
            OrderEntity.OrderStatus.PENDING,
            now
        );

        // Step 5: Create temporary Order domain model for validation
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

        // Step 6: Create temporary Account domain model for validation
        Account account = new Account(
            accountEntity.getAccountId(),
            accountEntity.getOpenedDate(),
            Account.AccountType.valueOf(accountEntity.getAccountType().toString()),
            accountEntity.getBalance(),
            accountEntity.getCashBalance(),
            new HashSet<>(),
            new HashSet<>()
        );

        // Step 7: Create dummy user for validation (in production, get from auth)
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

        // Step 8: Validate order using OrderValidator
        Boolean isValid = OrderValidator.isValidTrade(order, dummyUser, account);
        
        if (!isValid) {
            // Order rejected
            orderEntity.setStatus(OrderEntity.OrderStatus.REJECTED);
            orderRepository.save(orderEntity);
            throw new IllegalArgumentException("Order validation failed: insufficient funds or holdings");
        }

        // Step 9: Update order with execution details
        orderEntity.setStatus(OrderEntity.OrderStatus.SUCCEEDED);
        orderEntity.setExecutedOn(now);
        orderEntity.setExecutedValue(submittedValue);

        // Step 10: Update account balances (in production, use transaction manager)
        if ("BUY".equalsIgnoreCase(request.getAction())) {
            accountEntity.setCashBalance(accountEntity.getCashBalance().subtract(submittedValue));
        } else if ("SELL".equalsIgnoreCase(request.getAction())) {
            accountEntity.setCashBalance(accountEntity.getCashBalance().add(submittedValue));
        }

        // Step 11: Save to database
        accountService.saveAccount(accountEntity);
        orderEntity = orderRepository.save(orderEntity);

        return toOrderResponse(orderEntity);
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
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrder(UUID orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found with ID: " + orderId));
        return toOrderResponse(orderEntity);
    }

    /**
     * List orders for an account
     */
    public List<OrderSummary> listOrdersForAccount(UUID accountId) {
        return orderRepository.findByAccountAccountId(accountId).stream()
            .map(this::toOrderSummary)
            .toList();
    }

    /**
     * Convert OrderEntity to OrderResponse
     */
    private OrderResponse toOrderResponse(OrderEntity order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getAccount().getAccountId(),
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
     * Convert OrderEntity to OrderSummary
     */
    private OrderSummary toOrderSummary(OrderEntity order) {
        return new OrderSummary(
            order.getOrderId(),
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
    public OrderEntity getOrderDirect(UUID orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
}
