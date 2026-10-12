package domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import domain.dto.OrderStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published when an order validation completes.
 * Results in either OrderFilledEvent (if valid) or OrderRejectedEvent (if invalid).
 */
public class OrderValidatedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status")
    private OrderStatus status;

    @JsonProperty("validation_result")
    private String validationResult;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("validated_at")
    private LocalDateTime validatedAt;

    @JsonProperty("kafka_message_id")
    private String kafkaMessageId;

    // Constructors
    public OrderValidatedEvent() {
    }

    public OrderValidatedEvent(String orderId, OrderStatus status, String validationResult,
                              String rejectionReason, LocalDateTime validatedAt,
                              String kafkaMessageId) {
        this.orderId = orderId;
        this.status = status;
        this.validationResult = validationResult;
        this.rejectionReason = rejectionReason;
        this.validatedAt = validatedAt;
        this.kafkaMessageId = kafkaMessageId;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    public String getKafkaMessageId() {
        return kafkaMessageId;
    }

    public void setKafkaMessageId(String kafkaMessageId) {
        this.kafkaMessageId = kafkaMessageId;
    }

    @Override
    public String toString() {
        return "OrderValidatedEvent{" +
                "orderId='" + orderId + '\'' +
                ", status=" + status +
                ", validationResult='" + validationResult + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", validatedAt=" + validatedAt +
                ", kafkaMessageId='" + kafkaMessageId + '\'' +
                '}';
    }
}
