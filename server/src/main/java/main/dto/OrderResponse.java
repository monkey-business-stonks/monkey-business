package main.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OrderResponse {
    private UUID orderId;
    private UUID accountId;
    private String orderType;
    private String action;
    private String ticker;
    private Double quantity;
    private BigDecimal submittedValue;
    private BigDecimal executedValue;
    private String status;
    private ZonedDateTime submittedOn;
    private ZonedDateTime executedOn;
    private ZonedDateTime createdOn;

    public OrderResponse() {
    }

    public OrderResponse(UUID orderId, UUID accountId, String orderType, String action, String ticker,
                        Double quantity, BigDecimal submittedValue, BigDecimal executedValue, String status,
                        ZonedDateTime submittedOn, ZonedDateTime executedOn, ZonedDateTime createdOn) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.orderType = orderType;
        this.action = action;
        this.ticker = ticker;
        this.quantity = quantity;
        this.submittedValue = submittedValue;
        this.executedValue = executedValue;
        this.status = status;
        this.submittedOn = submittedOn;
        this.executedOn = executedOn;
        this.createdOn = createdOn;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
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

    public BigDecimal getSubmittedValue() {
        return submittedValue;
    }

    public void setSubmittedValue(BigDecimal submittedValue) {
        this.submittedValue = submittedValue;
    }

    public BigDecimal getExecutedValue() {
        return executedValue;
    }

    public void setExecutedValue(BigDecimal executedValue) {
        this.executedValue = executedValue;
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

    public ZonedDateTime getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(ZonedDateTime executedOn) {
        this.executedOn = executedOn;
    }

    public ZonedDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(ZonedDateTime createdOn) {
        this.createdOn = createdOn;
    }
}
