package com.sotaynauan.ai.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.home.HomeRecipeAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.HomeContent;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.ai.AiChefActivity;
import com.sotaynauan.ai.ui.ai.IngredientInputActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.ui.search.SearchActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.util.AppExecutors;
import com.sotaynauan.ai.util.AppNavigator;

public class HomeActivity extends Activity {
    private HomeViewModel viewModel;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        viewModel = new HomeViewModel(createRecipeRepository());
        statusText = findViewById(R.id.homeStatusText);

        HomeRecipeAdapter adapter = new HomeRecipeAdapter(this, this::openRecipe);
        statusText.setText("Đang tải kho công thức local...");
        AppExecutors.runOnIo(
                () -> viewModel.loadHomeContent(),
                content -> {
                    if (!canBindUi()) {
                        return;
                    }
                    adapter.bindHorizontalCards(findViewById(R.id.todayRecipesContainer),
                            content.getTodaySuggestions());
                    adapter.bindPopularCards(findViewById(R.id.popularRecipesContainer),
                            content.getPopularRecipes());
                    adapter.bindFriendShares(findViewById(R.id.friendSharesContainer),
                            content.getFriendShares());
                    statusText.setText("Kho công thức local đã sẵn sàng.");
                },
                exception -> {
                    if (canBindUi()) {
                        statusText.setText("Chưa tải được dữ liệu local: " + exception.getMessage());
                    }
                });

        Button quickRecipeButton = findViewById(R.id.quickRecipeButton);
        quickRecipeButton.setOnClickListener(view -> openQuickSuggestion());
        Button ingredientSuggestionButton = findViewById(R.id.ingredientSuggestionButton);
        ingredientSuggestionButton.setOnClickListener(view ->
                startActivity(new Intent(this, IngredientInputActivity.class)));

        bindBottomNavigation();
    }

    private RecipeRepository createRecipeRepository() {
        return new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
    }

    private void openQuickSuggestion() {
        statusText.setText("Đang chọn món nhanh từ kho local...");
        AppExecutors.runOnIo(
                () -> viewModel.loadRandomQuickSuggestion(),
                recipe -> {
                    if (!canBindUi()) {
                        return;
                    }
                    if (recipe != null) {
                        statusText.setText("AI đã random món nhanh từ kho local: " + recipe.getName()
                                + ". Muốn gợi ý theo nguyên liệu, vào AI Chef và nhập những gì bạn đang có.");
                        openRecipe(recipe);
                    } else {
                        statusText.setText("Kho công thức local chưa có món nhanh phù hợp.");
                    }
                },
                exception -> {
                    if (canBindUi()) {
                        statusText.setText("Chưa chọn được món nhanh: " + exception.getMessage());
                    }
                });
    }

    private boolean canBindUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void openRecipe(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    private void bindBottomNavigation() {
        findViewById(R.id.homeTab).setOnClickListener(view ->
                statusText.setText("Bạn đang ở trang chủ với dữ liệu recipe local."));
        findViewById(R.id.searchTab).setOnClickListener(view ->
                AppNavigator.openTopLevel(this, SearchActivity.class));
        findViewById(R.id.aiChefTab).setOnClickListener(view ->
                AppNavigator.openTopLevel(this, AiChefActivity.class));
        findViewById(R.id.shoppingTab).setOnClickListener(view ->
                AppNavigator.openTopLevel(this, ShoppingListActivity.class));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                AppNavigator.openTopLevel(this, ProfileActivity.class));
    }
}
