package main.dto;

import java.util.UUID;

public class AuthResponse {
    private UUID userId;
    private boolean isAuthenticated;
    private String accessLevel;
    private String token;

    public AuthResponse() {
    }

    public AuthResponse(UUID userId, boolean isAuthenticated, String accessLevel, String token) {
        this.userId = userId;
        this.isAuthenticated = isAuthenticated;
        this.accessLevel = accessLevel;
        this.token = token;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
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
