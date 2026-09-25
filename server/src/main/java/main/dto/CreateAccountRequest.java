package main.dto;

import java.math.BigDecimal;

public class CreateAccountRequest {
    private String accountType;
    private BigDecimal initialBalance;
    private BigDecimal initialCashBalance;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String accountType, BigDecimal initialBalance, BigDecimal initialCashBalance) {
        this.accountType = accountType;
        this.initialBalance = initialBalance;
        this.initialCashBalance = initialCashBalance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getInitialCashBalance() {
        return initialCashBalance;
    }

    public void setInitialCashBalance(BigDecimal initialCashBalance) {
        this.initialCashBalance = initialCashBalance;
    }
}
