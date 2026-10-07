package domain.service;

import domain.entities.Account;
import domain.entities.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExecutorTest {

    @Mock
    private PricingEngine pricingEngine;

    @InjectMocks
    private OrderExecutor orderExecutor;

    private Account testAccount;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(50000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );

        testOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            100.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(15000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        testOrder.setAccount(testAccount);
    }

    @Test
    void testProcessTrade_BuyOrder_Success() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        Order result = orderExecutor.processTrade(testOrder, testAccount);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.FILLED, result.getStatus());
        assertEquals(201, result.getStatusCode());
        assertNotNull(result.getExecutedOn());
        assertTrue(result.getExecutedValue().compareTo(BigDecimal.valueOf(15000)) == 0);
    }

    @Test
    void testProcessTrade_BuyOrder_InsufficientCash() {
        // Arrange
        Account lowCashAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(5000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        Order result = orderExecutor.processTrade(testOrder, lowCashAccount);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.REJECTED, result.getStatus());
        assertEquals(422, result.getStatusCode());
        assertNull(result.getExecutedOn());
        assertNull(result.getExecutedValue());
    }

    @Test
    void testProcessTrade_PriceRetrievalFailed() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.ZERO);

        // Act
        Order result = orderExecutor.processTrade(testOrder, testAccount);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.REJECTED, result.getStatus());
        assertEquals(503, result.getStatusCode());
        assertNull(result.getExecutedOn());
        assertNull(result.getExecutedValue());
    }

    @Test
    void testProcessTrade_PriceRetrievalNull() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenReturn(null);

        // Act
        Order result = orderExecutor.processTrade(testOrder, testAccount);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.REJECTED, result.getStatus());
        assertEquals(503, result.getStatusCode());
        assertNull(result.getExecutedOn());
        assertNull(result.getExecutedValue());
    }

    @Test
    void testProcessTrade_NullOrder() {
        // Act
        Order result = orderExecutor.processTrade(null, testAccount);

        // Assert
        assertNull(result);
    }

    @Test
    void testProcessTrade_NullAccount() {
        // Act
        Order result = orderExecutor.processTrade(testOrder, null);

        // Assert
        assertEquals(testOrder, result);
    }

    @Test
    void testProcessTrade_SellOrder_Success() {
        // Arrange
        Order sellOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            50.0,
            "SELL",
            ZonedDateTime.now(),
            BigDecimal.valueOf(7500),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        sellOrder.setAccount(testAccount);
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        Order result = orderExecutor.processTrade(sellOrder, testAccount);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.FILLED, result.getStatus());
        assertEquals(201, result.getStatusCode());
        assertTrue(result.getExecutedValue().compareTo(BigDecimal.valueOf(7500)) == 0);
    }

    @Test
    void testProcessTrade_ExceptionHandling() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenThrow(new RuntimeException("API Error"));

        // Act
        Order result = orderExecutor.processTrade(testOrder, testAccount);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.REJECTED, result.getStatus());
        assertEquals(500, result.getStatusCode());
        assertNull(result.getExecutedOn());
        assertNull(result.getExecutedValue());
    }

    @Test
    void testProcessTrade_DifferentPrices() {
        // Arrange
        Order largeOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "GOOG",
            10.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(25000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        largeOrder.setAccount(testAccount);
        when(pricingEngine.getPrice("GOOG")).thenReturn(BigDecimal.valueOf(2500));

        // Act
        Order result = orderExecutor.processTrade(largeOrder, testAccount);

        // Assert
        assertEquals(Order.OrderStatus.FILLED, result.getStatus());
        assertTrue(result.getExecutedValue().compareTo(BigDecimal.valueOf(25000)) == 0);
    }
}
