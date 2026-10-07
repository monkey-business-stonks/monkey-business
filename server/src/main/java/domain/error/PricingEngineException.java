package domain.error;

/**
 * Exception thrown when pricing engine operations fail.
 * Use this for market data failures (API errors, unavailable prices, etc.)
 */
public class PricingEngineException extends RuntimeException {
    private String errorCode;
    private Object details;

    public PricingEngineException(String message) {
        super(message);
        this.errorCode = "PRICING_ENGINE_ERROR";
    }

    public PricingEngineException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    public PricingEngineException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    /**
     * Constructor with root cause chain for preserving underlying exceptions
     * Useful for wrapping database, API, or system-level exceptions
     */
    public PricingEngineException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructor with all parameters including root cause
     */
    public PricingEngineException(String message, String errorCode, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
        this.details = details;
    }

    private static String validateErrorCode(String errorCode) {
        return (errorCode != null && !errorCode.isEmpty()) ? errorCode : "PRICING_ENGINE_ERROR";
    }

    public String getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
