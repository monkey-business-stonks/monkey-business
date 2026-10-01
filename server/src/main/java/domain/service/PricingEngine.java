package domain.service;

import java.math.BigDecimal;

import domain.entities.Asset;

public class PricingEngine {
    private final Asset asset;

    public PricingEngine(Asset asset) {
        this.asset = asset;
    }

    public Asset getAsset() {
        return asset;
    }

    public BigDecimal getCurrentPrice() {
        // TODO: implement pricing logic
        throw new UnsupportedOperationException("getCurrentPrice() not implemented");
    }
}
