package main.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "assets")
public class AssetEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "asset_id", columnDefinition = "UUID")
    private UUID assetId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;
    
    @Column(name = "ticker", nullable = false)
    private String ticker;
    
    @Column(name = "quantity", nullable = false)
    private Double quantity;
    
    @Column(name = "bought_average", nullable = false, precision = 19, scale = 2)
    private BigDecimal boughtAverage;

    // Constructors
    public AssetEntity() {}

    public AssetEntity(AccountEntity account, String ticker, Double quantity, BigDecimal boughtAverage) {
        this.account = account;
        this.ticker = ticker;
        this.quantity = quantity;
        this.boughtAverage = boughtAverage;
    }

    // Getters and Setters
    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }

    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public BigDecimal getBoughtAverage() { return boughtAverage; }
    public void setBoughtAverage(BigDecimal boughtAverage) { this.boughtAverage = boughtAverage; }

    // Business logic methods from Asset record
    public BigDecimal getCostBasis() {
        return boughtAverage.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getMarketValue(BigDecimal currentPrice) {
        return currentPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getUnrealizedGainLoss(BigDecimal currentPrice) {
        return getMarketValue(currentPrice).subtract(getCostBasis());
    }
}
