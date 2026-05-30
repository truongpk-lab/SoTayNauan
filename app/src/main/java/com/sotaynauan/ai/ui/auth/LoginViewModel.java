package com.sotaynauan.ai.ui.auth;

import com.sotaynauan.ai.data.model.AuthCredentials;
import com.sotaynauan.ai.data.model.AuthResult;
import com.sotaynauan.ai.data.repository.AuthRepository;

public class LoginViewModel {
    private final AuthRepository authRepository;

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public AuthResult login(AuthCredentials credentials) {
        return authRepository.login(credentials);
    }

    public AuthResult register(AuthCredentials credentials) {
        return authRepository.register(credentials);
    }

    public AuthResult recoverPassword(String email) {
        return authRepository.recoverPassword(email);
    }
}
