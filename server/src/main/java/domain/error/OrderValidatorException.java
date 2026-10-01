package domain.error;

public class OrderValidatorException extends RuntimeException {
    private String errorCode;
    private Object details;

    public OrderValidatorException(String message) {
        super(message);
        this.errorCode = "ORDER_VALIDATOR_ERROR";
    }

    public OrderValidatorException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderValidatorException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
