package main;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String ticker;
    
    @Column(nullable = false)
    private Double quantity;
    
    @Column(nullable = false)
    private BigDecimal boughtAverage;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // No-arg constructor for JPA
    public Asset() {}

    // Full constructor
    public Asset(String ticker, Double quantity, BigDecimal boughtAverage) {
        if (ticker == null) throw new IllegalArgumentException("ticker cannot be null");
        if (quantity == null || quantity < 0) 
            throw new IllegalArgumentException("quantity cannot be null or negative");
        if (boughtAverage == null || boughtAverage.compareTo(BigDecimal.ZERO) < 0) 
            throw new IllegalArgumentException("boughtAverage cannot be null or negative");
        
        this.ticker = ticker;
        this.quantity = quantity;
        this.boughtAverage = boughtAverage;
    }

    // Getters
    public String getId() { return this.id; }
    public String ticker() { return this.ticker; }
    public Double quantity() { return this.quantity; }
    public BigDecimal boughtAverage() { return this.boughtAverage; }
    public Account getAccount() { return this.account; }

    // Setters
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public void setBoughtAverage(BigDecimal boughtAverage) { this.boughtAverage = boughtAverage; }
    public void setAccount(Account account) { this.account = account; }

    // Creates a new Asset with updated quantity
    public Asset withQuantity(Double quantity) {
        return new Asset(this.ticker, quantity, this.boughtAverage);
    }

    // Creates a new Asset with recalculated average cost basis
    public Asset withBoughtAverage(Double addedQuantity, BigDecimal price) {
        Double oldQty = this.quantity;
        BigDecimal oldAvg = this.boughtAverage;
        Double newQty = oldQty + addedQuantity;
        
        if (newQty <= 0) 
            return new Asset(this.ticker, this.quantity, this.boughtAverage);
        
        // Calculate total cost: (old quantity × old avg) + (new shares × new price)
        BigDecimal oldTotal = oldAvg.multiply(BigDecimal.valueOf(oldQty));
        BigDecimal newTotal = oldTotal.add(price.multiply(BigDecimal.valueOf(addedQuantity)));
        
        // New average = total cost / total shares
        BigDecimal newAvg = newTotal.divide(BigDecimal.valueOf(newQty), 2, java.math.RoundingMode.HALF_UP);
        return new Asset(this.ticker, newQty, newAvg);
    }

    // Returns total amount paid for all shares
    public BigDecimal getCostBasis() {
        return boughtAverage.multiply(BigDecimal.valueOf(quantity));
    }

    // Returns current market value at given price
    public BigDecimal getMarketValue(BigDecimal currentPrice) {
        return currentPrice.multiply(BigDecimal.valueOf(quantity));
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
        return quantity == 0;
    }
}