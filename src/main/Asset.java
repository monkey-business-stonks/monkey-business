package main;

import java.math.BigDecimal;

// Immutable record representing a single security holding (stock, crypto, etc.)
public record Asset(String ticker, BigDecimal quantity, BigDecimal boughtAverage) {
    public Asset {
        if (ticker == null) throw new IllegalArgumentException("ticker cannot be null");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) 
            throw new IllegalArgumentException("quantity cannot be null or negative");
        if (boughtAverage == null || boughtAverage.compareTo(BigDecimal.ZERO) < 0) 
            throw new IllegalArgumentException("boughtAverage cannot be null or negative");
    }

    // Creates a new Asset with updated quantity
    public Asset withQuantity(BigDecimal quantity) {
        return new Asset(this.ticker, quantity, this.boughtAverage);
    }

    // Creates a new Asset with recalculated average cost basis
    public Asset withBoughtAverage(BigDecimal addedQuantity, BigDecimal price) {
        BigDecimal oldQty = this.quantity;
        BigDecimal oldAvg = this.boughtAverage;
        BigDecimal newQty = oldQty.add(addedQuantity);
        
        if (newQty.compareTo(BigDecimal.ZERO) <= 0) 
            return new Asset(this.ticker, this.quantity, this.boughtAverage);
        
        // Calculate total cost: (old quantity × old avg) + (new shares × new price)
        BigDecimal oldTotal = oldQty.multiply(oldAvg);
        BigDecimal newTotal = oldTotal.add(addedQuantity.multiply(price));
        
        // New average = total cost / total shares
        BigDecimal newAvg = newTotal.divide(newQty, 2, java.math.RoundingMode.HALF_UP);
        return new Asset(this.ticker, newQty, newAvg);
    }

    // Returns total amount paid for all shares
    public BigDecimal getCostBasis() {
        return quantity.multiply(boughtAverage);
    }

    // Returns current market value at given price
    public BigDecimal getMarketValue(BigDecimal currentPrice) {
        return quantity.multiply(currentPrice);
    }

    // Returns profit/loss in dollars
    public BigDecimal getUnrealizedGainLoss(BigDecimal currentPrice) {
        return getMarketValue(currentPrice).subtract(getCostBasis());
    }

    // Returns profit/loss as percentage
    public BigDecimal getGainLossPercentage(BigDecimal currentPrice) {
        BigDecimal costBasis = getCostBasis();
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getUnrealizedGainLoss(currentPrice)
            .divide(costBasis, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal(100));
    }

    @Override
    public String toString() {
        return String.format("%s: %s shares @ $%s avg (cost basis: $%s)", 
            ticker, quantity, boughtAverage, getCostBasis());
    }

    // Returns true if quantity is zero
    public boolean isEmpty() {
        return quantity.compareTo(BigDecimal.ZERO) == 0;
    }
}