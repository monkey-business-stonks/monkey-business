package domain.entities;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.*;
import domain.error.AccountException;

@Entity
@Table(name = "accounts")
public class Account {
	public enum AccountType { BROKERAGE, _401K, ROTH_IRA, CRYPTO, FOREX }

	@Id
	@Column(name = "accountid")
	private UUID accountId;
	
	@Column(name = "userid")
	private UUID userId;
	
	@Column(name = "createdon")
	private ZonedDateTime createdOn;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "accounttype")
	private AccountType accountType;
	
	@Column(name = "balance")
	private BigDecimal balance;
	
	@Column(name = "cashbalance")
	private BigDecimal cashBalance;
	
	@Column(name = "updatedat")
	private ZonedDateTime updatedAt;
	
	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<Asset> heldAssets;
	
	@OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<Order> orderHistory;

	@ManyToOne
	@JoinColumn(name = "userid", insertable=false, updatable=false)
	private User user;

	// No-arg constructor for JPA
	protected Account() {
		this.accountId = null;
		this.createdOn = null;
		this.accountType = null;
		this.balance = BigDecimal.ZERO;
		this.cashBalance = BigDecimal.ZERO;
		this.updatedAt = ZonedDateTime.now();
		this.heldAssets = new LinkedHashSet<>();
		this.orderHistory = new LinkedHashSet<>();
	}

	public Account(UUID accountId, ZonedDateTime createdOn, AccountType accountType,
				   BigDecimal balance, BigDecimal cashBalance,
				   Set<Asset> heldAssets, Set<Order> orderHistory) {
		// Validate required fields
		if (accountId == null) {
			throw AccountException.nullAccountId();
		}
		if (createdOn == null) {
			throw new AccountException("Created on timestamp cannot be null", "ACCOUNT_NULL_CREATED_ON");
		}
		if (accountType == null) {
			throw AccountException.nullAccountType();
		}

		// Validate balance
		BigDecimal validatedBalance = balance == null ? BigDecimal.ZERO : balance;
		if (validatedBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw AccountException.invalidBalance(validatedBalance.doubleValue());
		}

		// Validate cash balance
		BigDecimal validatedCashBalance = cashBalance == null ? BigDecimal.ZERO : cashBalance;
		if (validatedCashBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw AccountException.invalidCashBalance(validatedCashBalance.toPlainString());
		}

		// Cash balance cannot exceed total balance
		if (validatedCashBalance.compareTo(validatedBalance) > 0) {
			throw AccountException.cashExceedsBalance(
				validatedCashBalance.doubleValue(),
				validatedBalance.doubleValue()
			);
		}

		this.accountId = accountId;
		this.createdOn = createdOn;
		this.accountType = accountType;
		this.balance = validatedBalance;
		this.cashBalance = validatedCashBalance;
		this.updatedAt = ZonedDateTime.now();
		this.heldAssets = heldAssets == null ? new LinkedHashSet<>() : heldAssets;
		this.orderHistory = orderHistory == null ? new LinkedHashSet<>() : orderHistory;
	}

	// Returns account ID
	public UUID getAccountId() { return accountId; }
	// Returns account type
	public AccountType getAccountType() { return accountType; }
	// Returns created date
	public ZonedDateTime getCreatedOn() { return createdOn; }
	// Returns updated date
	public ZonedDateTime getUpdatedAt() { return updatedAt; }
	
	// Alias methods for compatibility
	public UUID getAccountID() { return accountId; }
	public UUID getAccID() { return accountId; }
	public AccountType getAccType() { return accountType; }

	// Returns total account value as double
	public double getBalance() { return balance.doubleValue(); }
	// Sets total balance (validates non-negative)
	public void setBalance(double newBalance) {
		if (newBalance < 0) {
			throw AccountException.invalidBalance(newBalance);
		}
		BigDecimal newBalanceBD = new BigDecimal(newBalance);
		// Cash balance cannot exceed new total balance
		if (cashBalance.compareTo(newBalanceBD) > 0) {
			throw AccountException.cashExceedsBalance(cashBalance.doubleValue(), newBalance);
		}
		this.balance = newBalanceBD;
		this.updatedAt = ZonedDateTime.now();
	}

	// Returns available cash
	public BigDecimal getCashBalance() { return cashBalance; }
	// Sets available cash (validates not null or negative)
	public void setCashBalance(BigDecimal cashBalance) {
		if (cashBalance == null) {
			throw AccountException.invalidCashBalance("null");
		}
		if (cashBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw AccountException.invalidCashBalance(cashBalance.toPlainString());
		}
		// Cash balance cannot exceed total balance
		if (cashBalance.compareTo(balance) > 0) {
			throw AccountException.cashExceedsBalance(
				cashBalance.doubleValue(),
				balance.doubleValue()
			);
		}
		this.cashBalance = cashBalance;
		this.updatedAt = ZonedDateTime.now();
	}

	// Returns associated user
	public User getUser() { return user; }
	// Sets associated user
	public void setUser(User user) { 
		this.user = user;
		if (user != null) {
			this.userId = user.getUserId();
		}
	}
	
	// Returns user ID
	public UUID getUserId() { return userId; }
	// Sets user ID
	public void setUserId(UUID userId) { this.userId = userId; }

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
}
