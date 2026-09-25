package main.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;
    
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel userAccessLevel;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AccountEntity> accounts;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum AccessLevel { User, Analyst, Operations }

    // Constructors
    public UserEntity() {}

    public UserEntity(UUID userId, String username, String email, String password,
                      AccessLevel userAccessLevel, boolean isActive, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.userAccessLevel = userAccessLevel;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public AccessLevel getUserAccessLevel() { return userAccessLevel; }
    public void setUserAccessLevel(AccessLevel userAccessLevel) { this.userAccessLevel = userAccessLevel; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Set<AccountEntity> getAccounts() { return accounts; }
    public void setAccounts(Set<AccountEntity> accounts) { this.accounts = accounts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
