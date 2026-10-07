package domain.service;

import domain.entities.Account;
import domain.entities.Order;
import domain.entities.User;
import domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagerTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private PricingEngine pricingEngine;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderManager orderManager;

    private UUID testUserId;
    private UUID testAccountId;
    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAccountId = UUID.randomUUID();

        testUser = new User(
            testUserId,
            "testuser",
            "test@example.com",
            "password123",
            "Test User",
            null,
            LocalDate.of(2000, 1, 1),
            User.AccessLevel.USER,
            new HashSet<>(),
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        testAccount = new Account(
            testAccountId,
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(50000),
            new HashSet<>(),
            new HashSet<>()
        );
        testAccount.setUser(testUser);
    }

    @Test
    void testCreateOrder_Success() {
        // Arrange
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(pricingEngine.getPrice("AAPL")).thenReturn(BigDecimal.valueOf(150));

        // Act
        Order order = orderManager.createOrder(
            testAccountId,
            "EQUITY",
            "AAPL",
            100.0,
            "BUY"
        );

        // Assert
        assertNotNull(order);
        assertEquals("AAPL", order.getTicker());
        assertEquals(100.0, order.getQuantity());
        assertEquals("BUY", order.getAction());
        assertEquals(Order.OrderStatus.SUBMITTED, order.getStatus());
        assertEquals(202, order.getStatusCode());
        assertNotNull(order.getSubmittedOn());
    }

    @Test
    void testCreateOrder_AccountNotFound() {
        // Arrange
        when(accountService.getAccountDirect(testAccountId)).thenReturn(null);

        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () -> {
            orderManager.createOrder(testAccountId, "EQUITY", "AAPL", 100.0, "BUY");
        });
    }

    @Test
    void testCreateOrder_TickerUppercased() {
        // Arrange
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(pricingEngine.getPrice(anyString())).thenReturn(BigDecimal.valueOf(150));

        // Act
        Order order = orderManager.createOrder(
            testAccountId,
            "EQUITY",
            "aapl",
            100.0,
            "BUY"
        );

        // Assert
        assertEquals("AAPL", order.getTicker());
    }

    @Test
    void testCreateOrder_SellOrder() {
        // Arrange
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(pricingEngine.getPrice("TSLA")).thenReturn(BigDecimal.valueOf(200));

        // Act
        Order order = orderManager.createOrder(
            testAccountId,
            "EQUITY",
            "TSLA",
            50.0,
            "SELL"
        );

        // Assert
        assertNotNull(order);
        assertEquals("SELL", order.getAction());
        assertEquals(50.0, order.getQuantity());
    }

    @Test
    void testCreateOrder_CryptoAsset() {
        // Arrange
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(pricingEngine.getPrice("BTC")).thenReturn(BigDecimal.valueOf(45000));

        // Act
        Order order = orderManager.createOrder(
            testAccountId,
            "CRYPTO",
            "BTC",
            1.0,
            "BUY"
        );

        // Assert
        assertEquals("CRYPTO", order.getOrderType());
        assertEquals("BTC", order.getTicker());
    }

    @Test
    void testUpdateOrderStatus_ToFilled() {
        // Arrange
        Order order = new Order(
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

        // Act
        orderManager.updateOrderStatus(order, Order.OrderStatus.FILLED, 201);

        // Assert
        assertEquals(Order.OrderStatus.FILLED, order.getStatus());
        assertEquals(201, order.getStatusCode());
    }

    @Test
    void testUpdateOrderStatus_ToAccepted() {
        // Arrange
        Order order = new Order(
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

        // Act
        orderManager.updateOrderStatus(order, Order.OrderStatus.ACCEPTED, 200);

        // Assert
        assertEquals(Order.OrderStatus.ACCEPTED, order.getStatus());
        assertEquals(200, order.getStatusCode());
        assertNull(order.getExecutedValue());
    }

    @Test
    void testUpdateOrderStatus_ToRejected() {
        // Arrange
        Order order = new Order(
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

        // Act
        orderManager.updateOrderStatus(order, Order.OrderStatus.REJECTED, 422);

        // Assert
        assertEquals(Order.OrderStatus.REJECTED, order.getStatus());
        assertEquals(422, order.getStatusCode());
        assertNull(order.getExecutedOn());
        assertNull(order.getExecutedValue());
    }

    @Test
    void testUpdateOrderStatus_NullOrder() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> orderManager.updateOrderStatus(null, Order.OrderStatus.REJECTED, 422));
    }

    @Test
    void testGetActiveOrders_InitiallyEmpty() {
        // Act
        var activeOrders = orderManager.getActiveOrders();

        // Assert
        assertNotNull(activeOrders);
        assertEquals(0, activeOrders.size());
    }
}
