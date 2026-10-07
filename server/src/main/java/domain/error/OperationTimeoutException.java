package domain.error;

/**
 * Exception thrown when operations exceed timeout.
 * Returns HTTP 504 Gateway Timeout
 * 
 * Use cases:
 * - Long-running order processing exceeds timeout
 * - API calls to external services take too long
 * - Database queries that timeout
 */
public class OperationTimeoutException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OperationTimeoutException(String message) {
        super(message);
        this.errorCode = "OPERATION_TIMEOUT";
    }

    public OperationTimeoutException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public OperationTimeoutException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    public OperationTimeoutException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    public OperationTimeoutException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "OPERATION_TIMEOUT";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
