package main;

public record Asset(String ticker, double quantity, double boughtAverage) {
    public Asset {
        if (ticker == null) throw new IllegalArgumentException("ticker cannot be null");
        if (quantity < 0) throw new IllegalArgumentException("quantity cannot be negative");
        if (boughtAverage < 0) throw new IllegalArgumentException("boughtAverage cannot be negative");
    }

    // Creates a new Asset with updated quantity
    public Asset withQuantity(double quantity) {
        return new Asset(this.ticker, quantity, this.boughtAverage);
    }

    // Creates a new Asset with recalculated average cost basis
    public Asset withBoughtAverage(double addedQuantity, double price) {
        double oldQty = this.quantity;
        double oldAvg = this.boughtAverage;
        double newQty = oldQty + addedQuantity;
        if (newQty <= 0) return new Asset(this.ticker, this.quantity, this.boughtAverage);
        
        // Calculate total cost: (old quantity × old avg) + (new shares × new price)
        double oldTotal = oldQty * oldAvg;
        double newTotal = oldTotal + addedQuantity * price;
        
        // New average = total cost / total shares
        double newAvg = newTotal / newQty;
        return new Asset(this.ticker, newQty, newAvg);
    }

    // Returns total amount paid for all shares
    public double getCostBasis() {
        return quantity * boughtAverage;
    }

    // Returns current market value at given price
    public double getMarketValue(double currentPrice) {
        return quantity * currentPrice;
    }

    // Returns profit/loss in dollars
    public double getUnrealizedGainLoss(double currentPrice) {
        return getMarketValue(currentPrice) - getCostBasis();
    }

    // Returns profit/loss as percentage
    public double getGainLossPercentage(double currentPrice) {
        double costBasis = getCostBasis();
        if (costBasis == 0) return 0;
        return (getUnrealizedGainLoss(currentPrice) / costBasis) * 100;
    }

    @Override
    public String toString() {
        return String.format("%s: %.2f shares @ $%.2f avg (cost basis: $%.2f)", 
            ticker, quantity, boughtAverage, getCostBasis());
    }

    // Returns true if quantity is zero
    public boolean isEmpty() {
        return quantity == 0;
    }
}