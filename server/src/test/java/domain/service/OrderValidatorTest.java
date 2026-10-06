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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @Mock
    private PricingEngine pricingEngine;

    @InjectMocks
    private OrderValidator orderValidator;

    private Account brokerageAccount;
    private Account cryptoAccount;
    private Account forexAccount;
    private Account retirementAccount;
    private Order validBuyOrder;

    @BeforeEach
    void setUp() {
        brokerageAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(50000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );

        cryptoAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.CRYPTO,
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(10000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );

        forexAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.FOREX,
            BigDecimal.valueOf(20000),
            BigDecimal.valueOf(20000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );

        retirementAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType._401K,
            BigDecimal.valueOf(30000),
            BigDecimal.valueOf(30000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );

        validBuyOrder = new Order(
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
    }

    @Test
    void testIsValidTrade_BuyOrder_SufficientCash() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        boolean isValid = orderValidator.isValidTrade(validBuyOrder, brokerageAccount);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsValidTrade_BuyOrder_InsufficientCash() {
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
        boolean isValid = orderValidator.isValidTrade(validBuyOrder, lowCashAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_SellOrder() {
        // Arrange
        Order sellOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            100.0,
            "SELL",
            ZonedDateTime.now(),
            BigDecimal.valueOf(15000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );

        // Act
        boolean isValid = orderValidator.isValidTrade(sellOrder, brokerageAccount);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsValidTrade_NullOrder() {
        // Act
        boolean isValid = orderValidator.isValidTrade(null, brokerageAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_NullAccount() {
        // Act
        boolean isValid = orderValidator.isValidTrade(validBuyOrder, null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_InvalidTicker() {
        // Arrange
        Order orderNoTicker = new Order(
            UUID.randomUUID(),
            "EQUITY",
            null,
            100.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(15000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );

        // Act
        boolean isValid = orderValidator.isValidTrade(orderNoTicker, brokerageAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_InvalidQuantity() {
        // Arrange
        Order orderZeroQuantity = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            0.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(0),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );

        // Act
        boolean isValid = orderValidator.isValidTrade(orderZeroQuantity, brokerageAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_InvalidAction() {
        // Arrange
        Order orderBadAction = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            100.0,
            "INVALID",
            ZonedDateTime.now(),
            BigDecimal.valueOf(15000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );

        // Act
        boolean isValid = orderValidator.isValidTrade(orderBadAction, brokerageAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_BrokerageAccount_EquityAsset() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        boolean isValid = orderValidator.isValidTrade(validBuyOrder, brokerageAccount);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsValidTrade_CryptoAccount_CryptoAsset_Valid() {
        // Arrange
        Order cryptoOrder = new Order(
            UUID.randomUUID(),
            "CRYPTO",
            "BTC",
            0.5,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(22500),  // 0.5 BTC * 45000 = 22500 > 10000 cash
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        
        // Crypto account has 10000 cash, but order costs more than that
        // So need an account with sufficient cash
        Account richCryptoAccount = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.CRYPTO,
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(50000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );
        
        when(pricingEngine.getPrice(anyString())).thenReturn(BigDecimal.valueOf(45000));

        // Act
        boolean isValid = orderValidator.isValidTrade(cryptoOrder, richCryptoAccount);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsValidTrade_CryptoAccount_EquityAsset_Invalid() {
        // Act
        boolean isValid = orderValidator.isValidTrade(validBuyOrder, cryptoAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_ForexAccount_ForexAsset_Valid() {
        // Arrange
        Order forexOrder = new Order(
            UUID.randomUUID(),
            "FOREX",
            "EURUSD",
            10000.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(10500),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        when(pricingEngine.getPrice("EURUSD")).thenReturn(BigDecimal.valueOf(1.05));

        // Act
        boolean isValid = orderValidator.isValidTrade(forexOrder, forexAccount);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsValidTrade_RetirementAccount_CryptoAsset_Invalid() {
        // Arrange
        Order cryptoOrder = new Order(
            UUID.randomUUID(),
            "CRYPTO",
            "ETH",
            10.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(25000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );

        // Act
        boolean isValid = orderValidator.isValidTrade(cryptoOrder, retirementAccount);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testIsValidTrade_RetirementAccount_EquityAsset_Valid() {
        // Arrange
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        boolean isValid = orderValidator.isValidTrade(validBuyOrder, retirementAccount);

        // Assert
        assertTrue(isValid);
    }
}
