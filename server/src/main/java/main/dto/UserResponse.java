package main.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {
    private UUID userId;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String dob;
    private String accessLevel;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(UUID userId, String username, String name, String email, String phone, 
                       String dob, String accessLevel, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.dob = dob;
        this.accessLevel = accessLevel;
        this.createdAt = createdAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
