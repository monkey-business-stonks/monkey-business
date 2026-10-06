import domain.entities.Order;
import domain.entities.Account;
import domain.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.UUID;

class OrderTest {
    private Order order;
    private UUID testOrderId;
    private Account testAccount;
    private ZonedDateTime now;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        now = ZonedDateTime.now();
        
        // Create a test account
        User testUser = new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            "password123",
            "Test User",
            "555-1234",
            LocalDate.of(1990, 1, 1),
            User.AccessLevel.USER,
            new HashSet<>(),
            now,
            now
        );
        
        testAccount = new Account(
            UUID.randomUUID(),
            now,
            Account.AccountType.BROKERAGE,
            new BigDecimal("10000.00"),
            new BigDecimal("10000.00"),
            new HashSet<>(),
            new HashSet<>()
        );
        testAccount.setUser(testUser);
        
        order = new Order(
            testOrderId,
            "EQUITY",
            "AAPL",
            10.0,
            "BUY",
            now,
            new BigDecimal("1500.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        order.setAccount(testAccount);
    }

    @Test
    void testOrderCreation() {
        assertEquals(testOrderId, order.getOrderId(), "Order ID should match");
        assertEquals("EQUITY", order.getOrderType(), "Order type should be EQUITY");
        assertEquals("AAPL", order.getTicker(), "Ticker should be AAPL");
        assertEquals(10.0, order.getQuantity(), "Quantity should be 10");
        assertEquals("BUY", order.getAction(), "Action should be BUY");
        assertEquals(Order.OrderStatus.FILLED, order.getStatus(), "Status should be FILLED");
    }

    @Test
    void testOrderGettersAndSetters() {
        assertEquals(testAccount, order.getAccount(), "Account should match");
        assertEquals(new BigDecimal("1500.00"), order.getSubmittedValue(), "Submitted value should match");
        assertEquals(201, order.getStatusCode(), "Status code should be 201");
    }

    @Test
    void testOrderStatusTransition() {
        Order newOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "GOOGL",
            5.0,
            "SELL",
            now,
            new BigDecimal("500.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        assertEquals(Order.OrderStatus.SUBMITTED, newOrder.getStatus(), "Initial status should be SUBMITTED");
        
        // Transition to ACCEPTED
        newOrder.updateExecution(now, null, Order.OrderStatus.ACCEPTED, 200);
        assertEquals(Order.OrderStatus.ACCEPTED, newOrder.getStatus(), "Status should be ACCEPTED");
    }

    @Test
    void testOrderUpdateExecution() {
        Order executingOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "MSFT",
            8.0,
            "BUY",
            now,
            new BigDecimal("2400.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        ZonedDateTime executionTime = now.plusSeconds(60);
        BigDecimal executedValue = new BigDecimal("2500.00");
        
        executingOrder.updateExecution(executionTime, executedValue, Order.OrderStatus.FILLED, 201);
        
        assertEquals(Order.OrderStatus.FILLED, executingOrder.getStatus(), "Status should be FILLED");
        assertEquals(executedValue, executingOrder.getExecutedValue(), "Executed value should be updated");
        assertEquals(executionTime, executingOrder.getExecutedOn(), "Execution time should be set");
    }

    @Test
    void testOrderRejection() {
        Order rejectedOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "TSLA",
            20.0,
            "BUY",
            now,
            new BigDecimal("7000.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        rejectedOrder.updateExecution(null, null, Order.OrderStatus.REJECTED, 422);
        
        assertEquals(Order.OrderStatus.REJECTED, rejectedOrder.getStatus(), "Status should be REJECTED");
        assertEquals(422, rejectedOrder.getStatusCode(), "Status code should be 422 (Unprocessable Entity)");
        assertNull(rejectedOrder.getExecutedOn(), "Executed on should be null for rejected orders");
        assertNull(rejectedOrder.getExecutedValue(), "Executed value should be null for rejected orders");
    }

    @Test
    void testBuyOrder() {
        assertEquals("BUY", order.getAction(), "Action should be BUY");
        assertEquals("BUY", order.getAction(), "Action should be identifiable as BUY");
    }

    @Test
    void testSellOrder() {
        Order sellOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            5.0,
            "SELL",
            now,
            new BigDecimal("750.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        
        assertEquals("SELL", sellOrder.getAction(), "Action should be SELL");
        assertTrue(sellOrder.getAction().equalsIgnoreCase("SELL"), "Should be identifiable as SELL order");
    }

    @Test
    void testOrderWithDifferentAssetClasses() {
        // Test CRYPTO order
        Order cryptoOrder = new Order(
            UUID.randomUUID(),
            "CRYPTO",
            "BTC",
            0.5,
            "BUY",
            now,
            new BigDecimal("21000.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        assertEquals("CRYPTO", cryptoOrder.getOrderType(), "Should be CRYPTO order");
        
        // Test FOREX order
        Order forexOrder = new Order(
            UUID.randomUUID(),
            "FOREX",
            "EURUSD",
            1000.0,
            "BUY",
            now,
            new BigDecimal("1100.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        assertEquals("FOREX", forexOrder.getOrderType(), "Should be FOREX order");
    }

    @Test
    void testOrderTimestamps() {
        ZonedDateTime createdAt = now.minusHours(1);
        
        Order timedOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "META",
            3.0,
            "BUY",
            createdAt,
            new BigDecimal("600.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            createdAt
        );
        
        assertEquals(createdAt, timedOrder.getCreatedAt(), "Created timestamp should match");
        assertNotNull(timedOrder.getSubmittedOn(), "Submitted on should not be null");
    }

    @Test
    void testOrderExecutionDetails() {
        Order detailedOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "NVDA",
            15.0,
            "BUY",
            now,
            new BigDecimal("3000.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        
        // Update execution to set both values explicitly
        detailedOrder.updateExecution(now, new BigDecimal("3000.00"), Order.OrderStatus.FILLED, 201);
        
        assertNotNull(detailedOrder.getSubmittedValue(), "Submitted value should not be null");
        assertNotNull(detailedOrder.getExecutedValue(), "Executed value should not be null");
        assertEquals(0, detailedOrder.getSubmittedValue().compareTo(detailedOrder.getExecutedValue()), 
            "For FILLED orders, values should match");
    }

    @Test
    void testOrderWithFractionalShares() {
        Order fractionalOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            2.5,
            "BUY",
            now,
            new BigDecimal("475.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        
        assertEquals(2.5, fractionalOrder.getQuantity(), "Should support fractional shares");
    }

    @Test
    void testOrderStatusCodes() {
        // Test different HTTP status codes for different statuses
        Order submittedOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            1.0,
            "BUY",
            now,
            new BigDecimal("150.00"),
            Order.OrderStatus.SUBMITTED,
            202,  // Accepted
            now
        );
        assertEquals(202, submittedOrder.getStatusCode(), "SUBMITTED should have status code 202");
        
        Order filledOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            1.0,
            "BUY",
            now,
            new BigDecimal("150.00"),
            Order.OrderStatus.FILLED,
            201,  // Created
            now
        );
        assertEquals(201, filledOrder.getStatusCode(), "FILLED should have status code 201");
    }

    @Test
    void testZeroQuantityOrder() {
        Order zeroOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            0.0,
            "BUY",
            now,
            new BigDecimal("0.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        assertEquals(0.0, zeroOrder.getQuantity(), "Should allow zero quantity order");
    }

    @Test
    void testNegativeQuantityOrder() {
        Order negativeOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            -10.0,
            "SELL",
            now,
            new BigDecimal("-1500.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        assertEquals(-10.0, negativeOrder.getQuantity(), "Should allow negative quantity for SELL orders");
    }

    @Test
    void testOrderActionCaseSensitivity() {
        Order lowerCaseOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            5.0,
            "buy",
            now,
            new BigDecimal("750.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        assertEquals("buy", lowerCaseOrder.getAction(), "Action should preserve case as provided");
        assertFalse(lowerCaseOrder.getAction().equals("BUY"), "Lowercase 'buy' should not equal uppercase 'BUY'");
    }

    @Test
    void testOrderWithNullExecutedValue() {
        Order pendingOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            5.0,
            "BUY",
            now,
            new BigDecimal("750.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        assertNull(pendingOrder.getExecutedValue(), "Submitted orders should have null executedValue");
        assertNull(pendingOrder.getExecutedOn(), "Submitted orders should have null executedOn");
    }

    @Test
    void testStatusTransitionFromSubmittedToFilled() {
        Order order = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            10.0,
            "BUY",
            now,
            new BigDecimal("1500.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        ZonedDateTime executionTime = now.plusSeconds(30);
        BigDecimal executedValue = new BigDecimal("1510.00");
        
        order.updateExecution(executionTime, executedValue, Order.OrderStatus.FILLED, 201);
        
        assertEquals(Order.OrderStatus.FILLED, order.getStatus(), "Should transition to FILLED");
        assertEquals(executionTime, order.getExecutedOn(), "Execution time should be set");
        assertEquals(0, order.getExecutedValue().compareTo(executedValue), "Executed value should be set");
    }

    @Test
    void testOrderAcceptedStatus() {
        Order order = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            10.0,
            "BUY",
            now,
            new BigDecimal("1500.00"),
            Order.OrderStatus.SUBMITTED,
            202,
            now
        );
        
        order.updateExecution(null, null, Order.OrderStatus.ACCEPTED, 200);
        
        assertEquals(Order.OrderStatus.ACCEPTED, order.getStatus(), "Status should transition to ACCEPTED");
        assertEquals(200, order.getStatusCode(), "Status code should be 200 for ACCEPTED");
        assertNull(order.getExecutedOn(), "ACCEPTED orders should not have execution time");
    }

    @Test
    void testLargeOrderValue() {
        Order largeOrder = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            1000000.0,
            "BUY",
            now,
            new BigDecimal("150000000.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        
        assertEquals(1000000.0, largeOrder.getQuantity(), "Should handle large quantities");
        assertEquals(0, largeOrder.getSubmittedValue().compareTo(new BigDecimal("150000000.00")), "Should handle large values");
    }
}
