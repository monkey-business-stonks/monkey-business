package domain.error;

/**
 * Base exception class for all entity validation and state errors.
 * Provides standardized error handling across all entities.
 */
public abstract class EntityException extends RuntimeException {
    private String errorCode;
    private Object details;

    public EntityException(String message) {
        super(message);
        this.errorCode = "ENTITY_ERROR";
    }

    public EntityException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public EntityException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
