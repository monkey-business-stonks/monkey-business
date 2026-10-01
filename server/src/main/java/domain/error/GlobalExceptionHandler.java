package domain.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all controllers.
 * Catches exceptions and returns standardized ErrorResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGeneralException(Exception ex) {
        return new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            "GENERAL_ERROR",
            null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            "ILLEGAL_ARGUMENT",
            null
        );
    }
}
