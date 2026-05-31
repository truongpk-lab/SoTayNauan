package com.sotaynauan.ai.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.home.HomeRecipeAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.RecipeDetailLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class RecipeListActivity extends Activity {
    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_ALL = "all";
    public static final String MODE_FAVORITES = "favorites";

    private RecipeRepository recipeRepository;
    private RecipeDetailLocalDataSource recipeDetailLocalDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        recipeDetailLocalDataSource = new RecipeDetailLocalDataSource(this);
        buildLayout();
    }

    private void buildLayout() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        boolean favoritesMode = MODE_FAVORITES.equals(mode);
        List<Recipe> recipes = favoritesMode ? loadFavoriteRecipes() : recipeRepository.getAllRecipes();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getResources().getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(28));
        scrollView.addView(root);

        TextView back = text("‹ Quay lại", 16, getResources().getColor(R.color.primary), true);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setMinHeight(dp(48));
        back.setOnClickListener(view -> finish());
        root.addView(back);

        root.addView(text(favoritesMode ? "Món yêu thích" : "Công thức của tôi",
                30, getResources().getColor(R.color.on_surface), true));
        root.addView(text(createSubtitle(favoritesMode, recipes.size()),
                15, getResources().getColor(R.color.on_surface_variant), false));

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        listParams.setMargins(0, dp(18), 0, 0);
        root.addView(listContainer, listParams);

        if (recipes.isEmpty()) {
            TextView empty = text(favoritesMode
                            ? "Chưa có món yêu thích. Hãy mở công thức và bấm tim để lưu vào đây."
                            : "Chưa có công thức nào trong kho local.",
                    16, getResources().getColor(R.color.primary), true);
            empty.setPadding(dp(14), dp(18), dp(14), dp(18));
            listContainer.addView(empty);
        } else {
            HomeRecipeAdapter adapter = new HomeRecipeAdapter(this, this::openRecipe);
            adapter.bindPopularCards(listContainer, recipes);
        }

        setContentView(scrollView);
    }

    private List<Recipe> loadFavoriteRecipes() {
        List<Recipe> favorites = new ArrayList<>();
        for (Long recipeId : recipeDetailLocalDataSource.getFavoriteRecipeIds()) {
            Recipe recipe = recipeRepository.findRecipe(recipeId);
            if (recipe != null) {
                favorites.add(recipe);
            }
        }
        return favorites;
    }

    private String createSubtitle(boolean favoritesMode, int count) {
        if (favoritesMode) {
            return "Hiển thị " + count + " món bạn đã bấm tim yêu thích.";
        }
        return "Hiển thị tất cả " + count + " công thức món ăn hiện có trong app.";
    }

    private void openRecipe(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setLineSpacing(0f, 1.08f);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
