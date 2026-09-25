package main.dto;

import java.math.BigDecimal;

public class PlaceOrderRequest {
    private String orderType;
    private String action;
    private String ticker;
    private Double quantity;

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(String orderType, String action, String ticker, Double quantity) {
        this.orderType = orderType;
        this.action = action;
        this.ticker = ticker;
        this.quantity = quantity;
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
}
