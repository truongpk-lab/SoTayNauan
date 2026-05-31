package com.sotaynauan.ai.ui.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.BuildConfig;
import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.ai.RecipeMatchAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.AiChefLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.AiRecipeSuggestionState;
import com.sotaynauan.ai.data.model.RecipeMatch;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;
import com.sotaynauan.ai.data.repository.AiChefRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.community.CommunityActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.profile.ProfileActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.ui.recipe.RecipeDetailActivity;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.List;
import java.util.Locale;

public class AiRecipeSuggestionActivity extends Activity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AiRecipeSuggestionViewModel viewModel;
    private RecipeMatchAdapter adapter;
    private AiRecipeSuggestionState currentState;
    private TextView resultsSubtitle;
    private TextView bestHero;
    private ImageView bestHeroImage;
    private TextView bestRecipeName;
    private TextView bestScore;
    private TextView bestReadyLabel;
    private TextView bestIngredientCount;
    private TextView bestMissing;
    private TextView resultsStatus;
    private Button favoriteBestButton;
    private Button openBestRecipeButton;
    private Button showMatchDetailButton;
    private LinearLayout bestMatchCard;
    private LinearLayout moreResultsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_recipe_suggestions);

        viewModel = new AiRecipeSuggestionViewModel(createAiChefRepository());
        adapter = new RecipeMatchAdapter(this, match -> openRecipe(match.getRecipe().getId()));

        resultsSubtitle = findViewById(R.id.resultsSubtitle);
        bestHero = findViewById(R.id.bestHero);
        bestHeroImage = findViewById(R.id.bestHeroImage);
        bestRecipeName = findViewById(R.id.bestRecipeName);
        bestScore = findViewById(R.id.bestScore);
        bestReadyLabel = findViewById(R.id.bestReadyLabel);
        bestIngredientCount = findViewById(R.id.bestIngredientCount);
        bestMissing = findViewById(R.id.bestMissing);
        resultsStatus = findViewById(R.id.resultsStatus);
        favoriteBestButton = findViewById(R.id.favoriteBestButton);
        openBestRecipeButton = findViewById(R.id.openBestRecipeButton);
        showMatchDetailButton = findViewById(R.id.showMatchDetailButton);
        bestMatchCard = findViewById(R.id.bestMatchCard);
        moreResultsContainer = findViewById(R.id.moreResultsContainer);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        openBestRecipeButton.setOnClickListener(view -> {
            RecipeMatch match = currentState == null ? null : currentState.getBestMatch();
            if (match != null) {
                openRecipe(match.getRecipe().getId());
            }
        });
        favoriteBestButton.setOnClickListener(view -> {
            RecipeMatch match = currentState == null ? null : currentState.getBestMatch();
            if (match != null) {
                bindState(viewModel.toggleFavorite(match.getRecipe().getId()));
            }
        });
        showMatchDetailButton.setOnClickListener(view -> showLocalMatchSummary());
        bindBottomNavigation();

        bindState(viewModel.loadSuggestions());
        loadAiBackendSuggestions();
    }

    private AiChefRepository createAiChefRepository() {
        RecipeRepository recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        return new AiChefRepository(new AiChefLocalDataSource(this),
                recipeRepository,
                new AiBackendRemoteDataSource(BuildConfig.AI_BACKEND_BASE_URL));
    }

    private void loadAiBackendSuggestions() {
        resultsStatus.setText(resultsStatus.getText() + "\nĐang hỏi AI backend...");
        new Thread(() -> {
            AiRecipeSuggestionState aiState = viewModel.loadSuggestionsWithAiBackend();
            mainHandler.post(() -> bindState(aiState));
        }).start();
    }

    private void bindState(AiRecipeSuggestionState state) {
        currentState = state;
        RecipeMatch bestMatch = state.getBestMatch();
        resultsSubtitle.setText("AI đã phân tích " + state.getSelectedIngredients().size()
                + " nguyên liệu bạn đang có");
        resultsStatus.setText(state.getStatusMessage());
        if (bestMatch == null) {
            bestMatchCard.setAlpha(0.55f);
            bestHero.setText("AI Chef");
            bestHeroImage.setImageResource(R.drawable.cooking_step_preview);
            findViewById(R.id.bestHeroContainer).setBackground(createGradient(Color.parseColor("#C56A2C"), dp(26)));
            bestRecipeName.setText("Chưa có kết quả phù hợp");
            bestScore.setText("0%");
            bestReadyLabel.setText("Cần thêm nguyên liệu");
            bestIngredientCount.setText("Hãy quay lại màn nhập nguyên liệu để lưu rổ bếp nhà.");
            bestMissing.setText("Chưa có nguyên liệu đã xác nhận.");
            openBestRecipeButton.setEnabled(false);
            favoriteBestButton.setEnabled(false);
            showMatchDetailButton.setEnabled(false);
            moreResultsContainer.removeAllViews();
            return;
        }
        bestMatchCard.setAlpha(1f);
        bestHero.setText(bestMatch.getRecipe().getCategory());
        RecipeImageResolver.apply(bestHeroImage, bestMatch.getRecipe());
        bestHeroImage.setContentDescription(bestMatch.getRecipe().getName());
        findViewById(R.id.bestHeroContainer).setBackground(createGradient(bestMatch.getRecipe().getColorArgb(), dp(26)));
        bestRecipeName.setText(bestMatch.getRecipe().getName());
        bestScore.setText(bestMatch.getScorePercent() + "%");
        bestReadyLabel.setText(bestMatch.getReadinessLabel());
        bestIngredientCount.setText(String.format(Locale.US, "Đã có %d/%d nguyên liệu",
                bestMatch.getAvailableCount(), bestMatch.getRequiredCount()));
        bestMissing.setText(createMissingText(bestMatch));
        favoriteBestButton.setText(bestMatch.isFavorite() ? "Đã lưu" : "Lưu món");
        openBestRecipeButton.setEnabled(true);
        favoriteBestButton.setEnabled(true);
        showMatchDetailButton.setEnabled(true);
        adapter.bindOtherMatches(moreResultsContainer, state.getOtherMatches());
    }

    private String createMissingText(RecipeMatch match) {
        if (match.getMissingIngredients().isEmpty()) {
            return "Đủ nguyên liệu chính. Bạn có thể mở công thức và bắt đầu chuẩn bị.";
        }
        return "Thiếu: " + join(match.getMissingIngredients());
    }

    private void showLocalMatchSummary() {
        RecipeMatch match = currentState == null ? null : currentState.getBestMatch();
        if (match == null) {
            return;
        }
        Intent intent = new Intent(this, MatchDetailActivity.class);
        intent.putExtra(MatchDetailActivity.EXTRA_RECIPE_ID, match.getRecipe().getId());
        startActivity(intent);
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private void openRecipe(long recipeId) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipeId);
        startActivity(intent);
    }

    private void bindBottomNavigation() {
        findViewById(R.id.homeTab).setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.searchTab).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class)));
        findViewById(R.id.aiChefTab).setOnClickListener(view ->
                startActivity(new Intent(this, AiChefActivity.class)));
        findViewById(R.id.shoppingTab).setOnClickListener(view ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        findViewById(R.id.profileTab).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private GradientDrawable createGradient(int baseColor, int radius) {
        int darker = Color.rgb(
                Math.max(0, (int) (Color.red(baseColor) * 0.65f)),
                Math.max(0, (int) (Color.green(baseColor) * 0.65f)),
                Math.max(0, (int) (Color.blue(baseColor) * 0.65f)));
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{baseColor, darker});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
