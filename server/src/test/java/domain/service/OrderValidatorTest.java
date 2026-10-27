package domain.service;

import domain.entities.Account;
import domain.entities.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @Mock
    private PricingEngine pricingEngine;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private OrderValidator orderValidator;

    private Account brokerageAccount;
    private Account cryptoAccount;
    private Account forexAccount;
    private Account retirementAccount;
    private Order validBuyOrder;

    @BeforeEach
    void setUp() {
        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-21T16:00:00Z"), ZoneOffset.UTC));

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
            Account.AccountType.TRADITIONAL_401K,
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
        OrderValidator.ValidationResult result = orderValidator.validateTrade(validBuyOrder, lowCashAccount);

        // Assert
        assertFalse(result.valid());
        assertEquals(OrderValidator.INSUFFICIENT_FUNDS_OR_QUANTITY_STATUS_CODE, result.rejectionStatusCode());
    }

    @Test
    void testIsValidTrade_ForexOrder_RequiresForexAccount() {
        Order forexOrder = new Order(
            UUID.randomUUID(), "FOREIGN EXCHANGE", "EURUSD", 1000.0, "BUY",
            ZonedDateTime.now(), BigDecimal.valueOf(1100), Order.OrderStatus.SUBMITTED, 202, ZonedDateTime.now()
        );

        assertFalse(orderValidator.isValidTrade(forexOrder, brokerageAccount));
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

        when(assetService.getAsset(brokerageAccount, "AAPL")).thenReturn(
            new domain.entities.Asset(UUID.randomUUID(), "EQUITY", "AAPL", "Apple Inc.", 100.0, BigDecimal.valueOf(150))
        );

        // Act
        boolean isValid = orderValidator.isValidTrade(sellOrder, brokerageAccount);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsValidTrade_SellOrder_InsufficientHoldings() {
        Order sellOrder = new Order(
            UUID.randomUUID(), "EQUITY", "AAPL", 100.0, "SELL",
            ZonedDateTime.now(), BigDecimal.valueOf(15000), Order.OrderStatus.SUBMITTED, 202, ZonedDateTime.now()
        );

        when(assetService.getAsset(brokerageAccount, "AAPL")).thenReturn(
            new domain.entities.Asset(UUID.randomUUID(), "EQUITY", "AAPL", "Apple Inc.", 25.0, BigDecimal.valueOf(150))
        );

        OrderValidator.ValidationResult result = orderValidator.validateTrade(sellOrder, brokerageAccount);
        assertFalse(result.valid());
        assertEquals(OrderValidator.INSUFFICIENT_FUNDS_OR_QUANTITY_STATUS_CODE, result.rejectionStatusCode());
    }

    @Test
    void testValidateTrade_EquityOrderOutsideMarketHours_ReturnsMarketClosedCode() {
        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC));

        OrderValidator.ValidationResult result = orderValidator.validateTrade(validBuyOrder, brokerageAccount);

        assertFalse(result.valid());
        assertEquals(OrderValidator.MARKET_CLOSED_STATUS_CODE, result.rejectionStatusCode());
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
    void testIsValidTrade_RetirementAccount_EquityAsset_Invalid() {
        assertFalse(orderValidator.isValidTrade(validBuyOrder, retirementAccount));
    }

    @Test
    void testIsValidTrade_RetirementAccount_ForexAsset_Invalid() {
        Order forexOrder = new Order(
            UUID.randomUUID(), "FOREIGN EXCHANGE", "EURUSD", 1000.0, "BUY",
            ZonedDateTime.now(), BigDecimal.valueOf(1100), Order.OrderStatus.SUBMITTED, 202, ZonedDateTime.now()
        );

        assertFalse(orderValidator.isValidTrade(forexOrder, retirementAccount));
    }

    @ParameterizedTest
    @CsvSource({"TRADITIONAL_401K", "ROTH_401K", "TRADITIONAL_IRA", "ROTH_IRA"})
    void testRetirementAccountTypesSupportEquities(String accountType) {
        Account account = new Account(
            UUID.randomUUID(),
            ZonedDateTime.now(),
            Account.AccountType.valueOf(accountType),
            BigDecimal.valueOf(30000),
            BigDecimal.valueOf(30000),
            new java.util.HashSet<>(),
            new java.util.HashSet<>()
        );
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        assertTrue(orderValidator.isValidTrade(validBuyOrder, account));
    }

    @ParameterizedTest
    @CsvSource({
        "AAPL, 2026-10-21T14:29:59Z, false",
        "AAPL, 2026-10-21T14:30:00Z, true",
        "AAPL, 2026-10-21T20:59:59Z, true",
        "AAPL, 2026-10-21T21:00:00Z, false",
        "AAPL, 2026-10-21T22:00:00Z, false",
        "INFY.NS, 2026-10-21T14:14:59Z, false",
        "INFY.NS, 2026-10-21T14:15:00Z, true",
        "INFY.NS, 2026-10-21T22:29:59Z, true",
        "INFY.NS, 2026-10-21T22:30:00Z, false",
        "INFY.BO, 2026-10-21T14:15:00Z, true",
        "INFY.BO, 2026-10-21T22:30:00Z, false",
        "AAPL, 2026-10-24T16:00:00Z, false",
        "INFY.NS, 2026-10-24T16:00:00Z, false",
        "AAPL, 2026-10-25T16:00:00Z, false",
        "INFY.NS, 2026-10-25T16:00:00Z, false"
    })
    void equityScheduleBoundaries(String ticker, String instant, boolean expected) {
        orderValidator.setClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        assertEquals(expected, orderValidator.isWithinTradingHours("EQUITY", ticker));
    }

    @ParameterizedTest
    @CsvSource({
        "2026-07-05T20:59:59Z, false",
        "2026-07-05T21:00:00Z, true",
        "2026-07-10T20:59:59Z, true",
        "2026-07-10T21:00:00Z, false",
        "2026-01-04T21:59:59Z, false",
        "2026-01-04T22:00:00Z, true",
        "2026-01-09T21:59:59Z, true",
        "2026-01-09T22:00:00Z, false",
        "2026-01-10T12:00:00Z, false",
        "2026-01-07T00:00:00Z, true"
    })
    void forexScheduleHandlesEasternDaylightAndStandardTime(String instant, boolean expected) {
        orderValidator.setClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        assertEquals(expected, orderValidator.isWithinTradingHours("FOREX", "FX:EURUSD"));
        assertEquals(expected, orderValidator.isWithinTradingHours("FOREIGN EXCHANGE", "FX:EURUSD"));
    }

    @ParameterizedTest
    @CsvSource({
        "2026-10-23T00:00:00Z",
        "2026-10-24T00:00:00Z",
        "2026-10-25T23:59:59Z"
    })
    void cryptoBuyAndSellRemainValidOutsideEquityHours(String instant) {
        orderValidator.setClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        when(pricingEngine.getPrice("X:BTC-USD")).thenReturn(BigDecimal.valueOf(60000));
        when(assetService.getAsset(cryptoAccount, "X:BTC-USD")).thenReturn(
            new domain.entities.Asset(UUID.randomUUID(), "CRYPTO", "X:BTC-USD", "Bitcoin",
                0.1, BigDecimal.valueOf(60000))
        );
        for (String action : new String[]{"BUY", "SELL"}) {
            Order order = new Order(UUID.randomUUID(), "CRYPTO", "X:BTC-USD", 0.01, action,
                ZonedDateTime.now(), BigDecimal.valueOf(600), Order.OrderStatus.SUBMITTED, 202,
                ZonedDateTime.now());
            assertTrue(orderValidator.isValidTrade(order, cryptoAccount), action + " at " + instant);
        }
    }

    @Test
    void testIsWithinTradingHours_USAndIndiaEquities() {
        assertTrue(orderValidator.isWithinTradingHours("EQUITY", "AAPL"));
        assertTrue(orderValidator.isWithinTradingHours("EQUITY", "INFY.NS"));

        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-21T14:14:59Z"), ZoneOffset.UTC));
        assertFalse(orderValidator.isWithinTradingHours("EQUITY", "INFY.NS"));
        assertFalse(orderValidator.isWithinTradingHours("EQUITY", "AAPL"));
    }

    @Test
    void testIsWithinTradingHours_EquitiesClosedOnWeekend() {
        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-24T16:00:00Z"), ZoneOffset.UTC));

        assertFalse(orderValidator.isWithinTradingHours("EQUITY", "AAPL"));
        assertFalse(orderValidator.isWithinTradingHours("EQUITY", "INFY.NS"));
    }

    @Test
    void testIsWithinTradingHours_CryptoAlwaysOpen() {
        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-24T16:00:00Z"), ZoneOffset.UTC));

        assertTrue(orderValidator.isWithinTradingHours("CRYPTO", "BTC"));
    }

    @Test
    void testIsWithinTradingHours_ForexSundayAndFridayBoundaries() {
        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-25T20:59:59Z"), ZoneOffset.UTC));
        assertFalse(orderValidator.isWithinTradingHours("FOREX", "EURUSD"));

        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-25T21:00:00Z"), ZoneOffset.UTC));
        assertTrue(orderValidator.isWithinTradingHours("FOREX", "EURUSD"));

        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-30T20:59:59Z"), ZoneOffset.UTC));
        assertTrue(orderValidator.isWithinTradingHours("FOREIGN EXCHANGE", "EURUSD"));

        orderValidator.setClock(Clock.fixed(Instant.parse("2026-10-30T21:00:00Z"), ZoneOffset.UTC));
        assertFalse(orderValidator.isWithinTradingHours("FOREX", "EURUSD"));
    }
}
