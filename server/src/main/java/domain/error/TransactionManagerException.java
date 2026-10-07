package domain.error;

/**
 * Exception thrown when transaction management operations fail.
 * Use this for account update failures (insufficient balance, asset updates, etc.)
 */
public class TransactionManagerException extends RuntimeException {
    private String errorCode;
    private Object details;

    public TransactionManagerException(String message) {
        super(message);
        this.errorCode = "TRANSACTION_MANAGER_ERROR";
    }

    public TransactionManagerException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public TransactionManagerException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    /**
     * Constructor with root cause chain for preserving underlying exceptions
     * Useful for wrapping database, API, or system-level exceptions
     */
    public TransactionManagerException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructor with all parameters including root cause
     */
    public TransactionManagerException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "TRANSACTION_MANAGER_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
