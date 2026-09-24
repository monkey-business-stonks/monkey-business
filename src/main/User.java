package main;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;

public record User(
    UUID userId,
    String username,
    String email,
    String password,
    AccessLevel userAccessLevel,
    boolean isActive,
    Set<Account> accounts,
    LocalDateTime createdAt
) {
    public enum AccessLevel { User, Analyst, Operations }

    public User {
        if (accounts == null) accounts = Set.of();
        else accounts = Set.copyOf(accounts);
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public boolean login(String pwd) {
        if (!isActive) return false;
        return password != null && password.equals(pwd);
    }

    public User logout() {
        return this;
    }

    public String isActive() {
        return isActive;
    }

    public User changePassword(String oldPassword, String newPassword) {
        if (password == null || !password.equals(oldPassword)) {
            throw new IllegalArgumentException("invalid current password");
        }
        return new User(userId, username, email, newPassword, userAccessLevel, isActive, accounts, createdAt);
    }

    public boolean checkAccessLevel(AccessLevel required) {
        return this.userAccessLevel.ordinal() >= required.ordinal();
    }

    public User addAccount(Account account) {
        var newAccounts = new HashSet<>(this.accounts);
        newAccounts.add(account);
        return new User(userId, username, email, password, userAccessLevel, isActive, Set.copyOf(newAccounts), createdAt);
    }

}
