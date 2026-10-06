package domain.error;

/**
 * Exception thrown when Asset entity encounters validation or state errors.
 * Covers invalid quantities, null tickers, invalid costs, etc.
 */
public class AssetException extends EntityException {
    
    public AssetException(String message) {
        super(message, "ASSET_ERROR");
    }

    public AssetException(String message, String errorCode) {
        super(message, errorCode);
    }

    public AssetException(String message, String errorCode, Object details) {
        super(message, errorCode, details);
    }

    // Convenience factory methods for common errors
    public static AssetException nullAssetId() {
        return new AssetException("Asset ID cannot be null", "ASSET_NULL_ID");
    }

    public static AssetException nullAssetClass() {
        return new AssetException("Asset class cannot be null", "ASSET_NULL_CLASS");
    }

    public static AssetException nullTicker() {
        return new AssetException("Ticker cannot be null", "ASSET_NULL_TICKER");
    }

    public static AssetException nullName() {
        return new AssetException("Asset name cannot be null", "ASSET_NULL_NAME");
    }

    public static AssetException invalidQuantity(double quantity) {
        return new AssetException(
            "Quantity cannot be null or negative: " + quantity,
            "ASSET_INVALID_QUANTITY",
            quantity
        );
    }

    public static AssetException invalidAverageCost(String cost) {
        return new AssetException(
            "Average cost cannot be null or negative: " + cost,
            "ASSET_INVALID_COST",
            cost
        );
    }

    public static AssetException invalidUpdate(String ticker, String reason) {
        return new AssetException(
            "Cannot update asset for " + ticker + ": " + reason,
            "ASSET_INVALID_UPDATE",
            new Object[]{ticker, reason}
        );
    }
}
