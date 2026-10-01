package domain.error;

public class PricingEngineException extends RuntimeException {
    private String errorCode;
    private Object details;

    public PricingEngineException(String message) {
        super(message);
        this.errorCode = "PRICING_ENGINE_ERROR";
    }

    public PricingEngineException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PricingEngineException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
