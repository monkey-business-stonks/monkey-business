package domain.entities;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import domain.error.OrderException;

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
    @Column(name = "orderid")
    private UUID orderId;
    
    @Column(name = "accountid", insertable = false, updatable = false)
    private UUID accountId;
    
    @Column(name = "ordertype")
    private String orderType;
    
    @Column(name = "ticker")
    private String ticker;
    
    @Column(name = "quantity")
    private Double quantity;
    
    @Column(name = "action")
    private String action;
    
    @Column(name = "submittedon")
    private ZonedDateTime submittedOn;
    
    @Column(name = "executedon")
    private ZonedDateTime executedOn;
    
    @Column(name = "submittedvalue")
    private BigDecimal submittedValue;
    
    @Column(name = "executedvalue")
    private BigDecimal executedValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;
    
    @Column(name = "statuscode")
    private Integer statusCode;
    
    @Column(name = "createdat")
    private ZonedDateTime createdAt;

    @ManyToOne
	@JoinColumn(name = "accountid")
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
        // Validate required fields
        if (orderId == null) {
            throw OrderException.nullOrderId();
        }
        if (ticker == null || ticker.isEmpty()) {
            throw OrderException.nullTicker();
        }
        if (quantity == null || quantity <= 0) {
            throw OrderException.invalidQuantity(quantity != null ? quantity : 0);
        }
        if (action == null) {
            throw OrderException.nullAction();
        }
        if (!action.equals("BUY") && !action.equals("SELL")) {
            throw OrderException.invalidAction(action);
        }
        if (orderType == null) {
            throw OrderException.nullOrderType();
        }
        if (submittedOn == null) {
            throw OrderException.nullSubmittedOn();
        }
        if (submittedValue != null && submittedValue.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.negativeSubmittedValue(submittedValue.doubleValue());
        }
        if (status == null) {
            throw new OrderException("Order status cannot be null", "ORDER_NULL_STATUS");
        }
        
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
        if (status == null) {
            throw new OrderException("Order status cannot be null", "ORDER_NULL_STATUS");
        }
        if (executedValue != null && executedValue.compareTo(BigDecimal.ZERO) < 0) {
            throw OrderException.negativeSubmittedValue(executedValue.doubleValue());
        }
        
        this.executedOn = executedOn;
        this.executedValue = executedValue;
        this.status = status;
        this.statusCode = statusCode;
    }
}
