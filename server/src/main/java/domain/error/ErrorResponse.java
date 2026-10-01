package domain.error;

public class ErrorResponse {
    private String status;
    private String message;
    private String errorCode;
    private Object details;

    public ErrorResponse(String status, String message, String errorCode, Object details) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}
