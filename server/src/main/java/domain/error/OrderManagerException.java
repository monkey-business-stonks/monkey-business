package domain.error;

/**
 * Exception thrown when order management operations fail.
 * Use this for order state management errors (ownership validation, order lookup, etc.)
 */
public class OrderManagerException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OrderManagerException(String message) {
        super(message);
        this.errorCode = "ORDER_MANAGER_ERROR";
    }

    public OrderManagerException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public OrderManagerException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    /**
     * Constructor with root cause chain for preserving underlying exceptions
     * Useful for wrapping database, API, or system-level exceptions
     */
    public OrderManagerException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructor with all parameters including root cause
     */
    public OrderManagerException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "ORDER_MANAGER_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
