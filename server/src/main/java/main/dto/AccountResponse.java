package main.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

public class AccountResponse {
    private UUID accountId;
    private UUID userId;
    private String accountType;
    private ZonedDateTime openedDate;
    private BigDecimal balance;
    private BigDecimal cashBalance;

    public AccountResponse() {
    }

    public AccountResponse(UUID accountId, UUID userId, String accountType, ZonedDateTime openedDate, 
                          BigDecimal balance, BigDecimal cashBalance) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountType = accountType;
        this.openedDate = openedDate;
        this.balance = balance;
        this.cashBalance = cashBalance;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public ZonedDateTime getOpenedDate() {
        return openedDate;
    }

    public void setOpenedDate(ZonedDateTime openedDate) {
        this.openedDate = openedDate;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
}
