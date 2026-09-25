package main;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    public enum AccessLevel { USER, ANALYST, OPERATIONS }

    @Id
    private UUID userId;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel userAccessLevel;
    
    @Column(nullable = false)
    private boolean isActive;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Account> accounts;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // No-arg constructor for JPA
    public User() {
        this.accounts = new HashSet<>();
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor
    public User(UUID userId, String username, String email, String password,
                AccessLevel userAccessLevel, boolean isActive, Set<Account> accounts, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.userAccessLevel = userAccessLevel;
        this.isActive = isActive;
        this.accounts = accounts == null ? new HashSet<>() : new HashSet<>(accounts);
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    // Getters
    public UUID getUserId() { return this.userId; }
    public String getUsername() { return this.username; }
    public String getEmail() { return this.email; }
    public String getPassword() { return this.password; }
    public AccessLevel getUserAccessLevel() { return this.userAccessLevel; }
    public boolean getIsActive() { return this.isActive; }
    public Set<Account> getAccounts() { return this.accounts; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }

    // Record-style getters for backward compatibility
    public UUID userId() { return this.userId; }
    public String username() { return this.username; }
    public String email() { return this.email; }
    public String password() { return this.password; }
    public AccessLevel userAccessLevel() { return this.userAccessLevel; }
    public boolean isActive() { return this.isActive; }
    public Set<Account> accounts() { return this.accounts; }
    public LocalDateTime createdAt() { return this.createdAt; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setUserAccessLevel(AccessLevel level) { this.userAccessLevel = level; }
    public void setIsActive(boolean active) { this.isActive = active; }
    public void setAccounts(Set<Account> accounts) { this.accounts = accounts; }

    // Business logic methods
    public boolean login(String pwd) {
        if (!isActive) return false;
        return password != null && password.equals(pwd);
    }

    public void logout() {
        // No-op for stateless API
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (password == null || !password.equals(oldPassword)) {
            throw new IllegalArgumentException("invalid current password");
        }
        this.password = newPassword;
    }

    public boolean checkAccessLevel(AccessLevel required) {
        return this.userAccessLevel.ordinal() >= required.ordinal();
    }

    public void addAccount(Account account) {
        if (this.accounts == null) {
            this.accounts = new HashSet<>();
        }
        this.accounts.add(account);
    }
}
