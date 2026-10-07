package domain.entities;

import domain.entities.Asset;
import domain.entities.Account;
import domain.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.UUID;

class AssetTest {
    private Asset asset;
    private UUID testAssetId;
    private Account testAccount;
    private ZonedDateTime now;

    @BeforeEach
    void setUp() {
        testAssetId = UUID.randomUUID();
        now = ZonedDateTime.now();
        
        // Create a test account
        User testUser = new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            "password123",
            "Test User",
            "555-1234",
            LocalDate.of(1990, 1, 1),
            User.AccessLevel.USER,
            new HashSet<>(),
            now,
            now
        );
        
        testAccount = new Account(
            UUID.randomUUID(),
            now,
            Account.AccountType.BROKERAGE,
            new BigDecimal("10000.00"),
            new BigDecimal("10000.00"),
            new HashSet<>(),
            new HashSet<>()
        );
        testAccount.setUser(testUser);
        
        asset = new Asset(
            testAssetId,
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(testAccount);
    }

    @Test
    void testAssetCreation() {
        assertEquals(testAssetId, asset.getAssetId(), "Asset ID should match");
        assertEquals("EQUITY", asset.getAssetClass(), "Asset class should be EQUITY");
        assertEquals("AAPL", asset.ticker(), "Ticker should be AAPL");
        assertEquals(10.0, asset.quantity(), "Quantity should be 10");
        assertEquals(new BigDecimal("150.00"), asset.averageCost(), "Average cost should be 150.00");
    }

    @Test
    void testAssetGettersAndSetters() {
        assertEquals(testAccount, asset.getAccount(), "Account should match");
        
        BigDecimal newCost = new BigDecimal("155.00");
        asset.setAverageCost(newCost);
        assertEquals(newCost, asset.averageCost(), "Average cost should be updated");
    }

    @Test
    void testGetCostBasis() {
        // Cost basis = quantity * averageCost = 10 * 150.00 = 1500.00
        BigDecimal costBasis = asset.getCostBasis();
        assertEquals(0, costBasis.compareTo(new BigDecimal("1500")), "Cost basis should be calculated correctly");
    }

    @Test
    void testGetMarketValue() {
        BigDecimal currentPrice = new BigDecimal("160.00");
        // Market value = quantity * currentPrice = 10 * 160.00 = 1600.00
        BigDecimal marketValue = asset.getMarketValue(currentPrice);
        assertEquals(0, marketValue.compareTo(new BigDecimal("1600")), "Market value should be calculated correctly");
    }

    @Test
    void testGetUnrealizedGainLoss() {
        BigDecimal currentPrice = new BigDecimal("160.00");
        // Unrealized P&L = marketValue - costBasis
        // = (10 * 160.00) - (10 * 150.00) = 1600.00 - 1500.00 = 100.00
        BigDecimal gainLoss = asset.getUnrealizedGainLoss(currentPrice);
        assertEquals(0, gainLoss.compareTo(new BigDecimal("100")), "Unrealized gain/loss should be calculated correctly");
    }

    @Test
    void testGetUnrealizedLoss() {
        BigDecimal currentPrice = new BigDecimal("140.00");
        // Unrealized P&L = marketValue - costBasis
        // = (10 * 140.00) - (10 * 150.00) = 1400.00 - 1500.00 = -100.00
        BigDecimal gainLoss = asset.getUnrealizedGainLoss(currentPrice);
        assertEquals(0, gainLoss.compareTo(new BigDecimal("-100")), "Unrealized loss should be calculated correctly");
    }

    @Test
    void testWithAverageCost() {
        // Adding 5 shares at 155.00
        Asset updatedAsset = asset.withAverageCost(5.0, new BigDecimal("155.00"));
        
        // New average cost = (10 * 150 + 5 * 155) / (10 + 5) = (1500 + 775) / 15 = 2275 / 15 = 151.67
        assertNotNull(updatedAsset, "Updated asset should not be null");
        assertEquals(15.0, updatedAsset.quantity(), "New quantity should be 15");
        // Check average cost is approximately 151.67
        BigDecimal expectedAvgCost = new BigDecimal("151.67");
        assertTrue(updatedAsset.averageCost().subtract(expectedAvgCost).abs().compareTo(new BigDecimal("0.01")) <= 0, 
            "Average cost should be approximately 151.67");
    }

