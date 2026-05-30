package com.sotaynauan.ai.data.model;

public class AuthResult {
    private final boolean success;
    private final String message;
    private final AuthUser user;

    private AuthResult(boolean success, String message, AuthUser user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static AuthResult success(String message, AuthUser user) {
        return new AuthResult(true, message, user);
    }

    public static AuthResult error(String message) {
        return new AuthResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public AuthUser getUser() {
        return user;
    }
}
