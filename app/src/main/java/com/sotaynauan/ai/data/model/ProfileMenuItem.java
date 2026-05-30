package com.sotaynauan.ai.data.model;

public class ProfileMenuItem {
    public static final String ACTION_MY_RECIPES = "my_recipes";
    public static final String ACTION_FAVORITES = "favorites";
    public static final String ACTION_FRIENDS = "friends";
    public static final String ACTION_AI_TASTE = "ai_taste";
    public static final String ACTION_VOICE_SETTINGS = "voice_settings";
    public static final String ACTION_APP_SETTINGS = "app_settings";

    private final String id;
    private final String iconText;
    private final String title;
    private final String badge;

    public ProfileMenuItem(String id, String iconText, String title, String badge) {
        this.id = id;
        this.iconText = iconText;
        this.title = title;
        this.badge = badge;
    }

    public String getId() {
        return id;
    }

    public String getIconText() {
        return iconText;
    }

    public String getTitle() {
        return title;
    }

    public String getBadge() {
        return badge;
    }

    public boolean hasBadge() {
        return badge != null && !badge.trim().isEmpty();
    }
}

