package main.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OrderSummary {
    private UUID orderId;
    private String orderType;
    private String action;
    private String ticker;
    private Double quantity;
    private String status;
    private ZonedDateTime submittedOn;
    private BigDecimal executedValue;

    public OrderSummary() {
    }

    public OrderSummary(UUID orderId, String orderType, String action, String ticker,
                       Double quantity, String status, ZonedDateTime submittedOn, BigDecimal executedValue) {
        this.orderId = orderId;
        this.orderType = orderType;
        this.action = action;
        this.ticker = ticker;
        this.quantity = quantity;
        this.status = status;
        this.submittedOn = submittedOn;
        this.executedValue = executedValue;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ZonedDateTime getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(ZonedDateTime submittedOn) {
        this.submittedOn = submittedOn;
    }

    public BigDecimal getExecutedValue() {
        return executedValue;
    }

    public void setExecutedValue(BigDecimal executedValue) {
        this.executedValue = executedValue;
    }
}
