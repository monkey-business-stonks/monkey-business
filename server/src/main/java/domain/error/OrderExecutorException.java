package domain.error;

/**
 * Exception thrown when order execution fails.
 * Use this for errors during trade execution (pricing API failures, insufficient funds, etc.)
 */
public class OrderExecutorException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OrderExecutorException(String message) {
        super(message);
        this.errorCode = "ORDER_EXECUTOR_ERROR";
    }

    public OrderExecutorException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public OrderExecutorException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    /**
     * Constructor with root cause chain for preserving underlying exceptions
     * Useful for wrapping database, API, or system-level exceptions
     */
    public OrderExecutorException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructor with all parameters including root cause
     */
    public OrderExecutorException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "ORDER_EXECUTOR_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
