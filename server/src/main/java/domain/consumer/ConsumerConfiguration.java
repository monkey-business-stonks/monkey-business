package domain.consumer;

import domain.event.OrderSubmittedEvent;
import domain.event.OrderFilledEvent;
import domain.event.OrderRejectedEvent;
import domain.service.OrderValidator;
import domain.service.OrderExecutor;
import domain.service.OrderEventPublisher;
import domain.service.OrderExecutionEngine;
import domain.entities.Order;
import domain.entities.Account;
import domain.repository.OrderRepository;
import domain.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spring Cloud Stream Kafka Consumer Configuration
 * 
 * Defines all consumer beans for the order processing pipeline:
 * 1. orderValidatorConsumer - validates orders from orders.submitted topic
 * 2. orderExecutorConsumer - executes orders from orders.submitted topic
 * 3. orderFilledEventConsumer - monitors filled orders from orders.filled topic
 * 4. orderRejectedEventConsumer - monitors rejected orders from orders.rejected topic
 */
@Configuration
public class ConsumerConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(ConsumerConfiguration.class);
    
    @Autowired
    private OrderValidator orderValidator;
    
    @Autowired
    private OrderExecutor orderExecutor;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private OrderExecutionEngine orderExecutionEngine;
    
    /**
     * Consumer for order validation.
     * Listens to: orders.submitted
     * Uses existing OrderValidator service to validate orders.
     * Publishes: orders.rejected (if invalid)
     * 
     * Max-attempts: 3 with exponential backoff (configured in application.yml)
     * On failure: Message sent to DLQ (orders.dlq)
     */
    @Bean
    public Consumer<OrderSubmittedEvent> orderValidatorConsumer() {
        return submittedEvent -> {
            String orderId = submittedEvent.getOrderId();
            String kafkaMessageId = submittedEvent.getKafkaMessageId();
            
            try {
                log.info("Validating order: orderId={}, user={}, kafkaMessageId={}", 
                         orderId, submittedEvent.getUsername(), kafkaMessageId);
                
                // Fetch order and account from database
                Order order = orderRepository.findById(UUID.fromString(orderId))
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
                
                Account account = accountRepository.findById(UUID.fromString(submittedEvent.getAccountId()))
                    .orElseThrow(() -> new RuntimeException("Account not found: " + submittedEvent.getAccountId()));
                
                // Validate trade
                boolean isValidTrade = orderValidator.isValidTrade(order, account);
                
                if (!isValidTrade) {
                    log.warn("Order validation failed: orderId={}, user={}", orderId, submittedEvent.getUsername());
                    orderEventPublisher.publishOrderRejected(
                        orderId, 
                        submittedEvent.getAccountId(), 
                        kafkaMessageId,
                        "Validation failed", 
                        "VALIDATION"
                    );
                } else {
                    log.info("Order validation passed: orderId={}, user={}", orderId, submittedEvent.getUsername());
                    // Executor consumer will take over from here
                }
            } catch (Exception e) {
                log.error("Error validating order: orderId={}, error={}", orderId, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }
    
    /**
     * Consumer for order execution.
     * Listens to: orders.submitted (after validation passes)
     * Uses existing OrderExecutor service to execute orders.
     * Publishes: orders.filled (if success) or orders.rejected (if failure)
     * 
     * Max-attempts: 3 with exponential backoff (configured in application.yml)
     * On failure: Message sent to DLQ (orders.dlq)
     */
    @Bean
    public Consumer<OrderSubmittedEvent> orderExecutorConsumer() {
        return submittedEvent -> {
            String orderId = submittedEvent.getOrderId();
            String kafkaMessageId = submittedEvent.getKafkaMessageId();
            
            try {
                log.info("Executing order: orderId={}, user={}, kafkaMessageId={}", 
                         orderId, submittedEvent.getUsername(), kafkaMessageId);
                
                // Fetch order and account from database
                Order order = orderRepository.findById(UUID.fromString(orderId))
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

                // Idempotency guard for retries/replays
                if (order.getStatus() == Order.OrderStatus.FILLED || order.getStatus() == Order.OrderStatus.REJECTED) {
                    log.info("Skipping already-terminal order: orderId={}, status={}", orderId, order.getStatus());
                    return;
                }
                
                Account account = accountRepository.findById(UUID.fromString(submittedEvent.getAccountId()))
                    .orElseThrow(() -> new RuntimeException("Account not found: " + submittedEvent.getAccountId()));
                
                // Execute trade
                order = orderExecutor.processTrade(order, account);
                
                if (order.getStatus() == Order.OrderStatus.FILLED) {
                    // Persist account and asset changes first, then persist terminal order status
                    orderExecutionEngine.updateAccount(account, order);
                    orderRepository.save(order);

                    log.info("Order executed successfully: orderId={}, user={}", orderId, submittedEvent.getUsername());
                    orderEventPublisher.publishOrderFilled(
                        orderId,
                        submittedEvent.getAccountId(),
                        kafkaMessageId
                    );
                } else if (order.getStatus() == Order.OrderStatus.REJECTED) {
                    // Persist terminal rejected status for polling clients
                    orderRepository.save(order);

                    log.warn("Order execution rejected: orderId={}, user={}, code={}", 
                             orderId, submittedEvent.getUsername(), order.getStatusCode());
                    orderEventPublisher.publishOrderRejected(
                        orderId,
                        submittedEvent.getAccountId(),
                        kafkaMessageId,
                        "Execution failed with code: " + order.getStatusCode(),
                        "EXECUTION"
                    );
                }
            } catch (Exception e) {
                log.error("Error executing order: orderId={}, error={}", orderId, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }
    
    /**
     * Consumer for monitoring filled orders.
     * Listens to: orders.filled
     * Purpose: Log events and trigger any post-execution actions (monitoring, notifications, etc.)
     */
    @Bean
    public Consumer<OrderFilledEvent> orderFilledEventConsumer() {
        return filledEvent -> {
            try {
                log.info("Order filled event received: orderId={}, accountId={}, executedPrice={}", 
                         filledEvent.getOrderId(), 
                         filledEvent.getAccountId(), 
                         filledEvent.getExecutedPrice());
                
                // TODO: Trigger notifications, webhooks, audit logging, etc.
                // For now, just logging the event
            } catch (Exception e) {
                log.error("Error processing filled event: orderId={}, error={}", 
                         filledEvent.getOrderId(), e.getMessage(), e);
            }
        };
    }
    
    /**
     * Consumer for monitoring rejected orders.
     * Listens to: orders.rejected
     * Purpose: Log events and trigger any post-rejection actions (monitoring, notifications, etc.)
     */
    @Bean
    public Consumer<OrderRejectedEvent> orderRejectedEventConsumer() {
        return rejectedEvent -> {
            try {
                log.info("Order rejected event received: orderId={}, accountId={}, reason={}", 
                         rejectedEvent.getOrderId(), 
                         rejectedEvent.getAccountId(), 
                         rejectedEvent.getRejectionReason());
                
                // TODO: Trigger notifications, webhooks, audit logging, etc.
                // For now, just logging the event
            } catch (Exception e) {
                log.error("Error processing rejected event: orderId={}, error={}", 
                         rejectedEvent.getOrderId(), e.getMessage(), e);
            }
        };
    }
}
