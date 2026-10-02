package domain.entities;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
    private String passwordHash;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = true)
    private String phone;
    
    @Column(nullable = false)
    private LocalDate dob;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel accessLevel;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Account> accounts;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // No-arg constructor for JPA
    public User() {
        this.accounts = new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Full constructor
    public User(UUID userId, String username, String email, String passwordHash, String name,
                String phone, LocalDate dob, AccessLevel accessLevel, Set<Account> accounts, 
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.dob = dob;
        this.accessLevel = accessLevel;
        this.accounts = accounts == null ? new HashSet<>() : new HashSet<>(accounts);
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    // Getters
    public UUID getUserId() { return this.userId; }
    public String getUsername() { return this.username; }
    public String getEmail() { return this.email; }
    public String getPasswordHash() { return this.passwordHash; }
    public String getName() { return this.name; }
    public String getPhone() { return this.phone; }
    public LocalDate getDob() { return this.dob; }
    public AccessLevel getAccessLevel() { return this.accessLevel; }
    public Set<Account> getAccounts() { return this.accounts; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }

    // Record-style getters for backward compatibility
    public UUID userId() { return this.userId; }
    public String username() { return this.username; }
    public String email() { return this.email; }
    public String passwordHash() { return this.passwordHash; }
    public String name() { return this.name; }
    public String phone() { return this.phone; }
    public LocalDate dob() { return this.dob; }
    public AccessLevel accessLevel() { return this.accessLevel; }
    public Set<Account> accounts() { return this.accounts; }
    public LocalDateTime createdAt() { return this.createdAt; }
    public LocalDateTime updatedAt() { return this.updatedAt; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public void setAccessLevel(AccessLevel level) { this.accessLevel = level; }
    public void setAccounts(Set<Account> accounts) { this.accounts = accounts; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business logic methods
    public void changePassword(String newPassword) {
        this.passwordHash = newPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean checkAccessLevel(AccessLevel required) {
        return this.accessLevel.ordinal() >= required.ordinal();
    }

    public void addAccount(Account account) {
        if (this.accounts == null) {
            this.accounts = new HashSet<>();
        }
        this.accounts.add(account);
    }
}
