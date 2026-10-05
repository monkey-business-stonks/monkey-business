package domain.service;

import domain.entities.*;
import domain.dto.*;
import domain.repository.OrderRepository;
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
    private OrderExecutionEngine transactionManager;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Place an order - PRIMARY ENTRY POINT
     * Orchestrates: Create → Validate → Execute → Transaction
     * 
     * Flow:
     * 1. OrderManager.createOrder() - Creates order with SUBMITTED status
     * 2. OrderValidator.isValidTrade() - Checks if trade is allowed
     * 3. If fails: Save rejected order, return with REJECTED status (422)
     * 4. If passes: OrderExecutor.processTrade() - Executes trade, sets FILLED status
     * 5. TransactionManager.updateAccount() - Updates holdings and balance
     * 6. TransactionManager.updateStatus() - Sets order to FILLED (201)
     * 7. Persist account and order
     * 8. Return response with actual orderType
     */
    @Transactional
    public OrderResponse placeOrder(UUID accountId, PlaceOrderRequest request) {
        // Step 1: Validate input
        validateOrderRequest(request);

        // Step 2: ORCHESTRATOR CALL: Create order with real user/account
        // OrderManager extracts user from JWT context, validates ownership, sets status=SUBMITTED
        Order order = orderManager.createOrder(
            accountId,
            request.getOrderType().toString(),      // Convert enum to String: EQUITY, CRYPTO, or FOREX
            request.getTicker(),
            request.getQuantity().doubleValue(),
            request.getAction().toString()          // Convert enum to String: BUY or SELL
        );

        // Step 3: Get account (already verified by OrderManager, but get fresh copy)
        Account account = accountService.getAccountDirect(accountId);
        if (account == null) {
            throw new NoSuchElementException("Account not found with ID: " + accountId);
        }

        // Step 4: VALIDATION: Check if trade is allowed
        boolean isValidTrade = orderValidator.isValidTrade(order, account);
        
        if (!isValidTrade) {
            // Trade rejected - mark order as REJECTED with statusCode 422
            order.updateExecution(
                null,  // executedon must be NULL for rejected orders
                null,  // executedvalue must be NULL for rejected orders
                Order.OrderStatus.REJECTED,
                422  // Unprocessable Entity
            );
            order.setAccount(account);
            orderRepository.save(order);
            orderManager.removeActiveOrder(order);
            
            return toOrderResponse(order, accountId);
        }

        // Step 5: EXECUTION: Process trade with current market price
        // OrderExecutor gets current price, verifies affordability, sets status=FILLED
        order = orderExecutor.processTrade(order, account);
        
        // If execution failed (price retrieval error, etc.), order status is already REJECTED
        if (order.getStatus() == Order.OrderStatus.REJECTED) {
            order.setAccount(account);
            orderRepository.save(order);
            orderManager.removeActiveOrder(order);
            
            return toOrderResponse(order, accountId);
        }

        // Step 6: TRANSACTION: Update account holdings, cash, and total balance
        // TransactionManager:
        // - Calls AssetService to update holdings (BUY/SELL)
        // - Deducts/adds cash
        // - Calls PricingEngine for each asset to calculate total balance
        // - Links order to account
        transactionManager.updateAccount(account, order);

        // Step 7: PERSIST: Order is already persisted by updateAccount, now update status
        transactionManager.updateStatus(order);
        order.setAccount(account);
        orderRepository.save(order);
        
        // Remove from active orders (successfully processed)
        orderManager.removeActiveOrder(order);

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
     * Convert Order to OrderResponse
     * Uses actual orderType from Order entity (not hardcoded)
     */
    private OrderResponse toOrderResponse(Order order, UUID accountId) {
        return new OrderResponse()
            .orderId(order.getOrderID())
            .accountId(accountId)
            .orderType(OrderType.valueOf(order.getOrderType()))  // Actual type from order
            .action(OrderAction.valueOf(order.getAction()))
            .ticker(order.getTicker())
            .quantity(new BigDecimal(order.getQuantity()))
            .submittedValue(order.getSubmittedValue())
            .executedValue(order.getExecutedValue())
            .status(mapOrderStatus(order.getStatus()))  // Map entity status to DTO status
            .submittedOn(order.getSubmittedOn().toOffsetDateTime())
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
            .orderType(OrderType.valueOf(order.getOrderType()))  // Actual type from order
            .action(OrderAction.valueOf(order.getAction()))
            .ticker(order.getTicker())
            .quantity(new BigDecimal(order.getQuantity()))
            .status(mapOrderStatus(order.getStatus()))  // Map entity status to DTO status
            .submittedOn(order.getSubmittedOn().toOffsetDateTime())
            .executedValue(order.getExecutedValue());
    }

    /**
     * Map entity OrderStatus to DTO OrderStatus
     * Entity: SUBMITTED, ACCEPTED, FILLED, REJECTED
     * DTO:    PENDING, SUCCEEDED, REJECTED
     */
    private OrderStatus mapOrderStatus(Order.OrderStatus entityStatus) {
        if (entityStatus == null) {
            return OrderStatus.PENDING;
        }
        switch (entityStatus) {
            case SUBMITTED:
            case ACCEPTED:
                return OrderStatus.PENDING;
            case FILLED:
                return OrderStatus.SUCCEEDED;
            case REJECTED:
                return OrderStatus.REJECTED;
            default:
                return OrderStatus.PENDING;
        }
    }
}
