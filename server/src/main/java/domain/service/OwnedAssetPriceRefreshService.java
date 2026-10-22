package domain.service;

import domain.entities.Asset;
import domain.entities.Account;
import domain.repository.AccountRepository;
import domain.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnedAssetPriceRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(OwnedAssetPriceRefreshService.class);

    private final AssetRepository assetRepository;
    private final AccountRepository accountRepository;
    private final PricingEngine pricingEngine;

    public OwnedAssetPriceRefreshService(
        AssetRepository assetRepository,
        AccountRepository accountRepository,
        PricingEngine pricingEngine
    ) {
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
        this.pricingEngine = pricingEngine;
    }

    @Scheduled(
        fixedRateString = "${market-data.refresh-interval-ms:300000}",
        initialDelayString = "${market-data.refresh-initial-delay-ms:10000}"
    )
    @Transactional
    public void refreshOwnedAssetPrices() {
        List<Asset> ownedAssets = assetRepository.findAll().stream()
            .filter(asset -> asset.quantity() != null && asset.quantity() > 0)
            .toList();
        List<String> symbols = ownedAssets.stream()
            .map(Asset::ticker)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (symbols.isEmpty()) {
            logger.debug("Skipping owned-asset price refresh: no holdings found");
            return;
        }

        try {
            Map<String, BigDecimal> refreshedPrices = pricingEngine.refreshPrices(symbols);
            if (refreshedPrices.size() != symbols.size()) {
                logger.warn(
                    "Skipping balance persistence because only {}/{} owned symbols were refreshed",
                    refreshedPrices.size(), symbols.size()
                );
                return;
            }

            Map<java.util.UUID, BigDecimal> portfolioValues = ownedAssets.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Asset::getAccountId,
                    java.util.stream.Collectors.reducing(
                        BigDecimal.ZERO,
                        asset -> refreshedPrices.get(asset.ticker())
                            .multiply(BigDecimal.valueOf(asset.quantity())),
                        BigDecimal::add
                    )
                ));

            for (Account account : accountRepository.findAll()) {
                BigDecimal portfolioValue = portfolioValues.getOrDefault(account.getAccID(), BigDecimal.ZERO);
                BigDecimal totalBalance = account.getCashBalance().add(portfolioValue);
                accountRepository.updateBalance(account.getAccID(), totalBalance);
            }

            logger.info("Refreshed market prices and persisted balances for {} owned symbols", symbols.size());
        } catch (Exception exception) {
            logger.error("Owned-asset price refresh failed; next schedule will retry", exception);
        }
    }
}