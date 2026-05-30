package com.sotaynauan.ai.data.model;

public class AppSession {
    private final String userId;
    private final String displayName;
    private final boolean loggedIn;

    public AppSession(String userId, String displayName, boolean loggedIn) {
        this.userId = userId;
        this.displayName = displayName;
        this.loggedIn = loggedIn;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
