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
    void testGetCandles_ReturnsEmptyList() {
        // Act
        List<?> candles = pricingEngine.getCandles("AAPL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        // Assert
        assertNotNull(candles);
        assertEquals(0, candles.size());
    }

    @Test
    void testGetQuotes_SingleSymbol() {
        // Arrange
        Map<String, Object> quoteData = new HashMap<>();
        quoteData.put("ticker", "AAPL");
        ResponseEntity<Map> response = new ResponseEntity<>(quoteData, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(),
            eq(Map.class)
        )).thenReturn(response);

        // Act
        Map<String, Map<String, Object>> result = pricingEngine.getQuotes(List.of("AAPL"));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("AAPL"));
    }

    @Test
    void testGetQuotes_ExceedsMaxLimit() {
        // Act & Assert
        List<String> tooManySymbols = new java.util.ArrayList<>();
        for (int i = 0; i < 26; i++) {
            tooManySymbols.add("SYM" + i);
        }
        assertThrows(IllegalArgumentException.class, () -> pricingEngine.getQuotes(tooManySymbols));
    }

    @Test
    void testGetQuotes_EmptyList() {
        // Act
        Map<String, Map<String, Object>> result = pricingEngine.getQuotes(List.of());

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
