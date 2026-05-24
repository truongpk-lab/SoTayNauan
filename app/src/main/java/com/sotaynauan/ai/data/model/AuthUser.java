package com.sotaynauan.ai.data.model;

public class AuthUser {
    private final String userId;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final long createdAtMillis;

    public AuthUser(String userId, String email, String displayName, String passwordHash, long createdAtMillis) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAtMillis = createdAtMillis;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
