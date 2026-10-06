import domain.entities.Account;
import domain.entities.Asset;
import domain.entities.Order;
import domain.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class AccountTest {
    private Account account;
    private UUID testAccountId;
    private User testUser;
    private ZonedDateTime now;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        now = ZonedDateTime.now();
        
        // Create a test user
        testUser = new User(
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
        
        account = new Account(
            testAccountId,
            now,
            Account.AccountType.BROKERAGE,
            new BigDecimal("10000.00"),
            new BigDecimal("10000.00"),
            new HashSet<>(),
            new HashSet<>()
        );
        account.setUser(testUser);
    }

    @Test
    void testAccountCreation() {
        assertEquals(testAccountId, account.getAccID(), "Account ID should match");
        assertEquals(Account.AccountType.BROKERAGE, account.getAccountType(), "Account type should be BROKERAGE");
        assertEquals(new BigDecimal("10000.00").compareTo(account.getCashBalance()), 0, "Balance should match");
        assertEquals(new BigDecimal("10000.00").compareTo(account.getCashBalance()), 0, "Cash balance should match");
    }

    @Test
    void testAccountGettersAndSetters() {
        assertEquals(testUser, account.getUser(), "User should match");
        
        account.setBalance(15000.0);
        assertEquals(15000.0, account.getBalance(), "Balance should be updated");
    }

    @Test
    void testUpdateCashBalance() {
        BigDecimal newCash = new BigDecimal("8000.00");
        account.setCashBalance(newCash);
        assertEquals(newCash, account.getCashBalance(), "Cash balance should be updated");
    }

    @Test
    void testUpdateBalance() {
        account.setBalance(12000.0);
        assertEquals(12000.0, account.getBalance(), "Balance should be updated");
    }

    @Test
    void testAddAsset() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        
        account.addAsset(asset);
        assertTrue(account.getAllAssets().contains(asset), "Asset should be added to account");
    }

    @Test
    void testGetAsset() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        
        account.addAsset(asset);
        Asset retrievedAsset = account.getAsset("AAPL");
        assertNotNull(retrievedAsset, "Asset should be retrieved");
        assertEquals("AAPL", retrievedAsset.ticker(), "Ticker should match");
    }

    @Test
    void testRemoveAsset() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        
        account.addAsset(asset);
        account.removeAsset(asset);
        assertFalse(account.getAllAssets().contains(asset), "Asset should be removed from account");
    }

    @Test
    void testUpdateAsset() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        
        account.addAsset(asset);
        
        Asset updatedAsset = new Asset(
            asset.getAssetId(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            15.0,
            new BigDecimal("155.00")
        );
        updatedAsset.setAccount(account);
        
        account.updateAsset(updatedAsset);
        Asset retrieved = account.getAsset("AAPL");
        assertEquals(15.0, retrieved.quantity(), "Quantity should be updated");
    }

    @Test
    void testGetTotalInvestedAmount() {
        Asset asset1 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        Asset asset2 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "GOOGL",
            "Google Inc.",
            5.0,
            new BigDecimal("100.00")
        );
        asset1.setAccount(account);
        asset2.setAccount(account);
        
        account.addAsset(asset1);
        account.addAsset(asset2);
        
        BigDecimal totalInvested = account.getTotalInvestedAmount();
        // (10 * 150) + (5 * 100) = 1500 + 500 = 2000
        assertEquals(0, totalInvested.compareTo(new BigDecimal("2000")), "Total invested should be calculated correctly");
    }

    @Test
    void testAddOrder() {
        Order order = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            10.0,
            "BUY",
            now,
            new BigDecimal("1500.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        
        account.addOrder(order);
        // Account has orderHistory but no getOrders() method, verify through isEmpty()
        assertFalse(account.isEmpty(), "Account should not be empty after adding order");
    }

    @Test
    void testGetAllAssets() {
        Asset asset1 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        Asset asset2 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "GOOGL",
            "Google Inc.",
            5.0,
            new BigDecimal("100.00")
        );
        asset1.setAccount(account);
        asset2.setAccount(account);
        
        account.addAsset(asset1);
        account.addAsset(asset2);
        
        Set<Asset> allAssets = account.getAllAssets();
        assertEquals(2, allAssets.size(), "Should have 2 assets");
        assertTrue(allAssets.contains(asset1), "Should contain AAPL");
        assertTrue(allAssets.contains(asset2), "Should contain GOOGL");
    }

    @Test
    void testIsEmptyWithAssets() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        assertFalse(account.isEmpty(), "Account with assets should not be empty");
    }

    @Test
    void testIsEmptyWithOrders() {
        Order order = new Order(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            10.0,
            "BUY",
            now,
            new BigDecimal("1500.00"),
            Order.OrderStatus.FILLED,
            201,
            now
        );
        account.addOrder(order);
        
        assertFalse(account.isEmpty(), "Account with orders should not be empty");
    }

    @Test
    void testIsEmptyNewAccount() {
        assertTrue(account.isEmpty(), "New account with no assets or orders should be empty");
    }

    @Test
    void testGetPortfolioValue() {
        Asset asset1 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")  // cost basis 1500
        );
        Asset asset2 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "GOOGL",
            "Google Inc.",
            5.0,
            new BigDecimal("100.00")   // cost basis 500
        );
        asset1.setAccount(account);
        asset2.setAccount(account);
        
        account.addAsset(asset1);
        account.addAsset(asset2);
        
        // Current prices: AAPL at 160, GOOGL at 110
        java.util.Map<String, BigDecimal> prices = new java.util.HashMap<>();
        prices.put("AAPL", new BigDecimal("160.00"));
        prices.put("GOOGL", new BigDecimal("110.00"));
        
        BigDecimal portfolioValue = account.getPortfolioValue(prices);
        // (10 * 160) + (5 * 110) = 1600 + 550 = 2150
        assertEquals(0, portfolioValue.compareTo(new BigDecimal("2150")), "Portfolio value should match");
    }

    @Test
    void testGetNetWorth() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        java.util.Map<String, BigDecimal> prices = new java.util.HashMap<>();
        prices.put("AAPL", new BigDecimal("160.00"));
        
        BigDecimal netWorth = account.getNetWorth(prices);
        // Cash (10000) + Portfolio (10 * 160 = 1600) = 11600
        assertEquals(0, netWorth.compareTo(new BigDecimal("11600")), "Net worth should be cash + portfolio value");
    }

    @Test
    void testSetBalanceValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            account.setBalance(-1000.0);
        }, "Setting negative balance should throw exception");
    }

    @Test
    void testSetCashBalanceValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            account.setCashBalance(new BigDecimal("-1000.00"));
        }, "Setting negative cash balance should throw exception");
    }

    @Test
    void testSetCashBalanceNullValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            account.setCashBalance(null);
        }, "Setting null cash balance should throw exception");
    }

    @Test
    void testGetPortfolioValueWithNullPrices() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        BigDecimal portfolioValue = account.getPortfolioValue(null);
        assertEquals(0, portfolioValue.compareTo(BigDecimal.ZERO), "Portfolio value with null prices should be zero");
    }

    @Test
    void testGetPortfolioValueWithMissingPrices() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        // Only provide price for GOOGL, not AAPL
        java.util.Map<String, BigDecimal> prices = new java.util.HashMap<>();
        prices.put("GOOGL", new BigDecimal("110.00"));
        
        BigDecimal portfolioValue = account.getPortfolioValue(prices);
        assertEquals(0, portfolioValue.compareTo(BigDecimal.ZERO), "Portfolio value should skip assets with missing prices");
    }

    @Test
    void testGetAssetNullTicker() {
        Asset retrieved = account.getAsset(null);
        assertNull(retrieved, "Getting asset with null ticker should return null");
    }

    @Test
    void testGetAssetNonExistent() {
        Asset retrieved = account.getAsset("NONEXISTENT");
        assertNull(retrieved, "Getting non-existent asset should return null");
    }

    @Test
    void testGetAssetAfterAdding() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "MSFT",
            "Microsoft",
            5.0,
            new BigDecimal("300.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        Asset retrieved = account.getAsset("MSFT");
        assertNotNull(retrieved, "Should retrieve added asset");
        assertEquals("MSFT", retrieved.ticker(), "Retrieved asset ticker should match");
    }

    @Test
    void testAddNullAsset() {
        int sizeBefore = account.getAllAssets().size();
        account.addAsset(null);
        assertEquals(sizeBefore, account.getAllAssets().size(), "Adding null asset should be ignored");
    }

    @Test
    void testRemoveNullAsset() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        account.removeAsset(null);
        assertEquals(1, account.getAllAssets().size(), "Removing null asset should not affect collection");
    }

    @Test
    void testUpdateAssetNullHandling() {
        Asset asset = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset.setAccount(account);
        account.addAsset(asset);
        
        account.updateAsset(null);
        assertEquals(1, account.getAllAssets().size(), "Updating with null should be ignored");
    }

    @Test
    void testAddOrderNullHandling() {
        account.addOrder(null);
        assertTrue(account.isEmpty(), "Adding null order should not affect isEmpty");
    }

    @Test
    void testGetNetWorthWithNullPrices() {
        account.setCashBalance(new BigDecimal("5000.00"));
        
        BigDecimal netWorth = account.getNetWorth(null);
        assertEquals(0, netWorth.compareTo(new BigDecimal("5000.00")), "Net worth with null prices should equal cash balance");
    }

    @Test
    void testUpdateAssetReplacementByTicker() {
        Asset asset1 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            10.0,
            new BigDecimal("150.00")
        );
        asset1.setAccount(account);
        account.addAsset(asset1);
        
        // Update with new asset for same ticker
        Asset asset2 = new Asset(
            UUID.randomUUID(),
            "EQUITY",
            "AAPL",
            "Apple Inc.",
            20.0,
            new BigDecimal("155.00")
        );
        asset2.setAccount(account);
        account.updateAsset(asset2);
        
        assertEquals(1, account.getAllAssets().size(), "Should have 1 asset after update");
        Asset retrieved = account.getAsset("AAPL");
        assertEquals(20.0, retrieved.quantity(), "Asset should be updated with new quantity");
    }
}