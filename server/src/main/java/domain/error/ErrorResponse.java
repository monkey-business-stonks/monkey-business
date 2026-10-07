package domain.error;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standardized error response for all API errors.
 * Matches test.yaml OpenAPI schema for security and simplicity.
 * Aligns with UI/client expectations for error display.
 */
public class ErrorResponse {
    private Integer status;        // HTTP status code (404, 401, 400, 409, 422, 500, 503, 504)
    private String error;          // Error type/description (e.g., "Not Found", "Unauthorized")
    private String message;        // User-friendly error message
    private String timestamp;      // ISO format timestamp

    // Default constructor
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Constructor with all fields
     */
    public ErrorResponse(Integer status, String error, String message) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // Builder-style setters for fluent API
    public ErrorResponse status(Integer status) {
        this.status = status;
        return this;
    }

    public ErrorResponse error(String error) {
        this.error = error;
        return this;
    }

    public ErrorResponse message(String message) {
        this.message = message;
        return this;
    }

    // Getters
    public Integer getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setStatus(Integer status) { this.status = status; }
    public void setError(String error) { this.error = error; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
