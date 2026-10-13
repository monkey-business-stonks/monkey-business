package domain.service;

import domain.event.OrderSubmittedEvent;
import domain.event.OrderFilledEvent;
import domain.event.OrderRejectedEvent;
import domain.entities.Order;
import domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for publishing order events to Kafka topics.
 * Integrates with existing OrderService to emit async events.
 * 
 * Kafka topics:
 * - orders.submitted: Order created, ready for validation
 * - orders.filled: Order successfully executed
 * - orders.rejected: Order validation or execution failed
 */
@Service
public class OrderEventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    
    @Autowired
    private StreamBridge streamBridge;
    
    @Autowired
    private OrderRepository orderRepository;
    
    /**
     * Publishes order.submitted event to Kafka.
     * Called by OrderService after order creation.
     */
    public void publishOrderSubmitted(String orderId, String accountId, String username, String ticker, BigDecimal quantity, BigDecimal price) {
        try {
            // Generate idempotent message ID
            String kafkaMessageId = UUID.randomUUID().toString();
            
            // Update order with message ID
            Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            order.setKafkaMessageId(kafkaMessageId);
            orderRepository.save(order);
            
            // Publish event
            OrderSubmittedEvent event = new OrderSubmittedEvent();
            event.setOrderId(orderId);
            event.setAccountId(accountId);
            event.setUsername(username);
            event.setSymbol(ticker);
            event.setQuantity(quantity);
            event.setLimitPrice(price);
            event.setKafkaMessageId(kafkaMessageId);
            
            Message<OrderSubmittedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka-message-id", kafkaMessageId)
                .setHeader("account-id", accountId)
                .build();
            
            boolean result = streamBridge.send("publishOrderSubmitted-out-0", message);
            
            if (result) {
                log.info("Published order.submitted: orderId={}, user={}, kafkaMessageId={}", 
                         orderId, username, kafkaMessageId);
            } else {
                log.error("Failed to publish order.submitted: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("Error publishing order.submitted: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Publishes an order after validation so execution only sees validated orders.
     */
    public void publishOrderValidated(OrderSubmittedEvent event) {
        try {
            Message<OrderSubmittedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka-message-id", event.getKafkaMessageId())
                .setHeader("account-id", event.getAccountId())
                .build();

            boolean result = streamBridge.send("publishOrderValidated-out-0", message);

            if (result) {
                log.info("Published order.validated: orderId={}, kafkaMessageId={}",
                         event.getOrderId(), event.getKafkaMessageId());
            } else {
                log.error("Failed to publish order.validated: orderId={}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error publishing order.validated: orderId={}, error={}",
                      event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Publishes order.filled event to Kafka.
     * Called after successful order execution.
     */
    public void publishOrderFilled(String orderId, String accountId, String kafkaMessageId) {
        try {
            Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            // Generate kafkaMessageId if not provided
            if (kafkaMessageId == null) {
                kafkaMessageId = order.getKafkaMessageId();
                if (kafkaMessageId == null) {
                    kafkaMessageId = UUID.randomUUID().toString();
                    order.setKafkaMessageId(kafkaMessageId);
                    orderRepository.save(order);
                }
            }
            
            OrderFilledEvent event = new OrderFilledEvent();
            event.setOrderId(orderId);
            event.setAccountId(accountId);
            // Convert Double quantity to BigDecimal
            event.setExecutedQuantity(BigDecimal.valueOf(order.getQuantity() != null ? order.getQuantity() : 0));
            event.setExecutedPrice(order.getExecutedValue());
            // Convert ZonedDateTime to LocalDateTime
            if (order.getExecutedOn() != null) {
                event.setFilledAt(order.getExecutedOn().toLocalDateTime());
            }
            event.setKafkaMessageId(kafkaMessageId);
            
            Message<OrderFilledEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka-message-id", kafkaMessageId)
                .setHeader("account-id", accountId)
                .build();
            
            boolean result = streamBridge.send("publishOrderFilled-out-0", message);
            
            if (result) {
                log.info("Published order.filled: orderId={}", orderId);
            } else {
                log.error("Failed to publish order.filled: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("Error publishing order.filled: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }
    
    /**
     * Publishes order.rejected event to Kafka.
     * Called when order validation or execution fails.
     */
    public void publishOrderRejected(String orderId, String accountId, String kafkaMessageId, String reason, String stage) {
        try {
            // Generate kafkaMessageId if not provided
            if (kafkaMessageId == null) {
                try {
                    Order order = orderRepository.findById(UUID.fromString(orderId))
                        .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
                    kafkaMessageId = order.getKafkaMessageId();
                    if (kafkaMessageId == null) {
                        kafkaMessageId = UUID.randomUUID().toString();
                        order.setKafkaMessageId(kafkaMessageId);
                        orderRepository.save(order);
                    }
                } catch (Exception e) {
                    kafkaMessageId = UUID.randomUUID().toString();
                }
            }
            
            OrderRejectedEvent event = new OrderRejectedEvent();
            event.setOrderId(orderId);
            event.setAccountId(accountId);
            event.setRejectionReason(reason);
            event.setRejectionStage(stage);
            event.setKafkaMessageId(kafkaMessageId);
            
            Message<OrderRejectedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader("kafka-message-id", kafkaMessageId)
                .setHeader("account-id", accountId)
                .build();
            
            boolean result = streamBridge.send("publishOrderRejected-out-0", message);
            
            if (result) {
                log.info("Published order.rejected: orderId={}, reason={}", orderId, reason);
            } else {
                log.error("Failed to publish order.rejected: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("Error publishing order.rejected: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }
}
