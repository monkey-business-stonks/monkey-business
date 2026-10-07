package domain.error;

/**
 * Exception thrown when order validation fails.
 * Use this for business rule validation errors (insufficient cash, incompatible account type, etc.)
 */
public class OrderValidatorException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OrderValidatorException(String message) {
        super(message);
        this.errorCode = "ORDER_VALIDATOR_ERROR";
    }

    public OrderValidatorException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public OrderValidatorException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    /**
     * Constructor with root cause chain for preserving underlying exceptions
     * Useful for wrapping database, API, or system-level exceptions
     */
    public OrderValidatorException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructor with all parameters including root cause
     */
    public OrderValidatorException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "ORDER_VALIDATOR_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
