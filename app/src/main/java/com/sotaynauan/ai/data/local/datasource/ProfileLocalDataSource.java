package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;

public class ProfileLocalDataSource {
    private static final String PREFS_NAME = "profile_settings";
    private static final String KEY_DISPLAY_NAME_PREFIX = "display_name_";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_COMPACT_MODE = "compact_mode";
    private static final String KEY_COOKED_COUNT = "cooked_count";
    private static final String KEY_AI_TASTE_STYLE = "ai_taste_style";
    private static final String KEY_AI_SPICE_LEVEL = "ai_spice_level";
    private static final String KEY_AI_MAX_COOKING_TIME = "ai_max_cooking_time";
    private static final String KEY_AI_VEGETARIAN = "ai_vegetarian";
    private static final String KEY_AI_BUDGET_FRIENDLY = "ai_budget_friendly";

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

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public void setCompactModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_COMPACT_MODE, enabled).apply();
    }

    public String getAiTasteStyle() {
        return preferences.getString(KEY_AI_TASTE_STYLE, "Đậm đà gia đình");
    }

    public void setAiTasteStyle(String style) {
        preferences.edit().putString(KEY_AI_TASTE_STYLE, style).apply();
    }

    public String getAiSpiceLevel() {
        return preferences.getString(KEY_AI_SPICE_LEVEL, "Vừa phải");
    }

    public void setAiSpiceLevel(String spiceLevel) {
        preferences.edit().putString(KEY_AI_SPICE_LEVEL, spiceLevel).apply();
    }

    public int getAiMaxCookingTime() {
        return preferences.getInt(KEY_AI_MAX_COOKING_TIME, 45);
    }

    public void setAiMaxCookingTime(int minutes) {
        preferences.edit().putInt(KEY_AI_MAX_COOKING_TIME, Math.max(10, minutes)).apply();
    }

    public boolean isAiVegetarianPreferred() {
        return preferences.getBoolean(KEY_AI_VEGETARIAN, false);
    }

    public void setAiVegetarianPreferred(boolean enabled) {
        preferences.edit().putBoolean(KEY_AI_VEGETARIAN, enabled).apply();
    }

    public boolean isAiBudgetFriendlyPreferred() {
        return preferences.getBoolean(KEY_AI_BUDGET_FRIENDLY, true);
    }

    public void setAiBudgetFriendlyPreferred(boolean enabled) {
        preferences.edit().putBoolean(KEY_AI_BUDGET_FRIENDLY, enabled).apply();
    }

    public int getCookedCount() {
        return preferences.getInt(KEY_COOKED_COUNT, 0);
    }

    private String safeUserId(String userId) {
        return userId == null || userId.trim().isEmpty() ? "guest-local" : userId;
    }
}

