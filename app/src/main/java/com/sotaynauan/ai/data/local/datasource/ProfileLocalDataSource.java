package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;

public class ProfileLocalDataSource {
    private static final String PREFS_NAME = "profile_settings";
    private static final String KEY_DISPLAY_NAME_PREFIX = "display_name_";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_COMPACT_MODE = "compact_mode";
    private static final String KEY_COOKED_COUNT = "cooked_count";

    private final SharedPreferences preferences;

    public ProfileLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getDisplayNameOverride(String userId) {
        return preferences.getString(KEY_DISPLAY_NAME_PREFIX + safeUserId(userId), "");
    }

    public void saveDisplayNameOverride(String userId, String displayName) {
        preferences.edit()
                .putString(KEY_DISPLAY_NAME_PREFIX + safeUserId(userId), displayName)
                .apply();
    }

    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public boolean toggleNotifications() {
        boolean nextValue = !isNotificationsEnabled();
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, nextValue).apply();
        return nextValue;
    }

    public boolean isCompactModeEnabled() {
        return preferences.getBoolean(KEY_COMPACT_MODE, false);
    }

    public boolean toggleCompactMode() {
        boolean nextValue = !isCompactModeEnabled();
        preferences.edit().putBoolean(KEY_COMPACT_MODE, nextValue).apply();
        return nextValue;
    }

    public int getCookedCount() {
        return preferences.getInt(KEY_COOKED_COUNT, 0);
    }

    private String safeUserId(String userId) {
        return userId == null || userId.trim().isEmpty() ? "guest-local" : userId;
    }
}

