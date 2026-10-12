package domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import domain.dto.OrderAction;
import domain.dto.OrderType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when an order is submitted via REST API.
 * Triggers the order validation process asynchronously via Kafka.
 */
public class OrderSubmittedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("order_type")
    private OrderType orderType;

    @JsonProperty("order_action")
    private OrderAction orderAction;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("limit_price")
    private BigDecimal limitPrice;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("kafka_message_id")
    private String kafkaMessageId;

    // Constructors
    public OrderSubmittedEvent() {
    }

    public OrderSubmittedEvent(String orderId, String accountId, String userId, String username,
                              OrderType orderType, OrderAction orderAction, String symbol,
                              BigDecimal quantity, BigDecimal limitPrice, LocalDateTime timestamp,
                              String kafkaMessageId) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.userId = userId;
        this.username = username;
        this.orderType = orderType;
        this.orderAction = orderAction;
        this.symbol = symbol;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.timestamp = timestamp;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderAction getOrderAction() {
        return orderAction;
    }

    public void setOrderAction(OrderAction orderAction) {
        this.orderAction = orderAction;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getKafkaMessageId() {
        return kafkaMessageId;
    }

    public void setKafkaMessageId(String kafkaMessageId) {
        this.kafkaMessageId = kafkaMessageId;
    }

    @Override
    public String toString() {
        return "OrderSubmittedEvent{" +
                "orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", username='" + username + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", limitPrice=" + limitPrice +
                ", timestamp=" + timestamp +
                ", kafkaMessageId='" + kafkaMessageId + '\'' +
                '}';
    }
}
