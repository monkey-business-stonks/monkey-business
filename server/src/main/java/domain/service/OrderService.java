package domain.service;

import domain.entities.*;
import domain.dto.*;
import domain.repository.OrderRepository;
import domain.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private OrderManager orderManager;

    @Autowired
    private OrderValidator orderValidator;

    @Autowired
    private OrderExecutor orderExecutor;

    @Autowired
    private OrderExecutionEngine orderExecutionEngine;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    /**
     * Place an order - ASYNC EVENT-DRIVEN APPROACH (Phase 3a)
     * 
     * NEW Flow (Asynchronous):
     * 1. OrderManager.createOrder() - Creates order with SUBMITTED status
     * 2. Save order to database
     * 3. Publish OrderSubmittedEvent to Kafka
     * 4. Return 202 ACCEPTED immediately (don't wait for validation/execution)
     * 5. Consumers process async:
     *    - OrderValidatorConsumer validates
     *    - OrderExecutorConsumer executes
     *    - OrderPersistenceConsumer updates final status
     * 
     * Benefits:
     * - API returns immediately (low latency)
     * - Validation/execution happens in parallel
     * - Better resource utilization
     * - Handles high throughput
     * 
     * Trade-off:
     * - Client gets 202 (not final status)
     * - Must poll /orders/{orderId} to get final status
     * - Need to handle distributed failures (DLQ)
     */
    @Transactional
    public OrderResponse placeOrder(UUID accountId, PlaceOrderRequest request) {
        // Step 1: Validate input
        validateOrderRequest(request);

        // Step 2: Create order with SUBMITTED status
        Order order = orderManager.createOrder(
            accountId,
            request.getOrderType().toString(),      // EQUITY, CRYPTO, FOREX
            request.getTicker(),
            request.getQuantity().doubleValue(),
            request.getAction().toString()          // BUY or SELL
        );

        // Step 3: Persist order to database
        Account account = accountService.getAccountDirect(accountId);
        if (account == null) {
            throw new NoSuchElementException("Account not found with ID: " + accountId);
        }
        order.setAccount(account);
        order.setPrevCashBalance(account.getCashBalance());
        orderRepository.save(order);

        // Step 4: Publish OrderSubmittedEvent to Kafka
        // This triggers async validation → execution → persistence pipeline
        try {
            orderEventPublisher.publishOrderSubmitted(
                order.getOrderId().toString(),
                accountId.toString(),
                "", // username - will be extracted from JWT in controller
                order.getTicker(),
                BigDecimal.valueOf(order.getQuantity()),
                order.getSubmittedValue()
            );
        } catch (Exception e) {
            // Log error but don't fail the order creation
            // Order is in database with SUBMITTED status
            // If Kafka publishing fails, order can be retried later
            org.slf4j.LoggerFactory.getLogger(this.getClass())
                .error("Failed to publish OrderSubmittedEvent for order: " + order.getOrderId(), e);
        }

        // Step 5: Return 202 ACCEPTED (processing async)
        // Client must poll /orders/{orderId} to check status
        return toOrderResponse(order, accountId);
    }

    /**
     * Validate order request
     */
    private void validateOrderRequest(PlaceOrderRequest request) {
        if (request.getTicker() == null || request.getTicker().isEmpty()) {
            throw new IllegalArgumentException("Ticker is required");
        }
        if (request.getQuantity() == null || request.getQuantity().doubleValue() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (request.getAction() == null) {
            throw new IllegalArgumentException("Action is required (BUY or SELL)");
        }
        if (request.getOrderType() == null) {
            throw new IllegalArgumentException("Order type is required (EQUITY, CRYPTO, FOREX)");
        }
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found with ID: " + orderId));
        
        UUID accountId = order.getAccount() != null ? order.getAccount().getAccID() : null;
        if (accountId == null) {
            throw new NoSuchElementException("Account not found for order: " + orderId);
        }

        return toOrderResponse(order, accountId);
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
     * List all orders across every account owned by a user.
     */
    public List<OrderResponse> listOrdersForUser(UUID userId) {
        return accountRepository.findByUserUserId(userId).stream()
            .flatMap(account -> orderRepository.findByAccountAccountId(account.getAccountId()).stream()
                .map(order -> toOrderResponse(order, account.getAccountId())))
            .toList();
    }

    /**
     * Convert Order to OrderResponse
     * Uses actual orderType from Order entity (not hardcoded)
     */
    private OrderResponse toOrderResponse(Order order, UUID accountId) {
        return new OrderResponse()
            .orderId(order.getOrderID())
            .accountId(accountId)
            .orderType(OrderType.fromValue(order.getOrderType()))
            .action(OrderAction.valueOf(order.getAction()))
            .ticker(order.getTicker())
            .quantity(new BigDecimal(order.getQuantity()))
            .submittedValue(order.getSubmittedValue())
            .cashBalanceBeforePurchase(order.getPrevCashBalance())
            .executedValue(order.getExecutedValue())
            .status(mapOrderStatus(order.getStatus()))  // Map entity status to DTO status
            .submittedOn(order.getSubmittedOn().toOffsetDateTime())
            .verifiedOn(order.getVerifiedOn() != null ? order.getVerifiedOn().toOffsetDateTime() : null)
            .settledOn(order.getSettledOn() != null ? order.getSettledOn().toOffsetDateTime() : null)
            .executedOn(order.getExecutedOn() != null ? order.getExecutedOn().toOffsetDateTime() : null)
            .createdOn(order.getCreatedOn().toOffsetDateTime());
    }

    /**
     * Convert Order to OrderSummary
     * Uses actual orderType from Order entity (not hardcoded)
     */
    private OrderSummary toOrderSummary(Order order) {
        return new OrderSummary()
            .orderId(order.getOrderID())
            .orderType(OrderType.fromValue(order.getOrderType()))
            .action(OrderAction.valueOf(order.getAction()))
            .ticker(order.getTicker())
            .quantity(new BigDecimal(order.getQuantity()))
            .cashBalanceBeforePurchase(order.getPrevCashBalance())
            .status(mapOrderStatus(order.getStatus()))  // Map entity status to DTO status
            .submittedOn(order.getSubmittedOn().toOffsetDateTime())
            .verifiedOn(order.getVerifiedOn() != null ? order.getVerifiedOn().toOffsetDateTime() : null)
            .settledOn(order.getSettledOn() != null ? order.getSettledOn().toOffsetDateTime() : null)
            .executedValue(order.getExecutedValue());
    }

    /**
     * Map entity OrderStatus to DTO OrderStatus
    * Entity: SUBMITTED, ACCEPTED, FILLED, SETTLED, REJECTED
    * API exposes the lifecycle statuses directly.
     */
    private OrderStatus mapOrderStatus(Order.OrderStatus entityStatus) {
        return entityStatus == null ? null : OrderStatus.valueOf(entityStatus.name());
    }
}