    @Test
    void testWithBoughtAverage() {
        // Test backward compatibility method
        Asset updatedAsset = asset.withBoughtAverage(5.0, new BigDecimal("155.00"));
        
        assertNotNull(updatedAsset, "Updated asset should not be null");
        assertEquals(15.0, updatedAsset.quantity(), "New quantity should be 15");
    }

    @Test
    void testAssetWithZeroQuantity() {
        Asset zeroQuantityAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "MSFT",
            "Microsoft Corp.",
            0.0,
            new BigDecimal("300.00")
        );
        
        assertEquals(0.0, zeroQuantityAsset.quantity(), "Quantity can be zero");
        assertEquals(0, zeroQuantityAsset.getCostBasis().compareTo(BigDecimal.ZERO), "Cost basis of zero quantity should be zero");
    }

    @Test
    void testAssetPricingWithLargeNumbers() {
        Asset largeAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "BRK.A",
            "Berkshire Hathaway Inc.",
            10.0,
            new BigDecimal("500000.00")
        );
        
        BigDecimal costBasis = largeAsset.getCostBasis();
        assertEquals(0, costBasis.compareTo(new BigDecimal("5000000")), "Should handle large prices correctly");
    }

    @Test
    void testGetGainLossPercentage() {
        // Asset has 10 shares at average cost 150 = 1500 total cost
        BigDecimal currentPrice = new BigDecimal("165.00");
        // Market value = 10 * 165 = 1650
        // Gain/Loss = 1650 - 1500 = 150
        // Percentage = 150 / 1500 = 0.10 = 10%
        BigDecimal gainLossPercent = asset.getGainLossPercentage(currentPrice);
        assertEquals(0, gainLossPercent.compareTo(new BigDecimal("10.00")), "Gain/Loss percentage should be 10%");
    }

    @Test
    void testGetGainLossPercentageWithLoss() {
        // Asset has 10 shares at average cost 150 = 1500 total cost
        BigDecimal currentPrice = new BigDecimal("135.00");
        // Market value = 10 * 135 = 1350
        // Loss = 1350 - 1500 = -150
        // Percentage = -150 / 1500 = -0.10 = -10%
        BigDecimal gainLossPercent = asset.getGainLossPercentage(currentPrice);
        assertEquals(0, gainLossPercent.compareTo(new BigDecimal("-10.00")), "Gain/Loss percentage should be -10%");
    }

    @Test
    void testGetGainLossPercentageZeroCostBasis() {
        // Asset with zero quantity has zero cost basis
        Asset zeroAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "MSFT",
            "Microsoft Corp.",
            0.0,
            new BigDecimal("300.00")
        );
        
        BigDecimal gainLossPercent = zeroAsset.getGainLossPercentage(new BigDecimal("310.00"));
        assertEquals(0, gainLossPercent.compareTo(BigDecimal.ZERO), "Percentage with zero cost basis should be zero");
    }

    @Test
    void testAssetConstructorValidations() {
        // Test null assetId
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(null, "EQUITY", "AAPL", "Apple", 10.0, new BigDecimal("150.00"));
        }, "null assetId should throw exception");
        
        // Test null ticker
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(UUID.randomUUID(), "EQUITY", null, "Apple", 10.0, new BigDecimal("150.00"));
        }, "null ticker should throw exception");
        
        // Test null name
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(UUID.randomUUID(), "EQUITY", "AAPL", null, 10.0, new BigDecimal("150.00"));
        }, "null name should throw exception");
        
        // Test null quantity
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(UUID.randomUUID(), "EQUITY", "AAPL", "Apple", null, new BigDecimal("150.00"));
        }, "null quantity should throw exception");
        
        // Test negative quantity
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(UUID.randomUUID(), "EQUITY", "AAPL", "Apple", -5.0, new BigDecimal("150.00"));
        }, "negative quantity should throw exception");
        
        // Test null averageCost
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(UUID.randomUUID(), "EQUITY", "AAPL", "Apple", 10.0, null);
        }, "null averageCost should throw exception");
        
        // Test negative averageCost
        assertThrows(IllegalArgumentException.class, () -> {
            new Asset(UUID.randomUUID(), "EQUITY", "AAPL", "Apple", 10.0, new BigDecimal("-150.00"));
        }, "negative averageCost should throw exception");
    }

    @Test
    void testAssetEdgeCaseFractionalShares() {
        Asset fractionalAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "BRK.A",
            "Berkshire Hathaway Inc.",
            0.5,  // Half a share
            new BigDecimal("500000.00")
        );
        
        BigDecimal costBasis = fractionalAsset.getCostBasis();
        assertEquals(0, costBasis.compareTo(new BigDecimal("250000.00")), "Should handle fractional shares");
    }

    @Test
    void testWithAverageCostAddingZeroShares() {
        // Adding zero shares should return original asset
        Asset result = asset.withAverageCost(0.0, new BigDecimal("155.00"));
        
        assertEquals(10.0, result.quantity(), "Quantity should remain unchanged");
        assertEquals(0, result.averageCost().compareTo(asset.averageCost()), "Average cost should remain unchanged");
    }

    @Test
    void testWithAverageCostNegativeShares() {
        // Adding negative shares should return original asset values
        Asset result = asset.withAverageCost(-5.0, new BigDecimal("155.00"));
        
        // New qty = 10 + (-5) = 5
        assertEquals(5.0, result.quantity(), "Should handle negative quantity addition");
    }

    @Test
    void testGetGainLossPercentageSamePrice() {
        // When current price equals average cost, gain/loss should be 0%
        BigDecimal gainLossPercent = asset.getGainLossPercentage(asset.averageCost());
        assertEquals(0, gainLossPercent.compareTo(BigDecimal.ZERO), "Identical price should return 0% gain/loss");
    }

    @Test
    void testGetGainLossPercentageLargeGain() {
        // Cost basis = 10 * 150 = 1500
        // Current value = 10 * 300 = 3000
        // Gain = 1500 (1500/1500) * 100 = 100%
        BigDecimal gainLossPercent = asset.getGainLossPercentage(new BigDecimal("300.00"));
        
        assertEquals(0, gainLossPercent.compareTo(new BigDecimal("100")), "100% gain should be calculated correctly");
    }

    @Test
    void testWithAverageCostNegativePrice() {
        // Adding shares at negative price should be handled
        Asset result = asset.withAverageCost(5.0, new BigDecimal("-100.00"));
        
        assertNotNull(result, "Should handle negative price");
        assertEquals(15.0, result.quantity(), "Quantity should be updated");
    }

    @Test
    void testWithBoughtAverageBackwardCompatibility() {
        Asset result = asset.withBoughtAverage(3.0, new BigDecimal("145.00"));
        
        assertNotNull(result, "withBoughtAverage should work");
        assertEquals(13.0, result.quantity(), "Should add 3 shares to 10");
    }

    @Test
    void testFractionalShareCostBasis() {
        Asset fractionalAsset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "GOOG",
            "Google",
            0.1,
            new BigDecimal("500000.00")
        );
        
        BigDecimal costBasis = fractionalAsset.getCostBasis();
        assertEquals(0, costBasis.compareTo(new BigDecimal("50000.00")), "Should handle fractional shares correctly");
    }

    @Test
    void testMarketValueCalculation() {
        // Market value = quantity * currentPrice = 10 * 160 = 1600
        BigDecimal marketValue = asset.getMarketValue(new BigDecimal("160.00"));
        assertEquals(0, marketValue.compareTo(new BigDecimal("1600")), "Market value should be calculated correctly");
    }

    @Test
    void testMarketValueWithZeroPrice() {
        BigDecimal marketValue = asset.getMarketValue(BigDecimal.ZERO);
        assertEquals(0, marketValue.compareTo(BigDecimal.ZERO), "Market value with zero price should be zero");
    }

    @Test
    void testAssetWithZeroAverageCost() {
        Asset zeroAverageCost = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "FREE",
            "Free Stock",
            10.0,
            BigDecimal.ZERO
        );
        
        BigDecimal costBasis = zeroAverageCost.getCostBasis();
        assertEquals(0, costBasis.compareTo(BigDecimal.ZERO), "Zero average cost should result in zero cost basis");
    }
}