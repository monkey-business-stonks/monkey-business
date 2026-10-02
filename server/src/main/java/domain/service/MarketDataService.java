package domain.service;

import domain.dto.MarketDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${market-data.api-url:https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1}")
    private String apiUrl;
    
    @Value("${market-data.api-key:}")
    private String apiKey;
    
    /**
     * Get current quote for a single symbol from Fauxnance API
     * @param ticker Symbol (e.g., "AAPL", "INFY.NS", "FX:EURUSD", "X:BTC-USD")
     * @return Quote with current price, bid, ask, spread
     */
    public MarketDataDto getQuote(String ticker) {
        String url = apiUrl + "/quotes/" + ticker;
        return callFauxnanceApi(url, MarketDataDto.class);
    }
    
    /**
     * Get quotes for multiple symbols in one call (max 25)
     * @param symbols List of symbols (comma-separated or list)
     * @return Map of symbol to quote
     */
    public Map<String, MarketDataDto> getQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashMap<>();
        }
        
        if (symbols.size() > 25) {
            throw new IllegalArgumentException("Maximum 25 symbols per request");
        }
        
        String symbolsParam = String.join(",", symbols);
        UriComponentsBuilder.fromHttpUrl(apiUrl + "/quotes")
                .queryParam("symbols", symbolsParam)
                .toUriString();
        
        // TODO: Parse batch response and return map
        // For now, return individual quotes - service team will implement batch parsing
        Map<String, MarketDataDto> result = new HashMap<>();
        for (String symbol : symbols) {
            result.put(symbol, getQuote(symbol));
        }
        return result;
    }
    
    /**
     * Get historical candles (OHLCV data) for a symbol
     * @param ticker Symbol
     * @param from Start date (inclusive)
     * @param to End date (inclusive)
     * @return List of candles with open, high, low, close, adjclose, volume
     */
    public List<?> getCandles(String ticker, LocalDate from, LocalDate to) {
        UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/candles/" + ticker)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("interval", "1d")
                .toUriString();
        
        // TODO: Parse candles response and return list
        // For now, return empty list - service team will implement parsing
        return List.of();
    }
    
    /**
     * Get symbol metadata and EOD coverage
     * @param ticker Symbol
     * @return Symbol registry entry
     */
    public Map<String, ?> getSymbolMetadata(String ticker) {
        String url = apiUrl + "/symbols/" + ticker;
        return callFauxnanceApi(url, Map.class);
    }
    
    /**
     * Check API health and market data freshness
     * @return Health status
     */
    public Map<String, ?> getHealth() {
        String url = apiUrl + "/health";
        return callFauxnanceApi(url, Map.class);
    }
    
    /**
     * Get daily quota usage for the API key
     * @return Usage statistics
     */
    public Map<String, ?> getUsage() {
        String url = apiUrl + "/usage";
        return callFauxnanceApi(url, Map.class);
    }
    
    /**
     * Get current market data for a ticker (returns raw Map)
     * @param ticker Stock ticker symbol
     * @return Map with current price
     */
    public Map<String, Object> getMarketDataRaw(String ticker) {
        try {
            logger.info("Fetching market data for ticker: {}", ticker);
            String url = apiUrl + "/quotes/" + ticker;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            logger.info("Calling Fauxnance API: {}", url);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    request, 
                    Map.class
            );
            
            Map<String, Object> result = response.getBody();
            logger.info("API Response: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error fetching market data for {}: {}", ticker, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch market data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current market data for a ticker (for backwards compatibility)
     * @param ticker Stock ticker symbol
     * @return MarketDataDto with current price
     */
    public MarketDataDto getMarketData(String ticker) {
        try {
            MarketDataDto data = getQuote(ticker);
            if (data == null) {
                logger.warn("No market data returned for ticker: {}", ticker);
                return null;
            }
            return data;
        } catch (Exception e) {
            logger.error("Failed to get market data for {}: {}", ticker, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get current price for a ticker
     * @param ticker Stock ticker symbol
     * @return BigDecimal price
     */
    public BigDecimal getPrice(String ticker) {
        MarketDataDto data = getMarketData(ticker);
        return data != null && data.getPrice() != null ? data.getPrice() : BigDecimal.ZERO;
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Generic HTTP call to Fauxnance API with auth header
     */
    private <T> T callFauxnanceApi(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<?> request = new HttpEntity<>(headers);
        
        try {
            logger.info("Calling Fauxnance API: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    request, 
                    responseType
            );
            
            logger.info("API Response status: {}", response.getStatusCode());
            logger.info("API Response body: {}", response.getBody());
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error calling Fauxnance API: {}", url, e);
            throw e;
        }
    }
}
