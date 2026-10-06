package domain.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all controllers.
 * Catches exceptions and returns standardized ErrorResponse.
 * 
 * Exception hierarchy:
 * - EntityException (base for all entity validation errors)
 *   - AccountException
 *   - OrderException
 *   - AssetException
 *   - UserException
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all entity validation exceptions with 400 Bad Request
     */
    @ExceptionHandler(EntityException.class)
    public ResponseEntity<ErrorResponse> handleEntityException(EntityException ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle account-specific exceptions
     */
    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorResponse> handleAccountException(AccountException ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle order-specific exceptions
     */
    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle asset-specific exceptions
     */
    @ExceptionHandler(AssetException.class)
    public ResponseEntity<ErrorResponse> handleAssetException(AssetException ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle user-specific exceptions
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            ex.getErrorCode(),
            ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            "GENERAL_ERROR",
            null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(
            "ERROR",
            ex.getMessage(),
            "ILLEGAL_ARGUMENT",
            null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
