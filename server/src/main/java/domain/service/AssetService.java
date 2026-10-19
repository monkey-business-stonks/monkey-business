package domain.service;

import domain.entities.Asset;
import domain.entities.Account;
import domain.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import domain.error.BusinessConflictException;

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
    @Transactional
    public Asset updateAssetOnSell(Account account, String ticker, Double quantity) {
        ticker = ticker.toUpperCase();

        int updatedRows = assetRepository.sellAsset(
            account.getAccID(), ticker, BigDecimal.valueOf(quantity)
        );
        if (updatedRows == 0) {
            throw new BusinessConflictException(
                "Insufficient holdings to sell " + quantity + " of " + ticker,
                "INSUFFICIENT_HOLDINGS"
            );
        }

        Asset asset = assetRepository.findByAccountAccountIdAndTicker(account.getAccID(), ticker)
            .orElse(null);

        if (asset == null || asset.quantity() == 0) {
            assetRepository.delete(asset);
            account.removeAsset(asset);
            return null;
        } else {
            return asset;
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
