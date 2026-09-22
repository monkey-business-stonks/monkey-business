package main;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
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

	public double getBalance() { return balance.doubleValue(); }
	public void setBalance(double newBalance) {
		// TODO: implement setBalance
		throw new UnsupportedOperationException("setBalance() not implemented");
	}

	public BigDecimal getCashBalance() { return cashBalance; }
	public void setCashBalance(BigDecimal cashBalance) {
		// TODO: implement setCashBalance
		throw new UnsupportedOperationException("setCashBalance() not implemented");
	}

	public Set<Asset> getAllAssets() { return heldAssets; }
	public Asset getAsset(String ticker) {
		if (ticker == null) return null;
		return heldAssets.stream().filter(a -> ticker.equals(a.ticker())).findFirst().orElse(null);
	}

	public ZonedDateTime getOpenDate() { return openedDate; }
	public UUID getAccID() { return accountID; }
	public AccountType getAccType() { return accountType; }
}

