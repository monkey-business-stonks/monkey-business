package domain.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global exception handler for all controllers.
 * Centralizes error handling and returns standardized ErrorResponse objects.
 * ErrorResponse matches test.yaml schema: status (HTTP code), error (type), message, timestamp.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle OrderValidatorException - validation failed on order
     * Returns 400 Bad Request
     */
    @ExceptionHandler(OrderValidatorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleOrderValidatorException(OrderValidatorException ex, WebRequest request) {
        logger.warn("Order validation failed: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage());
    }

    /**
     * Handle OrderExecutorException - order execution failed
     * Returns 422 Unprocessable Entity
     */
    @ExceptionHandler(OrderExecutorException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleOrderExecutorException(OrderExecutorException ex, WebRequest request) {
        logger.error("Order execution failed: {}", ex.getMessage());
        return new ErrorResponse(422, "Unprocessable Entity", ex.getMessage());
    }

    /**
     * Handle OrderManagerException - order management operation failed
     * Returns 422 Unprocessable Entity
     */
    @ExceptionHandler(OrderManagerException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleOrderManagerException(OrderManagerException ex, WebRequest request) {
        logger.error("Order management error: {}", ex.getMessage());
        return new ErrorResponse(422, "Unprocessable Entity", ex.getMessage());
    }

    /**
     * Handle PricingEngineException - pricing calculation failed
     * Returns 422 Unprocessable Entity
     */
    @ExceptionHandler(PricingEngineException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handlePricingEngineException(PricingEngineException ex, WebRequest request) {
        logger.error("Pricing engine error: {}", ex.getMessage());
        return new ErrorResponse(422, "Unprocessable Entity", ex.getMessage());
    }

    /**
     * Handle TransactionManagerException - transaction operation failed
     * Returns 422 Unprocessable Entity
     */
    @ExceptionHandler(TransactionManagerException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleTransactionManagerException(TransactionManagerException ex, WebRequest request) {
        logger.error("Transaction manager error: {}", ex.getMessage());
        return new ErrorResponse(422, "Unprocessable Entity", ex.getMessage());
    }

    /**
     * Handle BusinessConflictException - business rule conflict detected
     * Returns 409 Conflict for state conflicts and duplicate attempts
     */
    @ExceptionHandler(BusinessConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleBusinessConflictException(BusinessConflictException ex, WebRequest request) {
        logger.warn("Business conflict detected: {}", ex.getMessage());
        return new ErrorResponse(409, "Conflict", ex.getMessage());
    }

    /**
     * Handle AuthenticationException - authentication failed
     * Returns 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return new ErrorResponse(401, "Unauthorized", ex.getMessage());
    }

    /**
     * Handle AuthorizationException - user not authorized
     * Returns 403 Forbidden
     */
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationException(AuthorizationException ex, WebRequest request) {
        logger.warn("Authorization denied: {}", ex.getMessage());
        return new ErrorResponse(403, "Forbidden", ex.getMessage());
    }

    /**
     * Handle DataAccessException - database/persistence error
     * Returns 500 Internal Server Error
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleDataAccessException(DataAccessException ex, WebRequest request) {
        logger.error("Data access error: {}", ex.getMessage());
        return new ErrorResponse(500, "Internal Server Error", "Database operation failed");
    }

    /**
     * Handle OperationTimeoutException - operation exceeded timeout
     * Returns 504 Gateway Timeout
     */
    @ExceptionHandler(OperationTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ErrorResponse handleOperationTimeoutException(OperationTimeoutException ex, WebRequest request) {
        logger.error("Operation timeout: {}", ex.getMessage());
        return new ErrorResponse(504, "Gateway Timeout", ex.getMessage());
    }

    /**
     * Handle IllegalArgumentException - invalid input parameter
     * Returns 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage());
    }

    /**
     * Handle NoSuchElementException - resource not found
     * Returns 404 Not Found
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return new ErrorResponse(404, "Not Found", ex.getMessage());
    }

    /**
     * Handle validation errors from @Valid annotation
     * Returns 400 Bad Request with field validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        logger.warn("Validation error: {}", errors);
        return new ErrorResponse(400, "Bad Request", errors);
    }

    /**
     * Handle all other exceptions - catch-all for unexpected errors
     * Returns 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneralException(Exception ex, WebRequest request) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred");
    }
}
