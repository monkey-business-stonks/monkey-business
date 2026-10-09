package domain.service;

import domain.dto.*;
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private OrderManager orderManager;

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private OrderExecutor orderExecutor;

    @Mock
    private OrderExecutionEngine transactionManager;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private UUID testUserId;
    private UUID testAccountId;
    private User testUser;
    private Account testAccount;
    private PlaceOrderRequest validRequest;
    private Order testOrder;

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

        validRequest = new PlaceOrderRequest();
        validRequest.setOrderType(domain.dto.OrderType.EQUITY);
        validRequest.setTicker("AAPL");
        validRequest.setQuantity(BigDecimal.valueOf(100.0));
        validRequest.setAction(domain.dto.OrderAction.BUY);
    }

    @Test
    void testPlaceOrder_ValidationFails() {
        // Phase 3a: Async behavior - validation happens in consumer, not in OrderService
        // Arrange
        when(orderManager.createOrder(testAccountId, "EQUITY", "AAPL", 100.0, "BUY")).thenReturn(testOrder);
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.placeOrder(testAccountId, validRequest);

        // Assert
        assertNotNull(response);
        // Order should be SUBMITTED, not REJECTED (validation happens async in consumer)
        assertEquals(Order.OrderStatus.SUBMITTED, testOrder.getStatus());
        // Event should be published for async processing
        verify(orderEventPublisher, times(1)).publishOrderSubmitted(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPlaceOrder_ExecutionFails() {
        // Phase 3a: Async behavior - execution happens in consumer, not in OrderService
        // Arrange
        when(orderManager.createOrder(testAccountId, "EQUITY", "AAPL", 100.0, "BUY")).thenReturn(testOrder);
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.placeOrder(testAccountId, validRequest);

        // Assert
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testPlaceOrder_AccountNotFound() {
        // Arrange
        when(orderManager.createOrder(testAccountId, "EQUITY", "AAPL", 100.0, "BUY")).thenReturn(testOrder);
        when(accountService.getAccountDirect(testAccountId)).thenReturn(null);

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> orderService.placeOrder(testAccountId, validRequest));
    }

    @Test
    void testPlaceOrder_InvalidRequest_MissingTicker() {
        // Arrange
        PlaceOrderRequest invalidRequest = new PlaceOrderRequest();
        invalidRequest.setOrderType(domain.dto.OrderType.EQUITY);
        invalidRequest.setTicker(null);
        invalidRequest.setQuantity(BigDecimal.valueOf(100.0));
        invalidRequest.setAction(domain.dto.OrderAction.BUY);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(testAccountId, invalidRequest));
    }

    @Test
    void testPlaceOrder_InvalidRequest_MissingQuantity() {
        // Arrange
        PlaceOrderRequest invalidRequest = new PlaceOrderRequest();
        invalidRequest.setOrderType(domain.dto.OrderType.EQUITY);
        invalidRequest.setTicker("AAPL");
        invalidRequest.setQuantity(null);
        invalidRequest.setAction(domain.dto.OrderAction.BUY);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(testAccountId, invalidRequest));
    }

    @Test
    void testPlaceOrder_InvalidRequest_InvalidQuantity() {
        // Arrange
        PlaceOrderRequest invalidRequest = new PlaceOrderRequest();
        invalidRequest.setOrderType(domain.dto.OrderType.EQUITY);
        invalidRequest.setTicker("AAPL");
        invalidRequest.setQuantity(BigDecimal.valueOf(0.0));
        invalidRequest.setAction(domain.dto.OrderAction.BUY);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(testAccountId, invalidRequest));
    }

    @Test
    void testPlaceOrder_SellOrder() {
        // Phase 3a: Async behavior - all orders return SUBMITTED status
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

        PlaceOrderRequest sellRequest = new PlaceOrderRequest();
        sellRequest.setOrderType(domain.dto.OrderType.EQUITY);
        sellRequest.setTicker("AAPL");
        sellRequest.setQuantity(BigDecimal.valueOf(50.0));
        sellRequest.setAction(domain.dto.OrderAction.SELL);

        when(orderManager.createOrder(testAccountId, "EQUITY", "AAPL", 50.0, "SELL")).thenReturn(sellOrder);
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(orderRepository.save(any(Order.class))).thenReturn(sellOrder);

        // Act
        OrderResponse response = orderService.placeOrder(testAccountId, sellRequest);

        // Assert
        assertNotNull(response);
        assertEquals(Order.OrderStatus.SUBMITTED, sellOrder.getStatus());
        verify(orderEventPublisher, times(1)).publishOrderSubmitted(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPlaceOrder_CryptoOrder() {
        // Phase 3a: Async behavior - all orders return SUBMITTED status
        // Arrange
        Order cryptoOrder = new Order(
            UUID.randomUUID(),
            "CRYPTO",
            "BTC",
            1.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(45000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        cryptoOrder.setAccount(testAccount);

        PlaceOrderRequest cryptoRequest = new PlaceOrderRequest();
        cryptoRequest.setOrderType(domain.dto.OrderType.CRYPTO);
        cryptoRequest.setTicker("BTC");
        cryptoRequest.setQuantity(BigDecimal.valueOf(1.0));
        cryptoRequest.setAction(domain.dto.OrderAction.BUY);

        when(orderManager.createOrder(testAccountId, "CRYPTO", "BTC", 1.0, "BUY")).thenReturn(cryptoOrder);
        when(accountService.getAccountDirect(testAccountId)).thenReturn(testAccount);
        when(orderRepository.save(any(Order.class))).thenReturn(cryptoOrder);

        // Act
        OrderResponse response = orderService.placeOrder(testAccountId, cryptoRequest);

        // Assert
        assertNotNull(response);
        assertEquals(Order.OrderStatus.SUBMITTED, cryptoOrder.getStatus());
        verify(orderEventPublisher, times(1)).publishOrderSubmitted(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPlaceOrder_LargeOrder() {
        // Phase 3a: Async behavior - all orders return SUBMITTED status
        // Arrange
        Order largeOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "TSLA",
            1000.0,
            "BUY",
            ZonedDateTime.now(),
            BigDecimal.valueOf(250000),
            Order.OrderStatus.SUBMITTED,
            202,
            ZonedDateTime.now()
        );
        largeOrder.setAccount(testAccount);

        PlaceOrderRequest largeRequest = new PlaceOrderRequest();
        largeRequest.setOrderType(domain.dto.OrderType.EQUITY);
        largeRequest.setTicker("TSLA");
        largeRequest.setQuantity(BigDecimal.valueOf(1000.0));
        largeRequest.setAction(domain.dto.OrderAction.BUY);

        Account largeAccount = new Account(
            testAccountId,
            ZonedDateTime.now(),
            Account.AccountType.BROKERAGE,
            BigDecimal.valueOf(300000),
            BigDecimal.valueOf(300000),
            new HashSet<>(),
            new HashSet<>()
        );

        when(orderManager.createOrder(testAccountId, "EQUITY", "TSLA", 1000.0, "BUY")).thenReturn(largeOrder);
        when(accountService.getAccountDirect(testAccountId)).thenReturn(largeAccount);
        when(orderRepository.save(any(Order.class))).thenReturn(largeOrder);

        // Act
        OrderResponse response = orderService.placeOrder(testAccountId, largeRequest);

        // Assert
        assertNotNull(response);
        assertEquals(Order.OrderStatus.SUBMITTED, largeOrder.getStatus());
        verify(orderEventPublisher, times(1)).publishOrderSubmitted(any(), any(), any(), any(), any(), any());
    }
}
