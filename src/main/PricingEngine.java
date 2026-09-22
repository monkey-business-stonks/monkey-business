package main;

import java.math.BigDecimal;

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
