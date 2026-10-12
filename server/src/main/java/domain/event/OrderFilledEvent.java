package domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when an order is successfully executed.
 * Triggers portfolio balance updates and order persistence.
 */
public class OrderFilledEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("executed_quantity")
    private BigDecimal executedQuantity;

    @JsonProperty("executed_price")
    private BigDecimal executedPrice;

    @JsonProperty("filled_at")
    private LocalDateTime filledAt;

    @JsonProperty("kafka_message_id")
    private String kafkaMessageId;

    // Constructors
    public OrderFilledEvent() {
    }

    public OrderFilledEvent(String orderId, String accountId, BigDecimal executedQuantity,
                           BigDecimal executedPrice, LocalDateTime filledAt,
                           String kafkaMessageId) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.filledAt = filledAt;
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

    public BigDecimal getExecutedQuantity() {
        return executedQuantity;
    }

    public void setExecutedQuantity(BigDecimal executedQuantity) {
        this.executedQuantity = executedQuantity;
    }

    public BigDecimal getExecutedPrice() {
        return executedPrice;
    }

    public void setExecutedPrice(BigDecimal executedPrice) {
        this.executedPrice = executedPrice;
    }

    public LocalDateTime getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(LocalDateTime filledAt) {
        this.filledAt = filledAt;
    }

    public String getKafkaMessageId() {
        return kafkaMessageId;
    }

    public void setKafkaMessageId(String kafkaMessageId) {
        this.kafkaMessageId = kafkaMessageId;
    }

    @Override
    public String toString() {
        return "OrderFilledEvent{" +
                "orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", executedQuantity=" + executedQuantity +
                ", executedPrice=" + executedPrice +
                ", filledAt=" + filledAt +
                ", kafkaMessageId='" + kafkaMessageId + '\'' +
                '}';
    }
}
