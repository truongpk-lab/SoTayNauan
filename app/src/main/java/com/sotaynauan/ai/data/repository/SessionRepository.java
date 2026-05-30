package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.SessionLocalDataSource;
import com.sotaynauan.ai.data.model.AppSession;

public class SessionRepository {
    private final SessionLocalDataSource localDataSource;

    public SessionRepository(SessionLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    public boolean hasActiveSession() {
        return localDataSource.getSession().isLoggedIn();
    }

    public AppSession getSession() {
        return localDataSource.getSession();
    }

    public void continueAsGuest() {
        localDataSource.saveSession(new AppSession("guest-local", "Khach bep nha", true));
    }

    public void saveSession(AppSession session) {
        localDataSource.saveSession(session);
    }

    public void clearSession() {
        localDataSource.clearSession();
    }
}
