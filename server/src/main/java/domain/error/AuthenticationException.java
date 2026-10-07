package domain.error;

/**
 * Exception thrown when authentication fails.
 * Returns HTTP 401 Unauthorized
 * 
 * Use cases:
 * - Invalid credentials (wrong password, non-existent user)
 * - Expired or invalid JWT token
 * - Missing authentication header
 */
public class AuthenticationException extends RuntimeException {
    private String errorCode;
    private Object details;

    public AuthenticationException(String message) {
        super(message);
        this.errorCode = "AUTHENTICATION_FAILED";
    }

    public AuthenticationException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public AuthenticationException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    public AuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    public AuthenticationException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "AUTHENTICATION_FAILED";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
