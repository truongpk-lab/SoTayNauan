package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.ai.MatchIngredientAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.MatchDetailState;
import com.sotaynauan.ai.data.model.RecipeMatch;
import com.sotaynauan.ai.data.repository.AiChefRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingPlanActivity;

import java.util.Locale;

public class MatchDetailActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private MatchDetailViewModel viewModel;
    private MatchIngredientAdapter ingredientAdapter;
    private long recipeId;
    private MatchDetailState currentState;

    private TextView heroCategory;
    private TextView recipeName;
    private TextView scoreBadge;
    private TextView aiExplanation;
    private TextView availableCount;
    private TextView missingCount;
    private TextView substituteCount;
    private TextView statusText;
    private LinearLayout availableContainer;
    private LinearLayout missingContainer;
    private LinearLayout substituteContainer;
    private Button cookNowButton;
    private Button addShoppingButton;
    private Button saveRecipeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_detail);

        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        viewModel = new MatchDetailViewModel(createAiChefRepository(), createShoppingRepository());
        ingredientAdapter = new MatchIngredientAdapter(this);

        bindViews();
        bindActions();
        bindState(viewModel.loadMatchDetail(recipeId));
    }

    private void bindViews() {
        heroCategory = findViewById(R.id.matchHeroCategory);
        recipeName = findViewById(R.id.matchRecipeName);
        scoreBadge = findViewById(R.id.matchScoreBadge);
        aiExplanation = findViewById(R.id.matchAiExplanation);
        availableCount = findViewById(R.id.availableCount);
        missingCount = findViewById(R.id.missingCount);
        substituteCount = findViewById(R.id.substituteCount);
        statusText = findViewById(R.id.matchStatusText);
        availableContainer = findViewById(R.id.availableContainer);
        missingContainer = findViewById(R.id.missingContainer);
        substituteContainer = findViewById(R.id.substituteContainer);
        cookNowButton = findViewById(R.id.cookNowButton);
        addShoppingButton = findViewById(R.id.addShoppingButton);
        saveRecipeButton = findViewById(R.id.saveRecipeButton);
    }

    private void bindActions() {
        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        cookNowButton.setOnClickListener(view -> {
            RecipeMatch match = currentState == null ? null : currentState.getMatch();
            if (match != null) {
                Intent intent = new Intent(this, RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, match.getRecipe().getId());
                startActivity(intent);
            }
        });
        addShoppingButton.setOnClickListener(view -> {
            bindState(viewModel.saveMissingIngredients(recipeId));
            if (currentState != null && currentState.getShoppingPlanState() != null
                    && !currentState.getShoppingPlanState().isEmpty()) {
                startActivity(new Intent(this, ShoppingPlanActivity.class));
            }
        });
        saveRecipeButton.setOnClickListener(view -> {
            if (currentState != null && currentState.hasMatch()) {
                bindState(viewModel.toggleFavorite(recipeId));
            }
        });
    }

    private AiChefRepository createAiChefRepository() {
        RecipeRepository recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        return new AiChefRepository(new AiChefLocalDataSource(this), recipeRepository);
    }

    private ShoppingRepository createShoppingRepository() {
        return new ShoppingRepository(new ShoppingLocalDataSource(this));
    }

    private void bindState(MatchDetailState state) {
        currentState = state;
        RecipeMatch match = state.getMatch();
        statusText.setText(state.getStatusMessage());
        if (match == null) {
            recipeName.setText("Chưa có món phù hợp");
            heroCategory.setText("AI Chef");
            scoreBadge.setText("0%");
            aiExplanation.setText(state.getAiExplanation());
            availableCount.setText("0/0 nguyên liệu");
            missingCount.setText("Chưa có dữ liệu");
            substituteCount.setText("Chưa có gợi ý");
            cookNowButton.setEnabled(false);
            addShoppingButton.setEnabled(false);
            saveRecipeButton.setEnabled(false);
            return;
        }

        recipeId = match.getRecipe().getId();
        heroCategory.setText(match.getRecipe().getCategory());
        heroCategory.setBackground(createHeroBackground(match.getRecipe().getColorArgb()));
        recipeName.setText(match.getRecipe().getName());
        scoreBadge.setText(String.format(Locale.US, "Độ khớp: %d%%", match.getScorePercent()));
        aiExplanation.setText(state.getAiExplanation());
        availableCount.setText(String.format(Locale.US, "%d/%d nguyên liệu",
                match.getAvailableCount(), match.getRequiredCount()));
        missingCount.setText(match.getMissingIngredients().isEmpty()
                ? "Không thiếu nguyên liệu"
                : match.getMissingIngredients().size() + " cần mua");
        substituteCount.setText(state.getSubstituteSuggestions().isEmpty()
                ? "Không cần thay thế"
                : state.getSubstituteSuggestions().size() + " gợi ý thay thế");
        ingredientAdapter.bindAvailable(availableContainer, match.getAvailableIngredients());
        ingredientAdapter.bindMissing(missingContainer, match.getMissingIngredients());
        ingredientAdapter.bindSubstitutes(substituteContainer, state.getSubstituteSuggestions());
        addShoppingButton.setText(match.getMissingIngredients().isEmpty()
                ? "Không cần đi chợ"
                : "Thêm vào đi chợ");
        saveRecipeButton.setText(match.isFavorite() ? "♥" : "♡");
        addShoppingButton.setEnabled(true);
        cookNowButton.setEnabled(true);
        saveRecipeButton.setEnabled(true);
    }

    private GradientDrawable createHeroBackground(int baseColor) {
        int darker = Color.rgb(
                Math.max(0, (int) (Color.red(baseColor) * 0.62f)),
                Math.max(0, (int) (Color.green(baseColor) * 0.62f)),
                Math.max(0, (int) (Color.blue(baseColor) * 0.62f)));
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{baseColor, Color.parseColor("#F5CFA8"), darker});
        drawable.setCornerRadii(new float[]{
                0f, 0f, 0f, 0f,
                dp(34), dp(34), dp(34), dp(34)
        });
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
