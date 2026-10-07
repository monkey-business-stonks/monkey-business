package domain.service;

import domain.entities.Asset;
import domain.entities.Account;
import domain.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    /**
     * Update or create asset for a BUY order
     * If asset exists for this account + ticker, updates quantity and average cost
     * If not, creates new asset
     * 
     * CRITICAL: This provides explicit deduplication at the database level
     */
    public Asset updateAssetOnBuy(Account account, String ticker, Double quantity, BigDecimal unitPrice) {
        ticker = ticker.toUpperCase();
        
        // Query database to see if asset exists for this account + ticker
        Optional<Asset> existing = assetRepository.findByAccountAccountIdAndTicker(account.getAccID(), ticker);

        if (existing.isPresent()) {
            // Asset exists - update it with new average cost basis
            Asset asset = existing.get();
            
            // Calculate new average cost: (old_qty * old_avg + new_qty * new_price) / new_qty
            BigDecimal oldTotal = asset.averageCost().multiply(BigDecimal.valueOf(asset.quantity()));
            BigDecimal newTotal = oldTotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            Double newQuantity = asset.quantity() + quantity;
            BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(newQuantity), 2, java.math.RoundingMode.HALF_UP);
            
            // Update the asset
            asset.setQuantity(newQuantity);
            asset.setBoughtAverage(newAverage);
            return assetRepository.save(asset);
        } else {
            // Asset doesn't exist - create new one
            Asset newAsset = new Asset(
                java.util.UUID.randomUUID(),
                "EQUITY", // assetClass - TODO: determine from ticker/market data
                ticker,
                ticker, // name - use ticker as placeholder
                quantity,
                unitPrice
            );
            newAsset.setAccount(account);
            return assetRepository.save(newAsset);
        }
    }

    /**
     * Update asset for a SELL order
     * Reduces quantity and removes asset if quantity becomes 0 or negative
     */
    public void updateAssetOnSell(Account account, String ticker, Double quantity) {
        ticker = ticker.toUpperCase();
        
        Optional<Asset> existing = assetRepository.findByAccountAccountIdAndTicker(account.getAccID(), ticker);

        if (existing.isPresent()) {
            Asset asset = existing.get();
            Double newQuantity = asset.quantity() - quantity;
            
            if (newQuantity <= 0) {
                // Remove asset if quantity is 0 or negative
                assetRepository.delete(asset);
                account.removeAsset(asset);
            } else {
                // Update asset with new quantity
                asset.setQuantity(newQuantity);
                assetRepository.save(asset);
            }
        }
    }

    /**
     * Get asset for account and ticker
     */
    public Asset getAsset(Account account, String ticker) {
        return assetRepository.findByAccountAccountIdAndTicker(account.getAccID(), ticker.toUpperCase())
            .orElse(null);
    }
}
