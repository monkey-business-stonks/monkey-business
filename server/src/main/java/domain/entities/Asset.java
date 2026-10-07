package domain.entities;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "assets", uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "ticker"}))
public class Asset {
    @Id
    @Column(name = "assetid")
    private UUID assetId;
    
@Column(name = "accountid", insertable = false, updatable = false)
    private UUID accountId;
    
@Column(name = "assetclass")
	private String assetClass;
	
	@Column(name = "ticker")
	private String ticker;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "quantity")
	private Double quantity;
	
	@Column(name = "averagecost")
    private BigDecimal averageCost;
    
@Column(name = "createdat")
	private ZonedDateTime createdAt;
	
	@Column(name = "updatedat")
    private ZonedDateTime updatedAt;

    @ManyToOne
	@JoinColumn(name = "accountid")
	private Account account;

    // No-arg constructor for JPA
    public Asset() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // Full constructor
    public Asset(UUID assetId, String assetClass, String ticker, String name, Double quantity, BigDecimal averageCost) {
        if (assetId == null) throw new IllegalArgumentException("assetId cannot be null");
        if (assetClass == null) throw new IllegalArgumentException("assetClass cannot be null");
        if (ticker == null) throw new IllegalArgumentException("ticker cannot be null");
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (quantity == null || quantity < 0) 
            throw new IllegalArgumentException("quantity cannot be null or negative");
        if (averageCost == null || averageCost.compareTo(BigDecimal.ZERO) < 0) 
            throw new IllegalArgumentException("averageCost cannot be null or negative");
        
        this.assetId = assetId;
        this.assetClass = assetClass;
        this.ticker = ticker;
        this.name = name;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // Getters
    public UUID getAssetId() { return this.assetId; }
    public String getAssetClass() { return this.assetClass; }
    public String ticker() { return this.ticker; }
    public String getName() { return this.name; }
    public Double quantity() { return this.quantity; }
    public BigDecimal averageCost() { return this.averageCost; }
    public ZonedDateTime getCreatedAt() { return this.createdAt; }
    public ZonedDateTime getUpdatedAt() { return this.updatedAt; }
    public Account getAccount() { return this.account; }
    public String getId() { return this.assetId.toString(); }

    // Setters
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setAssetClass(String assetClass) { this.assetClass = assetClass; }
    public void setTicker(String ticker) { this.ticker = ticker; this.updatedAt = ZonedDateTime.now(); }
    public void setName(String name) { this.name = name; }
    public void setQuantity(Double quantity) { this.quantity = quantity; this.updatedAt = ZonedDateTime.now(); }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; this.updatedAt = ZonedDateTime.now(); }
    public void setAccount(Account account) { this.account = account; }
    public void setBoughtAverage(BigDecimal averageCost) { this.averageCost = averageCost; this.updatedAt = ZonedDateTime.now(); }

    // Creates a new Asset with updated quantity
    public Asset withQuantity(Double quantity) {
        return new Asset(this.assetId, this.assetClass, this.ticker, this.name, quantity, this.averageCost);
    }

    // Creates a new Asset with recalculated average cost basis
    public Asset withAverageCost(Double addedQuantity, BigDecimal price) {
        Double oldQty = this.quantity;
        BigDecimal oldAvg = this.averageCost;
        Double newQty = oldQty + addedQuantity;
        
        if (newQty <= 0) 
            return new Asset(this.assetId, this.assetClass, this.ticker, this.name, this.quantity, this.averageCost);
        
        // Calculate total cost: (old quantity × old avg) + (new shares × new price)
        BigDecimal oldTotal = oldAvg.multiply(BigDecimal.valueOf(oldQty));
        BigDecimal newTotal = oldTotal.add(price.multiply(BigDecimal.valueOf(addedQuantity)));
        
        // New average = total cost / total shares
        BigDecimal newAvg = newTotal.divide(BigDecimal.valueOf(newQty), 2, java.math.RoundingMode.HALF_UP);
        return new Asset(this.assetId, this.assetClass, this.ticker, this.name, newQty, newAvg);
    }
    
    // Backward compatibility method
    public Asset withBoughtAverage(Double addedQuantity, BigDecimal price) {
        return withAverageCost(addedQuantity, price);
    }

    // Returns total amount paid for all shares
    public BigDecimal getCostBasis() {
        return averageCost.multiply(BigDecimal.valueOf(quantity));
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
}
