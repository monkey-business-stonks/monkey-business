package main.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketDataDto {
    private String ticker;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private String currency;

    public MarketDataDto() {
    }

    public MarketDataDto(String ticker, BigDecimal price, LocalDateTime timestamp, String currency) {
        this.ticker = ticker;
        this.price = price;
        this.timestamp = timestamp;
        this.currency = currency;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "MarketDataDto{" +
                "ticker='" + ticker + '\'' +
                ", price=" + price +
                ", timestamp=" + timestamp +
                ", currency='" + currency + '\'' +
                '}';
    }
}
