package domain.error;

/**
 * Exception thrown when User entity encounters validation or state errors.
 * Covers invalid usernames, null emails, invalid access levels, etc.
 */
public class UserException extends EntityException {
    
    public UserException(String message) {
        super(message, "USER_ERROR");
    }

    public UserException(String message, String errorCode) {
        super(message, errorCode);
    }

    public UserException(String message, String errorCode, Object details) {
        super(message, errorCode, details);
    }

    // Convenience factory methods for common errors
    public static UserException nullUserId() {
        return new UserException("User ID cannot be null", "USER_NULL_ID");
    }

    public static UserException nullUsername() {
        return new UserException("Username cannot be null or empty", "USER_NULL_USERNAME");
    }

    public static UserException nullEmail() {
        return new UserException("Email cannot be null or empty", "USER_NULL_EMAIL");
    }

    public static UserException invalidEmail(String email) {
        return new UserException(
            "Invalid email format: " + email,
            "USER_INVALID_EMAIL",
            email
        );
    }

    public static UserException nullPassword() {
        return new UserException("Password cannot be null or empty", "USER_NULL_PASSWORD");
    }

    public static UserException invalidPasswordLength(int length) {
        return new UserException(
            "Password must be at least 8 characters, got " + length,
            "USER_INVALID_PASSWORD_LENGTH",
            length
        );
    }

    public static UserException nullAccessLevel() {
        return new UserException("Access level cannot be null", "USER_NULL_ACCESS_LEVEL");
    }

    public static UserException invalidAccessLevel(String level) {
        return new UserException(
            "Invalid access level: " + level,
            "USER_INVALID_ACCESS_LEVEL",
            level
        );
    }
}
