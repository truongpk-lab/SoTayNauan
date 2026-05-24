package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileState {
    private final String userId;
    private final String displayName;
    private final String email;
    private final int cookedCount;
    private final int favoriteCount;
    private final int friendCount;
    private final boolean notificationsEnabled;
    private final boolean compactModeEnabled;
    private final List<Recipe> savedRecipes;
    private final List<ProfileMenuItem> menuItems;
    private final String statusMessage;

    public ProfileState(String userId,
                        String displayName,
                        String email,
                        int cookedCount,
                        int favoriteCount,
                        int friendCount,
                        boolean notificationsEnabled,
                        boolean compactModeEnabled,
                        List<Recipe> savedRecipes,
                        List<ProfileMenuItem> menuItems,
                        String statusMessage) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.cookedCount = cookedCount;
        this.favoriteCount = favoriteCount;
        this.friendCount = friendCount;
        this.notificationsEnabled = notificationsEnabled;
        this.compactModeEnabled = compactModeEnabled;
        this.savedRecipes = new ArrayList<>(savedRecipes);
        this.menuItems = new ArrayList<>(menuItems);
        this.statusMessage = statusMessage;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public int getCookedCount() {
        return cookedCount;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isCompactModeEnabled() {
        return compactModeEnabled;
    }

    public List<Recipe> getSavedRecipes() {
        return Collections.unmodifiableList(savedRecipes);
    }

    public List<ProfileMenuItem> getMenuItems() {
        return Collections.unmodifiableList(menuItems);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getInitials() {
        String cleanName = displayName == null ? "" : displayName.trim();
        if (cleanName.isEmpty()) {
            return "BN";
        }
        String[] parts = cleanName.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : first;
        return (first + last).toUpperCase();
    }
}

