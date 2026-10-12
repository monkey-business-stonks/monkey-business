package domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published when an order is rejected during validation or execution.
 * Triggers order status update and cleanup.
 */
public class OrderRejectedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("rejection_stage")
    private String rejectionStage;  // VALIDATION or EXECUTION

    @JsonProperty("rejected_at")
    private LocalDateTime rejectedAt;

    @JsonProperty("kafka_message_id")
    private String kafkaMessageId;

    // Constructors
    public OrderRejectedEvent() {
    }

    public OrderRejectedEvent(String orderId, String accountId, String rejectionReason,
                             String rejectionStage, LocalDateTime rejectedAt,
                             String kafkaMessageId) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.rejectionReason = rejectionReason;
        this.rejectionStage = rejectionStage;
        this.rejectedAt = rejectedAt;
        this.kafkaMessageId = kafkaMessageId;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionStage() {
        return rejectionStage;
    }

    public void setRejectionStage(String rejectionStage) {
        this.rejectionStage = rejectionStage;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getKafkaMessageId() {
        return kafkaMessageId;
    }

    public void setKafkaMessageId(String kafkaMessageId) {
        this.kafkaMessageId = kafkaMessageId;
    }

    @Override
    public String toString() {
        return "OrderRejectedEvent{" +
                "orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", rejectionStage='" + rejectionStage + '\'' +
                ", rejectedAt=" + rejectedAt +
                ", kafkaMessageId='" + kafkaMessageId + '\'' +
                '}';
    }
}
