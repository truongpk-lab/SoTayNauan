package com.sotaynauan.ai.adapter.auth;

import android.widget.EditText;
import android.widget.TextView;

import com.sotaynauan.ai.data.model.AuthCredentials;
import com.sotaynauan.ai.data.model.AuthResult;

public class AuthFormAdapter {
    private final EditText emailInput;
    private final EditText passwordInput;
    private final TextView statusText;

    public AuthFormAdapter(EditText emailInput, EditText passwordInput, TextView statusText) {
        this.emailInput = emailInput;
        this.passwordInput = passwordInput;
        this.statusText = statusText;
    }

    public AuthCredentials getCredentials() {
        return new AuthCredentials(
                emailInput.getText().toString(),
                passwordInput.getText().toString()
        );
    }

    public String getEmail() {
        return emailInput.getText().toString();
    }

    public void bindResult(AuthResult result) {
        statusText.setText(result.getMessage());
        statusText.setSelected(result.isSuccess());
        emailInput.setError(null);
        passwordInput.setError(null);
        if (!result.isSuccess()) {
            String message = result.getMessage().toLowerCase();
            if (message.contains("email")) {
                emailInput.setError(result.getMessage());
            } else if (message.contains("mật khẩu")) {
                passwordInput.setError(result.getMessage());
            }
        }
    }
}
