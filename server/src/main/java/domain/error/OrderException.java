package domain.error;

/**
 * Exception thrown when Order entity encounters validation or state errors.
 * Covers invalid quantities, null tickers, invalid statuses, etc.
 */
public class OrderException extends EntityException {
    
    public OrderException(String message) {
        super(message, "ORDER_ERROR");
    }

    public OrderException(String message, String errorCode) {
        super(message, errorCode);
    }

    public OrderException(String message, String errorCode, Object details) {
        super(message, errorCode, details);
    }

    // Convenience factory methods for common errors
    public static OrderException nullOrderId() {
        return new OrderException("Order ID cannot be null", "ORDER_NULL_ID");
    }

    public static OrderException nullTicker() {
        return new OrderException("Ticker cannot be null or empty", "ORDER_NULL_TICKER");
    }

    public static OrderException invalidQuantity(double quantity) {
        return new OrderException(
            "Quantity must be positive: " + quantity,
            "ORDER_INVALID_QUANTITY",
            quantity
        );
    }

    public static OrderException nullAction() {
        return new OrderException("Action cannot be null", "ORDER_NULL_ACTION");
    }

    public static OrderException invalidAction(String action) {
        return new OrderException(
            "Action must be BUY or SELL: " + action,
            "ORDER_INVALID_ACTION",
            action
        );
    }

    public static OrderException nullOrderType() {
        return new OrderException("Order type cannot be null", "ORDER_NULL_TYPE");
    }

    public static OrderException invalidStatus(String status) {
        return new OrderException(
            "Invalid order status: " + status,
            "ORDER_INVALID_STATUS",
            status
        );
    }

    public static OrderException nullSubmittedOn() {
        return new OrderException("Submitted on timestamp cannot be null", "ORDER_NULL_SUBMITTED_ON");
    }

    public static OrderException negativeSubmittedValue(double value) {
        return new OrderException(
            "Submitted value cannot be negative: " + value,
            "ORDER_INVALID_SUBMITTED_VALUE",
            value
        );
    }
}
