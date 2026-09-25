package main.dto;

import java.util.UUID;

public class AuthResponse {
    private UUID userId;
    private String username;
    private String email;
    private String accessLevel;
    private String token;

    public AuthResponse() {
    }

    public AuthResponse(UUID userId, String username, String email, String accessLevel, String token) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.accessLevel = accessLevel;
        this.token = token;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
