package main.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    
    @Id
    @Column(name = "order_id", columnDefinition = "UUID")
    private UUID orderId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;
    
    @Column(name = "ticker", nullable = false)
    private String ticker;
    
    @Column(name = "quantity", nullable = false)
    private Double quantity;
    
    @Column(name = "action", nullable = false)
    private String action; // BUY or SELL
    
    @Column(name = "submitted_on", nullable = false)
    private ZonedDateTime submittedOn;
    
    @Column(name = "executed_on")
    private ZonedDateTime executedOn;
    
    @Column(name = "submitted_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal submittedValue;
    
    @Column(name = "executed_value", precision = 19, scale = 2)
    private BigDecimal executedValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;
    
    @Column(name = "created_on", nullable = false)
    private ZonedDateTime createdOn;

    public enum OrderStatus { REJECTED, PENDING, SUCCEEDED }

    // Constructors
    public OrderEntity() {}

    public OrderEntity(UUID orderId, AccountEntity account, String ticker, Double quantity,
                      String action, ZonedDateTime submittedOn, BigDecimal submittedValue,
                      OrderStatus status, ZonedDateTime createdOn) {
        this.orderId = orderId;
        this.account = account;
        this.ticker = ticker;
        this.quantity = quantity;
        this.action = action;
        this.submittedOn = submittedOn;
        this.submittedValue = submittedValue;
        this.status = status;
        this.createdOn = createdOn;
    }

    // Getters and Setters
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public ZonedDateTime getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(ZonedDateTime submittedOn) { this.submittedOn = submittedOn; }

    public ZonedDateTime getExecutedOn() { return executedOn; }
    public void setExecutedOn(ZonedDateTime executedOn) { this.executedOn = executedOn; }

    public BigDecimal getSubmittedValue() { return submittedValue; }
    public void setSubmittedValue(BigDecimal submittedValue) { this.submittedValue = submittedValue; }

    public BigDecimal getExecutedValue() { return executedValue; }
    public void setExecutedValue(BigDecimal executedValue) { this.executedValue = executedValue; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public ZonedDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(ZonedDateTime createdOn) { this.createdOn = createdOn; }
}
