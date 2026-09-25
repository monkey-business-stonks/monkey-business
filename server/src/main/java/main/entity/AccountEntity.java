package main.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {
    
    @Id
    @Column(name = "account_id", columnDefinition = "UUID")
    private UUID accountId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    
    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashBalance;
    
    @Column(name = "opened_date", nullable = false)
    private ZonedDateTime openedDate;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderEntity> orderHistory;
    
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AssetEntity> heldAssets;

    public enum AccountType { BROKERAGE, _401K, ROTH_IRA, CRYPTO, FOREX }

    // Constructors
    public AccountEntity() {}

    public AccountEntity(UUID accountId, UserEntity user, AccountType accountType,
                        BigDecimal balance, BigDecimal cashBalance, ZonedDateTime openedDate) {
        this.accountId = accountId;
        this.user = user;
        this.accountType = accountType;
        this.balance = balance;
        this.cashBalance = cashBalance;
        this.openedDate = openedDate;
    }

    // Getters and Setters
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public ZonedDateTime getOpenedDate() { return openedDate; }
    public void setOpenedDate(ZonedDateTime openedDate) { this.openedDate = openedDate; }

    public Set<OrderEntity> getOrderHistory() { return orderHistory; }
    public void setOrderHistory(Set<OrderEntity> orderHistory) { this.orderHistory = orderHistory; }

    public Set<AssetEntity> getHeldAssets() { return heldAssets; }
    public void setHeldAssets(Set<AssetEntity> heldAssets) { this.heldAssets = heldAssets; }
}
