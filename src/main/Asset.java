package main;

public record Asset(String ticker, double quantity, double boughtAverage) {
    public Asset {
        if (ticker == null) throw new IllegalArgumentException("ticker cannot be null");
    }

    public Asset withQuantity(double quantity) {
        return new Asset(this.ticker, quantity, this.boughtAverage);
    }

    public Asset withBoughtAverage(double addedQuantity, double price) {
        double oldQty = this.quantity;
        double oldAvg = this.boughtAverage;
        double newQty = oldQty + addedQuantity;
        if (newQty <= 0) return new Asset(this.ticker, this.quantity, this.boughtAverage);
        double oldTotal = oldQty * oldAvg;
        double newTotal = oldTotal + addedQuantity * price;
        double newAvg = newTotal / newQty;
        return new Asset(this.ticker, newQty, newAvg);
    }
}