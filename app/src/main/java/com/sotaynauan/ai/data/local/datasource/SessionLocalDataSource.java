package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;

import com.sotaynauan.ai.data.model.AppSession;

public class SessionLocalDataSource {
    private static final String PREFS_NAME = "so_tay_nau_an_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final SharedPreferences preferences;

    public SessionLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public AppSession getSession() {
        boolean loggedIn = preferences.getBoolean(KEY_LOGGED_IN, false);
        String userId = preferences.getString(KEY_USER_ID, "");
        String displayName = preferences.getString(KEY_DISPLAY_NAME, "");
        return new AppSession(userId, displayName, loggedIn);
    }

    public void saveSession(AppSession session) {
        preferences.edit()
                .putString(KEY_USER_ID, session.getUserId())
                .putString(KEY_DISPLAY_NAME, session.getDisplayName())
                .putBoolean(KEY_LOGGED_IN, session.isLoggedIn())
                .apply();
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
