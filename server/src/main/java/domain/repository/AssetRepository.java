package domain.repository;

import domain.entities.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    // Get asset position for account+ticker
    Optional<Asset> findByAccountAccountIDAndTicker(UUID accountID, String ticker);
    
    // Get specific asset
    Optional<Asset> findById(UUID assetId);
    
    // Get all assets for account, ordered by value (descending)
    @Query(value = "SELECT a.* FROM Assets a WHERE a.accountId = :accountId " +
           "ORDER BY (a.quantity * a.averageCost) DESC", 
           nativeQuery = true)
    List<Asset> findAssetsByAccountIdOrderByValue(@Param("accountId") UUID accountId);
    
    // Get assets by class (EQUITY, CRYPTO, FOREX)
    @Query(value = "SELECT a.* FROM Assets a WHERE a.accountId = :accountId " +
           "AND a.assetClass = :assetClass", 
           nativeQuery = true)
    List<Asset> findAssetsByAccountAndClass(@Param("accountId") UUID accountId, 
                                            @Param("assetClass") String assetClass);
    
    // Check if position exists
    @Query(value = "SELECT COUNT(*) > 0 FROM Assets WHERE accountId = :accountId " +
           "AND ticker = :ticker", 
           nativeQuery = true)
    boolean existsByAccountAndTicker(@Param("accountId") UUID accountId, 
                                     @Param("ticker") String ticker);
    
    // Get total value of all holdings
    @Query(value = "SELECT COALESCE(SUM(quantity * averageCost), 0) FROM Assets " +
           "WHERE accountId = :accountId", 
           nativeQuery = true)
    BigDecimal getTotalHoldingsValue(@Param("accountId") UUID accountId);
    
    // ===== ATOMIC OPERATIONS (for transaction safety) =====
    
    // Buy: upsert asset (insert or add to existing quantity)
    @Modifying
    @Query(value = "INSERT INTO Assets (assetId, accountId, assetClass, ticker, name, " +
           "averageCost, quantity, createdAt, updatedAt) " +
           "VALUES (:assetId, :accountId, :assetClass, :ticker, :name, :avgCost, :qty, " +
           "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
           "ON CONFLICT (accountId, ticker) DO UPDATE SET " +
           "quantity = quantity + :qty, averageCost = :avgCost, updatedAt = CURRENT_TIMESTAMP", 
           nativeQuery = true)
    void upsertAsset(@Param("assetId") UUID assetId, 
                     @Param("accountId") UUID accountId, 
                     @Param("assetClass") String assetClass, 
                     @Param("ticker") String ticker, 
                     @Param("name") String name, 
                     @Param("avgCost") BigDecimal avgCost, 
                     @Param("qty") BigDecimal qty);
    
    // Sell: reduce quantity (fails if quantity insufficient)
    @Modifying
    @Query(value = "UPDATE Assets SET quantity = quantity - :qty, updatedAt = CURRENT_TIMESTAMP " +
           "WHERE accountId = :accountId AND ticker = :ticker AND quantity >= :qty", 
           nativeQuery = true)
    int sellAsset(@Param("accountId") UUID accountId, 
                  @Param("ticker") String ticker, 
                  @Param("qty") BigDecimal qty);
    
    // Delete asset position (when quantity reaches zero)
    @Modifying
    @Query(value = "DELETE FROM Assets WHERE accountId = :accountId AND ticker = :ticker", 
           nativeQuery = true)
    int deleteAssetPosition(@Param("accountId") UUID accountId, 
                            @Param("ticker") String ticker);
}
