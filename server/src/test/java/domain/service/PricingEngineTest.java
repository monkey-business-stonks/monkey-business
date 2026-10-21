package domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingEngineTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PricingEngine pricingEngine;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pricingEngine, "apiUrl", "https://api.example.com/v1");
        ReflectionTestUtils.setField(pricingEngine, "apiKey", "test-api-key");
    }

    @Test
    void testGetPrice_Success() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("price", 150.50);
        responseData.put("data", data);

        ResponseEntity<Map> response = new ResponseEntity<>(responseData, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        BigDecimal price = pricingEngine.getPrice("AAPL");

        // Assert
        assertNotNull(price);
        assertEquals(BigDecimal.valueOf(150.50), price);
    }

    @Test
    void testGetMarketDataRaw_IndiaEquity_ConvertsInrToUsd() {
        Map<String, Object> indiaData = new HashMap<>();
        indiaData.put("symbol", "INFY.NS");
        indiaData.put("price", 1500.0);
        indiaData.put("bid", 1499.0);
        indiaData.put("ask", 1501.0);
        indiaData.put("previousClose", 1480.0);
        indiaData.put("change", 20.0);
        indiaData.put("currency", "INR");

        when(restTemplate.exchange(
            contains("/quotes/INFY.NS"), eq(HttpMethod.GET), any(), eq(Map.class)
        )).thenReturn(new ResponseEntity<>(Map.of("data", indiaData), HttpStatus.OK));
        when(restTemplate.exchange(
            contains("/quotes/FX:USDINR"), eq(HttpMethod.GET), any(), eq(Map.class)
        )).thenReturn(new ResponseEntity<>(Map.of("data", Map.of("price", 83.0)), HttpStatus.OK));

        Map<String, Object> result = pricingEngine.getMarketDataRaw("INFY.NS");
        @SuppressWarnings("unchecked")
        Map<String, Object> converted = (Map<String, Object>) result.get("data");

        assertEquals("USD", converted.get("currency"));
        assertEquals(1500.0 / 83.0, ((BigDecimal) converted.get("price")).doubleValue(), 0.000001);
        assertEquals(20.0 / 83.0, ((BigDecimal) converted.get("change")).doubleValue(), 0.000001);
        verify(restTemplate, times(1)).exchange(
            contains("/quotes/FX:USDINR"), eq(HttpMethod.GET), any(), eq(Map.class));
    }

    @Test
    void testGetPrice_NullResponse() {
        // Arrange
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // Act
        BigDecimal price = pricingEngine.getPrice("AAPL");

        // Assert
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    void testGetPrice_MissingDataField() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        ResponseEntity<Map> response = new ResponseEntity<>(responseData, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        BigDecimal price = pricingEngine.getPrice("AAPL");

        // Assert
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    void testGetPrice_MissingPriceInData() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        responseData.put("data", data);
        ResponseEntity<Map> response = new ResponseEntity<>(responseData, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        BigDecimal price = pricingEngine.getPrice("AAPL");

        // Assert
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    void testGetPrice_Exception() {
        // Arrange
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenThrow(new RuntimeException("Connection failed"));

        // Act
        BigDecimal price = pricingEngine.getPrice("AAPL");

        // Assert
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    void testGetQuote_Success() {
        // Arrange
        Map<String, Object> quoteData = new HashMap<>();
        quoteData.put("ticker", "AAPL");
        quoteData.put("price", 150);
        ResponseEntity<Map> response = new ResponseEntity<>(quoteData, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, Object> quote = pricingEngine.getQuote("AAPL");

        // Assert
        assertNotNull(quote);
        assertEquals("AAPL", quote.get("ticker"));
    }

    @Test
    void testGetQuote_NotFound() {
        // Arrange
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        // Act
        Map<String, Object> quote = pricingEngine.getQuote("INVALID");

        // Assert
        assertNull(quote);
    }

    @Test
    void testGetSymbolMetadata_Success() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("symbol", "AAPL");
        metadata.put("name", "Apple Inc.");
        ResponseEntity<Map> response = new ResponseEntity<>(metadata, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, ?> result = pricingEngine.getSymbolMetadata("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.get("symbol"));
    }

    @Test
    void testGetHealth_Success() {
        // Arrange
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", "2026-10-06T00:00:00Z");
        ResponseEntity<Map> response = new ResponseEntity<>(health, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, ?> result = pricingEngine.getHealth();

        // Assert
        assertNotNull(result);
        assertEquals("healthy", result.get("status"));
    }

    @Test
    void testGetUsage_Success() {
        // Arrange
        Map<String, Object> usage = new HashMap<>();
        usage.put("quota_remaining", 1000);
        usage.put("quota_limit", 5000);
        ResponseEntity<Map> response = new ResponseEntity<>(usage, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, ?> result = pricingEngine.getUsage();

        // Assert
        assertNotNull(result);
        assertEquals(1000, result.get("quota_remaining"));
    }

    @Test
    void testGetMarketDataRaw_Success() {
        // Arrange
        Map<String, Object> marketData = new HashMap<>();
        marketData.put("symbol", "AAPL");
        marketData.put("price", 150);
        ResponseEntity<Map> response = new ResponseEntity<>(marketData, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, Object> result = pricingEngine.getMarketDataRaw("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.get("symbol"));
    }

    @Test
    void testGetMarketDataRaw_Exception() {
        // Arrange
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> pricingEngine.getMarketDataRaw("AAPL"));
    }

    @Test
    void testGetCandles_ReturnsApiResponse() {
        Map<String, Object> responseData = Map.of("data", Map.of("symbol", "AAPL"));
        when(restTemplate.exchange(
            contains("/candles/AAPL"),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(new ResponseEntity<>(responseData, HttpStatus.OK));

        Map<String, Object> candles = pricingEngine.getCandles(
            "AAPL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(responseData, candles);
    }

    @Test
    void testGetCandles_IndiaEquity_ConvertsHistoricalPricesUsingPriorFxRate() {
        Map<String, Object> firstCandle = Map.of(
            "date", "2026-01-02",
            "open", 830.0,
            "high", 996.0,
            "low", 747.0,
            "close", 913.0,
            "adjclose", 904.7,
            "volume", 1000
        );
        Map<String, Object> secondCandle = Map.of(
            "date", "2026-01-03",
            "open", 920.0,
            "high", 1012.0,
            "low", 828.0,
            "close", 1000.0,
            "adjclose", 990.0,
            "volume", 1200
        );
        Map<String, Object> stockResponse = Map.of(
            "data", new HashMap<>(Map.of(
                "symbol", "INFY.NS",
                "currency", "INR",
                "candles", List.of(firstCandle, secondCandle)
            ))
        );
        Map<String, Object> fxResponse = Map.of(
            "data", Map.of(
                "symbol", "FX:USDINR",
                "currency", "USD",
                "candles", List.of(Map.of("date", "2026-01-02", "close", 83.0))
            )
        );

        when(restTemplate.exchange(
            contains("/candles/INFY.NS"), eq(HttpMethod.GET), any(), eq(Map.class)
        )).thenReturn(new ResponseEntity<>(stockResponse, HttpStatus.OK));
        when(restTemplate.exchange(
            contains("/candles/FX:USDINR"), eq(HttpMethod.GET), any(), eq(Map.class)
        )).thenReturn(new ResponseEntity<>(fxResponse, HttpStatus.OK));

        Map<String, Object> result = pricingEngine.getCandles(
            "INFY.NS", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candles = (List<Map<String, Object>>) data.get("candles");

        assertEquals("USD", data.get("currency"));
        assertEquals(10.0, ((BigDecimal) candles.get(0).get("open")).doubleValue(), 0.000001);
        assertEquals(11.0, ((BigDecimal) candles.get(0).get("close")).doubleValue(), 0.000001);
        assertEquals(1000.0 / 83.0, ((BigDecimal) candles.get(1).get("close")).doubleValue(), 0.000001);
        assertEquals(1200, candles.get(1).get("volume"));
    }

    @Test
    void testGetPrices_SingleSymbol() {
        // Arrange
        Map<String, Object> quote = new HashMap<>();
        quote.put("symbol", "AAPL");
        quote.put("price", 150.0);
        Map<String, Object> quoteItem = Map.of("symbol", "AAPL", "quote", quote);
        Map<String, Object> responseData = Map.of("quotes", List.of(quoteItem));
        ResponseEntity<Map> response = new ResponseEntity<>(Map.of("data", responseData), HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, Map<String, Object>> result = pricingEngine.getPrices(List.of("AAPL"));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("AAPL"));
    }

    @Test
    void testGetPrices_ExceedsMaxLimit() {
        // Act & Assert
        List<String> tooManySymbols = new java.util.ArrayList<>();
        for (int i = 0; i < 26; i++) {
            tooManySymbols.add("SYM" + i);
        }
        assertThrows(IllegalArgumentException.class, () -> pricingEngine.getPrices(tooManySymbols));
    }

    @Test
    void testGetPrices_EmptyList() {
        // Act
        Map<String, Map<String, Object>> result = pricingEngine.getPrices(List.of());

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
