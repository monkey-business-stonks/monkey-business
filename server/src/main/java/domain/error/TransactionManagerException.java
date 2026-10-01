package domain.error;

public class TransactionManagerException extends RuntimeException {
    private String errorCode;
    private Object details;

    public TransactionManagerException(String message) {
        super(message);
        this.errorCode = "TRANSACTION_MANAGER_ERROR";
    }

    public TransactionManagerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransactionManagerException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
