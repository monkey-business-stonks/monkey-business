package main;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Account {
	public enum AccountType { BROKERAGE, _401K, ROTH_IRA, CRYPTO, FOREX }

	private final UUID accountID;
	private final ZonedDateTime openedDate;
	private final AccountType accountType;
	private BigDecimal balance;
	private BigDecimal cashBalance;
	private final Set<Asset> heldAssets;
	private final Set<Order> orderHistory;

	public Account(UUID accountID, ZonedDateTime openedDate, AccountType accountType,
				   BigDecimal balance, BigDecimal cashBalance,
				   Set<Asset> heldAssets, Set<Order> orderHistory) {
		this.accountID = accountID;
		this.openedDate = openedDate;
		this.accountType = accountType;
		this.balance = balance == null ? BigDecimal.ZERO : balance;
		this.cashBalance = cashBalance == null ? BigDecimal.ZERO : cashBalance;
		this.heldAssets = heldAssets == null ? new LinkedHashSet<>() : heldAssets;
		this.orderHistory = orderHistory == null ? new LinkedHashSet<>() : orderHistory;
	}

	// Returns total account value as double
	public double getBalance() { return balance.doubleValue(); }
	// Sets total balance (validates non-negative)
	public void setBalance(double newBalance) {
		if (newBalance < 0) {
			throw new IllegalArgumentException("Balance cannot be negative");
		}
		this.balance = new BigDecimal(newBalance);
	}

	// Returns available cash
	public BigDecimal getCashBalance() { return cashBalance; }
	// Sets available cash (validates not null or negative)
	public void setCashBalance(BigDecimal cashBalance) {
		if (cashBalance == null) {
       		throw new IllegalArgumentException("Cash balance cannot be null");
    	}
    	if (cashBalance.compareTo(BigDecimal.ZERO) < 0) {
        	throw new IllegalArgumentException("Cash balance cannot be negative");
    	}
    	this.cashBalance = cashBalance;
	}

	// Returns all assets held
	public Set<Asset> getAllAssets() { return heldAssets; }
	// Finds asset by ticker symbol
	public Asset getAsset(String ticker) {
		if (ticker == null) return null;
		return heldAssets.stream().filter(a -> ticker.equals(a.ticker())).findFirst().orElse(null);
	}

	// Adds new asset to holdings
	public void addAsset(Asset asset) {
		if (asset == null) return;
		heldAssets.add(asset);
	}

	// Removes asset from holdings
	public void removeAsset(Asset asset) {
		if (asset == null) return;
		heldAssets.remove(asset);
	}

	// Updates existing asset with new data
	public void updateAsset(Asset asset){
		if (asset == null) return;
		heldAssets.removeIf(a -> a.ticker().equals(asset.ticker()));
		heldAssets.add(asset);
	}

	// Adds order to history
	public void addOrder(Order order) {
		if (order == null) return;
		orderHistory.add(order);
	}

	// ===== PORTFOLIO CALCULATIONS =====
	
	// Calculates sum of cost basis across all holdings
	public BigDecimal getTotalInvestedAmount() {
		BigDecimal totalInvested = BigDecimal.ZERO;
		for (Asset asset : heldAssets) {
			BigDecimal costBasis = BigDecimal.valueOf(asset.quantity())
				.multiply(BigDecimal.valueOf(asset.boughtAverage()));
			totalInvested = totalInvested.add(costBasis);
		}
		return totalInvested;
	}

	// Calculates current market value of all holdings
	public BigDecimal getPortfolioValue(Map<String, Double> currentPrices) {
		if (currentPrices == null) return BigDecimal.ZERO;
		BigDecimal portfolioValue = BigDecimal.ZERO;
		for (Asset asset : heldAssets) {
			Double price = currentPrices.get(asset.ticker());
			if (price != null && price >= 0) {
				BigDecimal marketValue = BigDecimal.valueOf(asset.quantity())
					.multiply(BigDecimal.valueOf(price));
				portfolioValue = portfolioValue.add(marketValue);
			}
		}
		return portfolioValue;
	}

	// Returns net worth (cash + portfolio value)
	public BigDecimal getNetWorth(Map<String, Double> currentPrices) {
		return cashBalance.add(getPortfolioValue(currentPrices));
	}

	// Checks if account has no assets and no orders
	public boolean isEmpty() {
		return heldAssets.isEmpty() && orderHistory.isEmpty();
	}

	// Returns when account was opened
	public ZonedDateTime getOpenDate() { return openedDate; }
	// Returns unique account ID
	public UUID getAccID() { return accountID; }
	// Returns account type
	public AccountType getAccType() { return accountType; }
}
