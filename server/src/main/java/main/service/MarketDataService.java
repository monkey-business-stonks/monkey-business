package main.service;

import main.dto.MarketDataDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MarketDataService {

    // Mock price data for demonstration
    // In production, this would fetch from real market data providers
    private static final Map<String, BigDecimal> MOCK_PRICES = new HashMap<>();

    static {
        MOCK_PRICES.put("AAPL", new BigDecimal("228.50"));
        MOCK_PRICES.put("GOOGL", new BigDecimal("165.75"));
        MOCK_PRICES.put("MSFT", new BigDecimal("380.25"));
        MOCK_PRICES.put("AMZN", new BigDecimal("185.40"));
        MOCK_PRICES.put("TSLA", new BigDecimal("245.30"));
        MOCK_PRICES.put("BTC", new BigDecimal("42150.00"));
        MOCK_PRICES.put("ETH", new BigDecimal("2350.75"));
    }

    /**
     * Get current market data for a ticker
     * @param ticker Stock ticker symbol
     * @return MarketDataDto with current price
     */
    public MarketDataDto getMarketData(String ticker) {
        BigDecimal price = MOCK_PRICES.getOrDefault(ticker.toUpperCase(), new BigDecimal("100.00"));
        
        return new MarketDataDto()
            .ticker(ticker.toUpperCase())
            .price(price.doubleValue())
            .timestamp(java.time.OffsetDateTime.now())
            .currency("USD");
    }

    /**
     * Get current price for a ticker
     * @param ticker Stock ticker symbol
     * @return BigDecimal price
     */
    public BigDecimal getPrice(String ticker) {
        return MOCK_PRICES.getOrDefault(ticker.toUpperCase(), new BigDecimal("100.00"));
    }
}
