package domain.error;

/**
 * Exception thrown when authorization fails.
 * Returns HTTP 403 Forbidden
 * 
 * Use cases:
 * - User doesn't own the account/order they're accessing
 * - Insufficient permissions for operation
 * - Role-based access control violation
 */
public class AuthorizationException extends RuntimeException {
    private String errorCode;
    private Object details;

    public AuthorizationException(String message) {
        super(message);
        this.errorCode = "AUTHORIZATION_DENIED";
    }

    public AuthorizationException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public AuthorizationException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    public AuthorizationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    public AuthorizationException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "AUTHORIZATION_DENIED";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
