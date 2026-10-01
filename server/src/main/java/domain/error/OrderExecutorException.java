package domain.error;

public class OrderExecutorException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OrderExecutorException(String message) {
        super(message);
        this.errorCode = "ORDER_EXECUTOR_ERROR";
    }

    public OrderExecutorException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderExecutorException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
