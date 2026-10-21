package domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PricingEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingEngine.class);
    
    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private CacheManager cacheManager;
    
    @Value("${market-data.api-url:https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1}")
    private String apiUrl;
    
    @Value("${market-data.api-key:}")
    private String apiKey;

    private static final String INDIA_EQUITY_SUFFIX = ".NS";
    private static final String USD_TO_INR_SYMBOL = "FX:USDINR";
    
    /**
     * Get current quote for a single symbol from Fauxnance API
     * @param ticker Symbol (e.g., "AAPL", "INFY.NS", "FX:EURUSD", "X:BTC-USD")
     * @return Quote with current price, bid, ask, spread
     */
    public Map<String, Object> getQuote(String ticker) {
        String url = apiUrl + "/quotes/" + ticker;
        return callFauxnanceApi(url);
    }

    private String buildCandlesUrl(String ticker, LocalDate from, LocalDate to) {
        return UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/candles/" + ticker)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("interval", "1d")
                .toUriString();
    }

    private Map<String, Object> getCachedFxCandles(LocalDate from, LocalDate to) {
        String cacheKey = USD_TO_INR_SYMBOL + ":" + from + ":" + to;
        Cache cache = cacheManager == null ? null : cacheManager.getCache("candles");
        if (cache != null) {
            Object cachedValue = cache.get(cacheKey, Object.class);
            if (cachedValue instanceof Map<?, ?> cachedMap) {
                Map<String, Object> cached = new LinkedHashMap<>();
                cachedMap.forEach((key, value) -> cached.put(key.toString(), value));
                return cached;
            }
        }

        Map<String, Object> response = callFauxnanceApi(
                buildCandlesUrl(USD_TO_INR_SYMBOL, from, to));
        if (cache != null) {
            cache.put(cacheKey, response);
        }
        return response;
    }
    
    /**
     * Get quotes for multiple symbols in one call (max 25)
     * @param symbols List of symbols (comma-separated or list)
     * @return Map of symbol to quote
     */
    public Map<String, Map<String, Object>> getPrices(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashMap<>();
        }
        
        if (symbols.size() > 25) {
            throw new IllegalArgumentException("Maximum 25 symbols per request");
        }
        
        String symbolsParam = String.join(",", symbols);
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl + "/quotes")
                .queryParam("symbols", symbolsParam)
                .toUriString();

        Map<String, Object> response = callFauxnanceApi(url);
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        BigDecimal usdToInrRate = null;
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.get("quotes") instanceof List<?> quotes) {
            for (Object item : quotes) {
                if (!(item instanceof Map<?, ?> quoteItem)) {
                    continue;
                }

                Object symbol = quoteItem.get("symbol");
                Object quote = quoteItem.get("quote");
                if (symbol != null && quote instanceof Map<?, ?> quoteMap) {
                    Map<String, Object> normalizedQuote = new HashMap<>();
                    quoteMap.forEach((key, value) -> normalizedQuote.put(key.toString(), value));
                    if (isIndiaEquity(symbol.toString())) {
                        if (usdToInrRate == null) {
                            usdToInrRate = getUsdToInrRate();
                        }
                        convertIndiaEquityQuote(normalizedQuote, usdToInrRate);
                    }
                    result.put(symbol.toString(), normalizedQuote);
                }
            }
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
    public Map<String, Object> getCandles(String ticker, LocalDate from, LocalDate to) {
        String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/candles/" + ticker)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("interval", "1d")
                .toUriString();

        Map<String, Object> response = callFauxnanceApi(url);
        if (isIndiaEquity(ticker)) {
            Map<String, Object> fxResponse = getCachedFxCandles(from, to);
            convertIndiaEquityCandles(response, fxResponse);
        }
        return response;
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
            if (isIndiaEquity(ticker)) {
                convertIndiaEquityResponse(result, getUsdToInrRate());
            }
            return result;
        } catch (Exception e) {
            logger.error("Error fetching market data for {}: {}", ticker, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch market data: " + e.getMessage(), e);
        }
    }

    /**
     * Get current price for a ticker (CACHED)
     * Extracts the price from the quote response.
     * 
     * Caching Strategy (Phase 3c):
     * - Cache key: ticker symbol
     * - TTL: 5 minutes (configured in application.yml: expireAfterWrite=5m)
     * - Max entries: 10,000
     * - Target: 90% reduction in Fauxnance API calls
     * 
     * @param ticker Stock ticker symbol
     * @return BigDecimal price (cached for 5 minutes)
     */
    @Cacheable(value = "prices", key = "#ticker")
    public BigDecimal getPrice(String ticker) {
        try {
            logger.debug("Fetching price from API (not cached) for ticker: {}", ticker);
            Map<String, Object> quoteData = getMarketDataRaw(ticker);
            if (quoteData != null && quoteData.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) quoteData.get("data");
                if (data != null && data.containsKey("price")) {
                    Object price = data.get("price");
                    if (price instanceof Number) {
                        BigDecimal result = new BigDecimal(price.toString());
                        logger.debug("Cached price for {}: {}", ticker, result);
                        return result;
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

    private boolean isIndiaEquity(String ticker) {
        return ticker != null && ticker.toUpperCase().endsWith(INDIA_EQUITY_SUFFIX);
    }

    private BigDecimal getUsdToInrRate() {
        Map<String, Object> response = callFauxnanceApi(apiUrl + "/quotes/" + USD_TO_INR_SYMBOL);
        return extractPrice(response, USD_TO_INR_SYMBOL);
    }

    private BigDecimal extractPrice(Map<String, Object> response, String symbol) {
        if (response != null && response.get("data") instanceof Map<?, ?> data
                && data.get("price") instanceof Number price) {
            return new BigDecimal(price.toString());
        }
        throw new RuntimeException("Could not extract FX conversion rate for " + symbol);
    }

    private void convertIndiaEquityResponse(Map<String, Object> response, BigDecimal usdToInrRate) {
        if (response != null && response.get("data") instanceof Map<?, ?> data) {
            @SuppressWarnings("unchecked")
            Map<String, Object> quote = (Map<String, Object>) data;
            convertIndiaEquityQuote(quote, usdToInrRate);
        }
    }

    private void convertIndiaEquityQuote(Map<String, Object> quote, BigDecimal usdToInrRate) {
        for (String field : List.of("price", "bid", "ask", "previousClose", "change")) {
            Object value = quote.get(field);
            if (value instanceof Number number) {
                quote.put(field, new BigDecimal(number.toString()).divide(
                        usdToInrRate, 10, java.math.RoundingMode.HALF_UP));
            }
        }
        quote.put("currency", "USD");
    }

    private void convertIndiaEquityCandles(Map<String, Object> stockResponse,
                                           Map<String, Object> fxResponse) {
        if (!(stockResponse != null && stockResponse.get("data") instanceof Map<?, ?> stockData)
                || !(fxResponse != null && fxResponse.get("data") instanceof Map<?, ?> fxData)) {
            throw new RuntimeException("Could not extract candle data for Indian equity conversion");
        }

        Object stockCandlesValue = stockData.get("candles");
        Object fxCandlesValue = fxData.get("candles");
        if (!(stockCandlesValue instanceof List<?> stockCandles)
                || !(fxCandlesValue instanceof List<?> fxCandles)) {
            throw new RuntimeException("Could not extract candle series for Indian equity conversion");
        }

        TreeMap<LocalDate, BigDecimal> ratesByDate = new TreeMap<>();
        for (Object item : fxCandles) {
            if (item instanceof Map<?, ?> candle) {
                LocalDate date = parseCandleDate(candle.get("date"));
                BigDecimal rate = toDecimal(candle.get("close"));
                if (date != null && rate != null && rate.signum() > 0) {
                    ratesByDate.put(date, rate);
                }
            }
        }

        List<Map<String, Object>> convertedCandles = new ArrayList<>();
        for (Object item : stockCandles) {
            if (!(item instanceof Map<?, ?> rawCandle)) {
                continue;
            }

            LocalDate date = parseCandleDate(rawCandle.get("date"));
            BigDecimal rate = date == null ? null : ratesByDate.floorEntry(date) != null
                    ? ratesByDate.floorEntry(date).getValue() : null;
            if (rate == null) {
                throw new RuntimeException("Missing USDINR rate for candle date " + date);
            }

            Map<String, Object> candle = new LinkedHashMap<>();
            rawCandle.forEach((key, value) -> candle.put(key.toString(), value));
            for (String field : List.of("open", "high", "low", "close", "adjclose")) {
                BigDecimal value = toDecimal(candle.get(field));
                if (value != null) {
                    candle.put(field, value.divide(rate, 10, java.math.RoundingMode.HALF_UP));
                }
            }
            convertedCandles.add(candle);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> mutableStockData = (Map<String, Object>) stockData;
        mutableStockData.put("candles", convertedCandles);
        mutableStockData.put("currency", "USD");
    }

    private LocalDate parseCandleDate(Object value) {
        return value instanceof String date ? LocalDate.parse(date) : null;
    }

    private BigDecimal toDecimal(Object value) {
        return value instanceof Number number ? new BigDecimal(number.toString()) : null;
    }
}
