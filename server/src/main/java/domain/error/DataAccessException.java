package domain.error;

/**
 * Exception thrown when database/data access operations fail.
 * Returns HTTP 500 Internal Server Error
 * 
 * Use cases:
 * - Database connection failures
 * - Query execution errors
 * - Transaction failures
 * - Persistence layer errors
 */
public class DataAccessException extends RuntimeException {
    private String errorCode;
    private Object details;

    public DataAccessException(String message) {
        super(message);
        this.errorCode = "DATA_ACCESS_ERROR";
    }

    public DataAccessException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public DataAccessException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    public DataAccessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    public DataAccessException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "DATA_ACCESS_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
