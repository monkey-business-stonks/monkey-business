package domain.error;

public class OrderManagerException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OrderManagerException(String message) {
        super(message);
        this.errorCode = "ORDER_MANAGER_ERROR";
    }

    public OrderManagerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderManagerException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
