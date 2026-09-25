package main;

import java.math.BigDecimal;
// import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public class Order {
    public enum OrderStatus {
        REJECTED,
        PENDING,
        SUCCEEDED
    }

    private final UUID orderID;
    private final String ticker;
    private final Double quantity;
    private final String action;
    private final ZonedDateTime submittedOn;
    private ZonedDateTime executedOn;
    private final BigDecimal submittedValue;
    private BigDecimal executedValue;
    private OrderStatus status;
    private ZonedDateTime createdOn;

    public UUID getOrderID() { return this.orderID; }
    public String getTicker() { return this.ticker; }
    public Double getQuantity() { return this.quantity; }
    public String getAction() { return this.action; }
    public ZonedDateTime getSubmittedOn() { return this.submittedOn; }
    public ZonedDateTime getExecutedOn() { return this.executedOn; }
    public BigDecimal getSubmittedValue() { return this.submittedValue; }
    public BigDecimal getExecutedValue() { return this.executedValue; }
    public OrderStatus getStatus() { return this.status; }
    public ZonedDateTime getCreatedOn() { return this.createdOn; }

    public Order(UUID orderID, String ticker, Double quantity, String action, 
                ZonedDateTime submittedOn, BigDecimal submittedValue, 
                OrderStatus status, ZonedDateTime createdOn) {
        this.orderID = orderID;
        this.ticker = ticker;
        this.quantity = quantity;
        this.action = action;
        this.submittedOn = submittedOn;
        this.submittedValue = submittedValue;
        this.status = status;
        this.createdOn = createdOn;
    }

    public void updateExecution(ZonedDateTime executedOn, BigDecimal executedValue, OrderStatus status) {
        this.executedOn = executedOn;
        this.executedValue = executedValue;
        this.status = status;
    }
}