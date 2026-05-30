package com.sotaynauan.ai.data.model;

public class AuthCredentials {
    private final String email;
    private final String password;

    public AuthCredentials(String email, String password) {
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
