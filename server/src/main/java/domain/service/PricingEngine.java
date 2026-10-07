package domain.service;

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
public class PricingEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingEngine.class);
    
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
    public Map<String, Object> getQuote(String ticker) {
        String url = apiUrl + "/quotes/" + ticker;
        return callFauxnanceApi(url);
    }
    
    /**
     * Get quotes for multiple symbols in one call (max 25)
     * @param symbols List of symbols (comma-separated or list)
     * @return Map of symbol to quote
     */
    public Map<String, Map<String, Object>> getQuotes(List<String> symbols) {
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
        Map<String, Map<String, Object>> result = new HashMap<>();
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
        return callFauxnanceApi(url);
    }
    
    /**
     * Check API health and market data freshness
     * @return Health status
     */
    public Map<String, ?> getHealth() {
        String url = apiUrl + "/health";
        return callFauxnanceApi(url);
    }
    
    /**
     * Get daily quota usage for the API key
     * @return Usage statistics
     */
    public Map<String, ?> getUsage() {
        String url = apiUrl + "/usage";
        return callFauxnanceApi(url);
    }
    
    /**
     * Get current market data for a ticker (returns raw Map)
     * @param ticker Stock ticker symbol
     * @return Map with current price and quote data
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
     * Get current price for a ticker
     * Extracts the price from the quote response
     * @param ticker Stock ticker symbol
     * @return BigDecimal price
     */
    public BigDecimal getPrice(String ticker) {
        try {
            Map<String, Object> quoteData = getMarketDataRaw(ticker);
            if (quoteData != null && quoteData.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) quoteData.get("data");
                if (data != null && data.containsKey("price")) {
                    Object price = data.get("price");
                    if (price instanceof Number) {
                        return new BigDecimal(price.toString());
                    }
                }
            }
            logger.warn("Could not extract price for ticker: {}", ticker);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("Error extracting price for ticker {}: {}", ticker, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Helper method to call Fauxnance API
     * @param url Full URL to API endpoint
     * @return Response as Map
     */
    private Map<String, Object> callFauxnanceApi(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    request, 
                    Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error calling Fauxnance API at {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("API call failed: " + e.getMessage(), e);
        }
    }
}
