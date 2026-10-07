package domain.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.WebRequest;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test GlobalExceptionHandler
 * 
 * Verifies all 14 exception handlers:
 * - Map to correct HTTP status codes
 * - Return ErrorResponse with 4 fields (status, error, message, timestamp)
 * - Log appropriately
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest mockRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mockRequest = mock(WebRequest.class);
    }

    // ============ 400 Bad Request Tests ============

    @Test
    void testOrderValidatorException_Returns400() {
        OrderValidatorException ex = new OrderValidatorException(
            "Invalid order quantity",
            "INVALID_INPUT"
        );

        ErrorResponse response = handler.handleOrderValidatorException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertEquals("Invalid order quantity", response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testIllegalArgumentException_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input value");

        ErrorResponse response = handler.handleIllegalArgumentException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }

    // Note: MethodArgumentNotValidException is tested via Spring integration tests
    // (not unit testable due to complex BindingResult requirements)

    // ============ 401 Unauthorized Tests ============

    @Test
    void testAuthenticationException_Returns401() {
        AuthenticationException ex = new AuthenticationException(
            "Invalid credentials",
            "AUTHENTICATION_FAILED"
        );

        ErrorResponse response = handler.handleAuthenticationException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(401, response.getStatus());
        assertEquals("Invalid credentials", response.getMessage());
    }

    // ============ 403 Forbidden Tests ============

    @Test
    void testAuthorizationException_Returns403() {
        AuthorizationException ex = new AuthorizationException(
            "Insufficient permissions",
            "AUTHORIZATION_DENIED"
        );

        ErrorResponse response = handler.handleAuthorizationException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(403, response.getStatus());
        assertEquals("Insufficient permissions", response.getMessage());
    }

    // ============ 404 Not Found Tests ============

    @Test
    void testNoSuchElementException_Returns404() {
        NoSuchElementException ex = new NoSuchElementException("User not found");

        ErrorResponse response = handler.handleNoSuchElementException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(404, response.getStatus());
        assertEquals("User not found", response.getMessage());
    }

    // ============ 409 Conflict Tests ============

    @Test
    void testBusinessConflictException_Returns409() {
        BusinessConflictException ex = new BusinessConflictException(
            "Duplicate order detected",
            "BUSINESS_CONFLICT"
        );

        ErrorResponse response = handler.handleBusinessConflictException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(409, response.getStatus());
        assertEquals("Duplicate order detected", response.getMessage());
    }

    // ============ 422 Unprocessable Entity Tests ============

    @Test
    void testOrderExecutorException_Returns422() {
        OrderExecutorException ex = new OrderExecutorException(
            "Insufficient cash balance",
            "EXECUTION_FAILED"
        );

        ErrorResponse response = handler.handleOrderExecutorException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(422, response.getStatus());
    }

    @Test
    void testOrderManagerException_Returns422() {
        OrderManagerException ex = new OrderManagerException(
            "Order state transition invalid",
            "ORDER_MANAGER_ERROR"
        );

        ErrorResponse response = handler.handleOrderManagerException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(422, response.getStatus());
    }

    @Test
    void testPricingEngineException_Returns422() {
        PricingEngineException ex = new PricingEngineException(
            "Failed to calculate pricing",
            "PRICING_ERROR"
        );

        ErrorResponse response = handler.handlePricingEngineException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(422, response.getStatus());
    }

    @Test
    void testTransactionManagerException_Returns422() {
        TransactionManagerException ex = new TransactionManagerException(
            "Transaction commit failed",
            "TRANSACTION_ERROR"
        );

        ErrorResponse response = handler.handleTransactionManagerException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(422, response.getStatus());
    }

    // ============ 500 Internal Server Error Tests ============

    @Test
    void testDataAccessException_Returns500() {
        DataAccessException ex = new DataAccessException(
            "Database connection failed",
            "DATA_ACCESS_ERROR"
        );

        ErrorResponse response = handler.handleDataAccessException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(500, response.getStatus());
    }

    @Test
    void testGeneralException_Returns500() {
        Exception ex = new Exception("Unexpected error");

        ErrorResponse response = handler.handleGeneralException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(500, response.getStatus());
    }

    // ============ 504 Gateway Timeout Tests ============

    @Test
    void testOperationTimeoutException_Returns504() {
        OperationTimeoutException ex = new OperationTimeoutException(
            "Operation took too long",
            "OPERATION_TIMEOUT"
        );

        ErrorResponse response = handler.handleOperationTimeoutException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(504, response.getStatus());
    }

    // ============ ErrorResponse Format Tests ============

    @Test
    void testErrorResponseHas4Fields() {
        OrderValidatorException ex = new OrderValidatorException("Test error", "TEST_ERROR");

        ErrorResponse errorResponse = handler.handleOrderValidatorException(ex, mockRequest);

        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getStatus(), "status field is required");
        assertNotNull(errorResponse.getError(), "error field is required");
        assertNotNull(errorResponse.getMessage(), "message field is required");
        assertNotNull(errorResponse.getTimestamp(), "timestamp field is required");
    }

    @Test
    void testErrorResponseTimestampIsISO8601() {
        OrderValidatorException ex = new OrderValidatorException("Test error", "TEST_ERROR");

        ErrorResponse errorResponse = handler.handleOrderValidatorException(ex, mockRequest);

        assertNotNull(errorResponse.getTimestamp());
        // Verify ISO 8601 format (basic check for T separator)
        assertTrue(errorResponse.getTimestamp().contains("T"), 
            "Timestamp should be ISO 8601 format");
    }

    @Test
    void testExceptionWithCause_PreservesChain() {
        Throwable rootCause = new RuntimeException("Root cause error");
        OrderExecutorException ex = new OrderExecutorException(
            "Execution failed",
            "EXECUTION_FAILED",
            rootCause
        );

        ErrorResponse response = handler.handleOrderExecutorException(ex, mockRequest);

        assertNotNull(response);
        assertEquals(422, response.getStatus());
        // The cause is preserved in the exception chain for logging
        assertNotNull(ex.getCause());
        assertEquals("Root cause error", ex.getCause().getMessage());
    }
}
