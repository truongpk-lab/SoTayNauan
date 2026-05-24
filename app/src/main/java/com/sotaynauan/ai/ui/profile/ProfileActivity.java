package com.sotaynauan.ai.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.profile.ProfileMenuAdapter;
import com.sotaynauan.ai.adapter.profile.ProfileSavedRecipeAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.AuthLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.CommunityLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ProfileLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeDetailLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.SessionLocalDataSource;
import com.sotaynauan.ai.data.mapper.CommunityMapper;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.ProfileMenuItem;
import com.sotaynauan.ai.data.model.ProfileState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.CommunityRepository;
import com.sotaynauan.ai.data.repository.ProfileRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.SessionRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.ai.AiChefActivity;
import com.sotaynauan.ai.ui.auth.LoginActivity;
import com.sotaynauan.ai.ui.community.CommunityActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.ui.voice.VoiceSettingsActivity;

public class ProfileActivity extends Activity {
    private ProfileViewModel viewModel;
    private ProfileMenuAdapter menuAdapter;
    private ProfileSavedRecipeAdapter savedRecipeAdapter;
    private TextView avatarText;
    private TextView headerAvatarText;
    private TextView nameText;
    private TextView emailText;
    private TextView cookedStatText;
    private TextView favoriteStatText;
    private TextView friendStatText;
    private TextView statusText;
    private TextView settingsSummaryText;
    private LinearLayout savedRecipesContainer;
    private LinearLayout menuContainer;
    private ProfileState currentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ProfileViewModel(createProfileRepository());
        bindViews();
        bindActions();
        currentState = viewModel.loadProfile();
        bindState(currentState);
    }

    private void bindViews() {
        avatarText = findViewById(R.id.profileAvatarText);
        headerAvatarText = findViewById(R.id.profileHeaderAvatarText);
        nameText = findViewById(R.id.profileNameText);
        emailText = findViewById(R.id.profileEmailText);
        cookedStatText = findViewById(R.id.profileCookedStatText);
        favoriteStatText = findViewById(R.id.profileFavoriteStatText);
        friendStatText = findViewById(R.id.profileFriendStatText);
        statusText = findViewById(R.id.profileStatusText);
        settingsSummaryText = findViewById(R.id.profileSettingsSummaryText);
        savedRecipesContainer = findViewById(R.id.profileSavedRecipesContainer);
        menuContainer = findViewById(R.id.profileMenuContainer);
        menuAdapter = new ProfileMenuAdapter(this, this::handleMenuClick);
        savedRecipeAdapter = new ProfileSavedRecipeAdapter(this, this::openRecipe);
    }

    private void bindActions() {
        findViewById(R.id.profileEditButton).setOnClickListener(view -> showEditProfileDialog());
        findViewById(R.id.profileLogoutButton).setOnClickListener(view -> showLogoutDialog());
        findViewById(R.id.profileNotificationToggle).setOnClickListener(view ->
                bindState(viewModel.toggleNotifications()));
        findViewById(R.id.profileCompactToggle).setOnClickListener(view ->
                bindState(viewModel.toggleCompactMode()));

        findViewById(R.id.homeTab).setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.searchTab).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class)));
        findViewById(R.id.aiChefTab).setOnClickListener(view ->
                startActivity(new Intent(this, AiChefActivity.class)));
        findViewById(R.id.shoppingTab).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                statusText.setText("Bạn đang ở tab cá nhân."));
    }

    private void bindState(ProfileState state) {
        currentState = state;
        avatarText.setText(state.getInitials());
        headerAvatarText.setText(state.getInitials());
        nameText.setText(state.getDisplayName());
        emailText.setText(state.getEmail());
        cookedStatText.setText(String.valueOf(state.getCookedCount()));
        favoriteStatText.setText(String.valueOf(state.getFavoriteCount()));
        friendStatText.setText(String.valueOf(state.getFriendCount()));
        settingsSummaryText.setText((state.isNotificationsEnabled() ? "Thông báo bật" : "Thông báo tắt")
                + " • " + (state.isCompactModeEnabled() ? "Màn gọn bật" : "Màn gọn tắt"));
        statusText.setText(state.getStatusMessage());
        savedRecipeAdapter.bind(savedRecipesContainer, state.getSavedRecipes());
        menuAdapter.bind(menuContainer, state.getMenuItems());
    }

    private void handleMenuClick(ProfileMenuItem item) {
        if (ProfileMenuItem.ACTION_MY_RECIPES.equals(item.getId())) {
            startActivity(new Intent(this, HomeActivity.class));
        } else if (ProfileMenuItem.ACTION_FAVORITES.equals(item.getId())) {
            statusText.setText("Đang hiển thị " + currentState.getFavoriteCount()
                    + " món đã lưu từ local storage.");
        } else if (ProfileMenuItem.ACTION_FRIENDS.equals(item.getId())) {
            startActivity(new Intent(this, CommunityActivity.class));
        } else if (ProfileMenuItem.ACTION_AI_TASTE.equals(item.getId())) {
            bindState(viewModel.toggleCompactMode());
        } else if (ProfileMenuItem.ACTION_VOICE_SETTINGS.equals(item.getId())) {
            startActivity(new Intent(this, VoiceSettingsActivity.class));
        } else if (ProfileMenuItem.ACTION_APP_SETTINGS.equals(item.getId())) {
            bindState(viewModel.toggleNotifications());
        }
    }

    private void showEditProfileDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(currentState.getDisplayName());
        input.setSelectAllOnFocus(true);
        input.setTextColor(getColor(R.color.on_surface));
        input.setHint("Tên hiển thị");
        new AlertDialog.Builder(this)
                .setTitle("Sửa hồ sơ")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    hideKeyboard(input);
                    bindState(viewModel.updateDisplayName(input.getText().toString()));
                })
                .show();
        input.requestFocus();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Phiên đăng nhập local sẽ được xóa. Công thức, shopping list và cài đặt vẫn nằm trên thiết bị.")
                .setNegativeButton("Ở lại", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    viewModel.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private void openRecipe(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    private ProfileRepository createProfileRepository() {
        AppDatabase database = AppDatabase.getInstance(this);
        SeedDataProvider seedDataProvider = new SeedDataProvider();
        RecipeRepository recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(database.recipeDao(), seedDataProvider),
                new RecipeMapper());
        CommunityRepository communityRepository = new CommunityRepository(
                new CommunityLocalDataSource(database.communityDao(), seedDataProvider),
                new CommunityMapper());
        return new ProfileRepository(
                new ProfileLocalDataSource(this),
                new SessionRepository(new SessionLocalDataSource(this)),
                new AuthLocalDataSource(this),
                recipeRepository,
                new RecipeDetailLocalDataSource(this),
                communityRepository,
                new CookingLocalDataSource(this));
    }

    private void hideKeyboard(EditText input) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }
}
