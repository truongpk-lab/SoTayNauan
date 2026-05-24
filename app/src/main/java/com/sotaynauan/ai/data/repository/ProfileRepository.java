package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.AuthLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ProfileLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeDetailLocalDataSource;
import com.sotaynauan.ai.data.model.AppSession;
import com.sotaynauan.ai.data.model.AuthUser;
import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.ProfileMenuItem;
import com.sotaynauan.ai.data.model.ProfileState;
import com.sotaynauan.ai.data.model.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileRepository {
    private final ProfileLocalDataSource profileLocalDataSource;
    private final SessionRepository sessionRepository;
    private final AuthLocalDataSource authLocalDataSource;
    private final RecipeRepository recipeRepository;
    private final RecipeDetailLocalDataSource recipeDetailLocalDataSource;
    private final CommunityRepository communityRepository;
    private final CookingLocalDataSource cookingLocalDataSource;

    public ProfileRepository(ProfileLocalDataSource profileLocalDataSource,
                             SessionRepository sessionRepository,
                             AuthLocalDataSource authLocalDataSource,
                             RecipeRepository recipeRepository,
                             RecipeDetailLocalDataSource recipeDetailLocalDataSource,
                             CommunityRepository communityRepository,
                             CookingLocalDataSource cookingLocalDataSource) {
        this.profileLocalDataSource = profileLocalDataSource;
        this.sessionRepository = sessionRepository;
        this.authLocalDataSource = authLocalDataSource;
        this.recipeRepository = recipeRepository;
        this.recipeDetailLocalDataSource = recipeDetailLocalDataSource;
        this.communityRepository = communityRepository;
        this.cookingLocalDataSource = cookingLocalDataSource;
    }

    public ProfileState loadProfile(String statusMessage) {
        AppSession session = sessionRepository.getSession();
        AuthUser user = authLocalDataSource.findUserById(session.getUserId());
        String displayName = resolveDisplayName(session, user);
        String email = user == null ? "khach.bepnha@local" : user.getEmail();
        List<Recipe> savedRecipes = loadSavedRecipes();
        CommunityState communityState = communityRepository.loadCommunity("", "");
        return new ProfileState(
                session.getUserId(),
                displayName,
                email,
                cookingLocalDataSource.getCookedCount(),
                savedRecipes.size(),
                communityState.getFriends().size(),
                profileLocalDataSource.isNotificationsEnabled(),
                profileLocalDataSource.isCompactModeEnabled(),
                savedRecipes,
                createMenuItems(),
                statusMessage);
    }

    public ProfileState updateDisplayName(String displayName) {
        AppSession session = sessionRepository.getSession();
        String safeName = sanitizeDisplayName(displayName);
        AuthUser user = authLocalDataSource.findUserById(session.getUserId());
        if (user != null) {
            authLocalDataSource.saveUser(new AuthUser(user.getUserId(), user.getEmail(),
                    safeName, user.getPasswordHash(), user.getCreatedAtMillis()));
        }
        profileLocalDataSource.saveDisplayNameOverride(session.getUserId(), safeName);
        sessionRepository.saveSession(new AppSession(session.getUserId(), safeName, session.isLoggedIn()));
        return loadProfile("Đã cập nhật hồ sơ cá nhân và lưu local.");
    }

    public ProfileState toggleNotifications() {
        boolean enabled = profileLocalDataSource.toggleNotifications();
        return loadProfile(enabled
                ? "Đã bật nhắc nhở nấu ăn trên thiết bị này."
                : "Đã tắt nhắc nhở nấu ăn local.");
    }

    public ProfileState toggleCompactMode() {
        boolean enabled = profileLocalDataSource.toggleCompactMode();
        return loadProfile(enabled
                ? "Đã bật chế độ gọn cho màn hình cá nhân."
                : "Đã tắt chế độ gọn cho màn hình cá nhân.");
    }

    public void logout() {
        sessionRepository.clearSession();
    }

    private String resolveDisplayName(AppSession session, AuthUser user) {
        String override = profileLocalDataSource.getDisplayNameOverride(session.getUserId());
        if (!override.trim().isEmpty()) {
            return override;
        }
        if (user != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        if (!session.getDisplayName().trim().isEmpty()) {
            return session.getDisplayName();
        }
        return "Bạn bếp nhà";
    }

    private String sanitizeDisplayName(String displayName) {
        String safeName = displayName == null ? "" : displayName.trim();
        if (safeName.length() < 2) {
            return "Bạn bếp nhà";
        }
        return safeName.length() > 40 ? safeName.substring(0, 40) : safeName;
    }

    private List<Recipe> loadSavedRecipes() {
        List<Recipe> savedRecipes = new ArrayList<>();
        for (Long recipeId : recipeDetailLocalDataSource.getFavoriteRecipeIds()) {
            Recipe recipe = recipeRepository.findRecipe(recipeId);
            if (recipe != null) {
                savedRecipes.add(recipe);
            }
        }
        if (savedRecipes.isEmpty()) {
            Recipe quickSuggestion = recipeRepository.getQuickSuggestion();
            if (quickSuggestion != null) {
                savedRecipes.add(quickSuggestion);
            }
        }
        return savedRecipes;
    }

    private List<ProfileMenuItem> createMenuItems() {
        return Arrays.asList(
                new ProfileMenuItem(ProfileMenuItem.ACTION_MY_RECIPES, "★", "Công thức của tôi", ""),
                new ProfileMenuItem(ProfileMenuItem.ACTION_FAVORITES, "♡", "Món yêu thích", ""),
                new ProfileMenuItem(ProfileMenuItem.ACTION_FRIENDS, "Nh", "Bạn bè & Nhóm", ""),
                new ProfileMenuItem(ProfileMenuItem.ACTION_AI_TASTE, "AI", "Hồ sơ khẩu vị AI", "Mới"),
                new ProfileMenuItem(ProfileMenuItem.ACTION_VOICE_SETTINGS, "Mic", "Cài đặt giọng nói", ""),
                new ProfileMenuItem(ProfileMenuItem.ACTION_APP_SETTINGS, "⚙", "Cài đặt app", ""));
    }
}
