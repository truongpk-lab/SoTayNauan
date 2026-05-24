package com.sotaynauan.ai.ui.splash;

import com.sotaynauan.ai.data.repository.SessionRepository;

public class SplashViewModel {
    private final SessionRepository sessionRepository;

    public SplashViewModel(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SplashDestination resolveDestination() {
        return sessionRepository.hasActiveSession()
                ? SplashDestination.HOME
                : SplashDestination.LOGIN;
    }
}
