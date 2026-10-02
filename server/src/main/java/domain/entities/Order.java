package domain.entities;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    public enum OrderStatus {
        SUBMITTED,
        ACCEPTED,
        FILLED,
        REJECTED
    }

    @Id
    private UUID orderId;
    private String orderType;
    private String ticker;
    private Double quantity;
    private String action;
    private ZonedDateTime submittedOn;
    private ZonedDateTime executedOn;
    private BigDecimal submittedValue;
    private BigDecimal executedValue;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Integer statusCode;
    private ZonedDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // No-arg constructor for JPA
    protected Order() {
        this.orderId = null;
        this.orderType = null;
        this.ticker = null;
        this.quantity = null;
        this.action = null;
        this.submittedOn = null;
        this.executedOn = null;
        this.submittedValue = null;
        this.executedValue = null;
        this.status = null;
        this.statusCode = null;
        this.createdAt = null;
    }

    public UUID getOrderId() { return this.orderId; }
    public String getOrderType() { return this.orderType; }
    public String getTicker() { return this.ticker; }
    public Double getQuantity() { return this.quantity; }
    public String getAction() { return this.action; }
    public ZonedDateTime getSubmittedOn() { return this.submittedOn; }
    public ZonedDateTime getExecutedOn() { return this.executedOn; }
    public BigDecimal getSubmittedValue() { return this.submittedValue; }
    public BigDecimal getExecutedValue() { return this.executedValue; }
    public OrderStatus getStatus() { return this.status; }
    public Integer getStatusCode() { return this.statusCode; }
    public ZonedDateTime getCreatedAt() { return this.createdAt; }
    public Account getAccount() { return this.account; }
    public void setAccount(Account account) { this.account = account; }
    public UUID getOrderID() { return this.orderId; }
    public ZonedDateTime getCreatedOn() { return this.createdAt; }

    public Order(UUID orderId, String orderType, String ticker, Double quantity, String action, 
                ZonedDateTime submittedOn, BigDecimal submittedValue, 
                OrderStatus status, Integer statusCode, ZonedDateTime createdAt) {
        this.orderId = orderId;
        this.orderType = orderType;
        this.ticker = ticker;
        this.quantity = quantity;
        this.action = action;
        this.submittedOn = submittedOn;
        this.executedOn = null;
        this.submittedValue = submittedValue;
        this.executedValue = null;
        this.status = status;
        this.statusCode = statusCode;
        this.createdAt = createdAt;
    }

    public void updateExecution(ZonedDateTime executedOn, BigDecimal executedValue, OrderStatus status, Integer statusCode) {
        this.executedOn = executedOn;
        this.executedValue = executedValue;
        this.status = status;
        this.statusCode = statusCode;
    }
}
