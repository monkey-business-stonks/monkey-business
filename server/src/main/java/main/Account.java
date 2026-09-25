package main;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {
	public enum AccountType { BROKERAGE, _401K, ROTH_IRA, CRYPTO, FOREX }

	@Id
	private final UUID accountID;
	private final ZonedDateTime openedDate;
	private final AccountType accountType;
	private BigDecimal balance;
	private BigDecimal cashBalance;
	
	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
	private final Set<Asset> heldAssets;
	
	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
	private final Set<Order> orderHistory;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

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

	// Returns associated user
	public User getUser() { return user; }
	// Sets associated user
	public void setUser(User user) { this.user = user; }

	// Returns all assets held
	public Set<Asset> getAllAssets() { return heldAssets; }
	public Asset getAsset(String ticker) { // Finds asset by ticker symbol
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
	
	// Calculates sum of cost basis for all holdings
	public BigDecimal getTotalInvestedAmount() {
		BigDecimal totalInvested = BigDecimal.ZERO;
		for (Asset asset : heldAssets) {
			totalInvested = totalInvested.add(asset.getCostBasis());
		}
		return totalInvested;
	}

	// Calculates current market value of all holdings
	public BigDecimal getPortfolioValue(Map<String, BigDecimal> currentPrices) {
		if (currentPrices == null) return BigDecimal.ZERO;
		BigDecimal portfolioValue = BigDecimal.ZERO;
		for (Asset asset : heldAssets) {
			BigDecimal price = currentPrices.get(asset.ticker());
			if (price != null && price.compareTo(BigDecimal.ZERO) >= 0) {
				portfolioValue = portfolioValue.add(asset.getMarketValue(price));
			}
		}
		return portfolioValue;
	}

	// Returns net worth (cash + portfolio value)
	public BigDecimal getNetWorth(Map<String, BigDecimal> currentPrices) {
		return cashBalance.add(getPortfolioValue(currentPrices));
	}

	//Checks if account has no assets and no orders
	public boolean isEmpty() {
		return heldAssets.isEmpty() && orderHistory.isEmpty();
	}

	public ZonedDateTime getOpenDate() { return openedDate; }
	public UUID getAccID() { return accountID; }
	public AccountType getAccType() { return accountType; }
}
