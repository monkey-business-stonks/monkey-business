package domain.error;

/**
 * Exception thrown when a business rule conflict occurs.
 * Returns HTTP 409 Conflict
 * 
 * Use cases:
 * - Duplicate order attempts
 * - Insufficient holdings for SELL
 * - Account state conflicts (locked, frozen, etc.)
 * - Resource state conflicts preventing operation
 */
public class BusinessConflictException extends RuntimeException {
    private String errorCode;
    private Object details;

    public BusinessConflictException(String message) {
        super(message);
        this.errorCode = "BUSINESS_CONFLICT";
    }

    public BusinessConflictException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public BusinessConflictException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    /**
     * Constructor with root cause chain for preserving underlying exceptions
     * Useful for wrapping database, API, or system-level exceptions
     */
    public BusinessConflictException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructor with all parameters including root cause
     */
    public BusinessConflictException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "BUSINESS_CONFLICT";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
