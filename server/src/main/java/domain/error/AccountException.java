package domain.error;

/**
 * Exception thrown when Account entity encounters validation or state errors.
 * Covers invalid balances, null IDs, invalid types, etc.
 */
public class AccountException extends EntityException {
    
    public AccountException(String message) {
        super(message, "ACCOUNT_ERROR");
    }

    public AccountException(String message, String errorCode) {
        super(message, errorCode);
    }

    public AccountException(String message, String errorCode, Object details) {
        super(message, errorCode, details);
    }

    // Convenience factory methods for common errors
    public static AccountException nullAccountId() {
        return new AccountException("Account ID cannot be null", "ACCOUNT_NULL_ID");
    }

    public static AccountException nullUserId() {
        return new AccountException("User ID cannot be null", "ACCOUNT_NULL_USER_ID");
    }

    public static AccountException nullAccountType() {
        return new AccountException("Account type cannot be null", "ACCOUNT_NULL_TYPE");
    }

    public static AccountException invalidBalance(double balance) {
        return new AccountException(
            "Balance cannot be negative: " + balance, 
            "ACCOUNT_INVALID_BALANCE",
            balance
        );
    }

    public static AccountException invalidCashBalance(String balance) {
        return new AccountException(
            "Cash balance cannot be null or negative: " + balance,
            "ACCOUNT_INVALID_CASH_BALANCE",
            balance
        );
    }

    public static AccountException cashExceedsBalance(double cash, double balance) {
        return new AccountException(
            "Cash balance (" + cash + ") cannot exceed total balance (" + balance + ")",
            "ACCOUNT_CASH_EXCEEDS_BALANCE",
            new Object[]{cash, balance}
        );
    }
}
