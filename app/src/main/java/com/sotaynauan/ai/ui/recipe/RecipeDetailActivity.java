package com.sotaynauan.ai.ui.recipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.recipe.RecipeIngredientAdapter;
import com.sotaynauan.ai.adapter.recipe.RecipeStepAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeDetailLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;
import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.model.RecipeDetailState;
import com.sotaynauan.ai.data.repository.CookingPreparationRepository;
import com.sotaynauan.ai.data.repository.CookingRepository;
import com.sotaynauan.ai.data.repository.RecipeDetailRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.cooking.CookingPreparationActivity;
import com.sotaynauan.ai.ui.cooking.CookingModeActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingPlanActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingListActivity;
import com.sotaynauan.ai.util.AppExecutors;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeDetailActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private RecipeDetailViewModel viewModel;
    private CookingPreparationRepository preparationRepository;
    private RecipeIngredientAdapter ingredientAdapter;
    private RecipeStepAdapter stepAdapter;
    private long recipeId;
    private RecipeDetailState currentState;

    private TextView heroTitle;
    private TextView heroCategory;
    private ImageView heroImage;
    private TextView favoriteButton;
    private TextView ratingChip;
    private TextView timeChip;
    private TextView servingChip;
    private TextView difficultyChip;
    private TextView descriptionText;
    private TextView aiTipText;
    private TextView ingredientTitle;
    private TextView statusText;
    private LinearLayout ingredientContainer;
    private LinearLayout stepContainer;
    private Button shoppingButton;
    private Button prepareButton;
    private Button startCookingButton;
    private boolean startCookingInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        preparationRepository = new CookingPreparationRepository(AppDatabase.getInstance(this));
        viewModel = createViewModel();
        ingredientAdapter = new RecipeIngredientAdapter(this,
                ingredient -> bindState(viewModel.toggleIngredient(recipeId, ingredient)));
        stepAdapter = new RecipeStepAdapter(this);

        bindViews();
        bindActions();
        bindState(viewModel.loadRecipeDetail(recipeId));
    }

    private RecipeDetailViewModel createViewModel() {
        RecipeRepository recipeRepository = createRecipeRepository();
        ShoppingRepository shoppingRepository = new ShoppingRepository(new ShoppingLocalDataSource(this));
        return new RecipeDetailViewModel(
                new RecipeDetailRepository(recipeRepository,
                        new RecipeDetailLocalDataSource(this),
                        shoppingRepository),
                new CookingRepository(new CookingLocalDataSource(this), recipeRepository,
                        shoppingRepository));
    }

    private RecipeRepository createRecipeRepository() {
        return new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
    }

    private void bindViews() {
        heroTitle = findViewById(R.id.recipeDetailHeroTitle);
        heroCategory = findViewById(R.id.recipeDetailHeroCategory);
        heroImage = findViewById(R.id.recipeDetailHeroImage);
        favoriteButton = findViewById(R.id.recipeDetailFavoriteButton);
        ratingChip = findViewById(R.id.recipeDetailRatingChip);
        timeChip = findViewById(R.id.recipeDetailTimeChip);
        servingChip = findViewById(R.id.recipeDetailServingChip);
        difficultyChip = findViewById(R.id.recipeDetailDifficultyChip);
        descriptionText = findViewById(R.id.recipeDetailDescription);
        aiTipText = findViewById(R.id.recipeDetailAiTip);
        ingredientTitle = findViewById(R.id.recipeDetailIngredientTitle);
        statusText = findViewById(R.id.recipeDetailStatus);
        ingredientContainer = findViewById(R.id.recipeDetailIngredientContainer);
        stepContainer = findViewById(R.id.recipeDetailStepContainer);
        shoppingButton = findViewById(R.id.recipeDetailShoppingButton);
        prepareButton = findViewById(R.id.recipeDetailPrepareButton);
        startCookingButton = findViewById(R.id.recipeDetailStartCookingButton);
    }

    private void bindActions() {
        findViewById(R.id.recipeDetailBackButton).setOnClickListener(view -> finish());
        favoriteButton.setOnClickListener(view -> bindState(viewModel.toggleFavorite(recipeId)));
        shoppingButton.setOnClickListener(view -> {
            bindState(viewModel.addIngredientsToShopping(recipeId));
            startActivity(new Intent(this, ShoppingPlanActivity.class));
        });
        prepareButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, CookingPreparationActivity.class);
            intent.putExtra(CookingPreparationActivity.EXTRA_RECIPE_ID, recipeId);
            startActivity(intent);
        });
        startCookingButton.setOnClickListener(view -> {
            handleStartCooking();
        });
    }

    private void bindState(RecipeDetailState state) {
        currentState = state;
        statusText.setText(state.getStatusMessage());
        if (!state.hasRecipe()) {
            startCookingButton.setEnabled(false);
            shoppingButton.setEnabled(false);
            prepareButton.setEnabled(false);
            favoriteButton.setEnabled(false);
            heroTitle.setText("Không tìm thấy công thức");
            return;
        }

        Recipe recipe = state.getRecipe();
        recipeId = recipe.getId();
        heroTitle.setText(recipe.getName());
        heroCategory.setText(recipe.getCategory());
        findViewById(R.id.recipeDetailHero).setBackground(createHeroBackground(recipe.getColorArgb()));
        RecipeImageResolver.apply(heroImage, recipe);
        heroImage.setContentDescription(recipe.getName());
        favoriteButton.setText(state.isFavorite() ? "♥" : "♡");
        ratingChip.setText(recipe.getCalories().isEmpty()
                ? String.format(Locale.US, "%.1f", recipe.getPopularityScore() / 20f)
                : recipe.getCalories().replace("/phần", ""));
        timeChip.setText(recipe.getTotalMinutes() + "m");
        servingChip.setText(recipe.getServing().isEmpty() ? suggestServing(recipe) : recipe.getServing());
        difficultyChip.setText(recipe.getDifficulty());
        descriptionText.setText(recipe.getDescription()
                + (recipe.getCost().isEmpty() ? "" : "\nChi phí dự kiến: " + recipe.getCost()));
        aiTipText.setText(createAiTip(recipe));
        ingredientTitle.setText("Danh sách (" + recipe.getIngredients().size() + ")");
        ingredientAdapter.bind(ingredientContainer, recipe.getIngredients(), state.getCheckedIngredients());
        stepAdapter.bind(stepContainer, recipe.getSteps());
        startCookingButton.setEnabled(!startCookingInProgress);
        startCookingButton.setAlpha(startCookingInProgress ? 0.72f : 1f);
        shoppingButton.setEnabled(true);
        prepareButton.setEnabled(true);
        favoriteButton.setEnabled(true);
    }

    private String suggestServing(Recipe recipe) {
        if (recipe.getTotalMinutes() <= 20) {
            return "2-3";
        }
        return "3-4";
    }

    private String createAiTip(Recipe recipe) {
        String lower = recipe.getName().toLowerCase();
        if (lower.contains("bò")) {
            return "Để bò mềm hơn, hãy ướp với một chút dầu ăn và bột năng trước khi xào. Không xào quá lâu để giữ độ ngọt của thịt.";
        }
        if (lower.contains("trứng")) {
            return "Đánh trứng nhẹ tay và tắt bếp sớm hơn một chút để món giữ độ mềm, không bị khô.";
        }
        if (lower.contains("gà")) {
            return "Kho lửa nhỏ sau khi thịt săn sẽ giúp gà thấm vị mà không bị khô.";
        }
        return "Chuẩn bị đủ nguyên liệu trước khi bật bếp để các bước nấu liền mạch và món giữ được vị ngon.";
    }

    private void handleStartCooking() {
        if (startCookingInProgress || currentState == null || !currentState.hasRecipe()) {
            return;
        }
        long targetRecipeId = recipeId;
        List<String> checkedIngredients = new ArrayList<>(currentState.getCheckedIngredients());
        setStartCookingBusy(true, "Đang kiểm tra kho nguyên liệu...");
        AppExecutors.runOnIo(() -> {
            try {
                CookingPlanEntity plan = preparationRepository
                        .preparePlanFromRecipe(targetRecipeId, 0, checkedIngredients);
                List<CookingPlanIngredientEntity> missingIngredients =
                        preparationRepository.getMissingIngredients(plan.id);
                if (isActive()) {
                    runOnUiThread(() -> {
                        if (!isActive()) {
                            return;
                        }
                    setStartCookingBusy(false, "");
                    if (missingIngredients.isEmpty()) {
                        openCookingMode(plan.id);
                    } else {
                        showMissingIngredientsDialog(plan.id, missingIngredients);
                    }
                    });
                }
            } catch (Exception exception) {
                if (isActive()) {
                    runOnUiThread(() -> {
                        if (!isActive()) {
                            return;
                        }
                    String message = "Chưa thể bắt đầu nấu: " + safeErrorMessage(exception);
                    setStartCookingBusy(false, message);
                    showStartCookingErrorDialog(message);
                    });
                }
            }
        });
    }

    private void setStartCookingBusy(boolean busy, String message) {
        startCookingInProgress = busy;
        startCookingButton.setEnabled(!busy);
        startCookingButton.setAlpha(busy ? 0.72f : 1f);
        startCookingButton.setText(busy ? "Đang kiểm tra..." : "Bắt đầu nấu");
        if (message != null && !message.trim().isEmpty()) {
            statusText.setText(message);
        }
    }

    private void openCookingMode(String planId) {
        viewModel.startCooking(recipeId, planId);
        Intent intent = new Intent(this, CookingModeActivity.class);
        intent.putExtra(CookingModeActivity.EXTRA_RECIPE_ID, recipeId);
        intent.putExtra(CookingPreparationActivity.EXTRA_PLAN_ID, planId);
        startActivity(intent);
    }

    private void showMissingIngredientsDialog(String planId,
                                              List<CookingPlanIngredientEntity> missingIngredients) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(6), dp(18), 0);

        TextView intro = new TextView(this);
        intro.setText("Bạn còn thiếu nguyên liệu dưới đây. Hãy đi chợ nhé.");
        intro.setTextColor(Color.parseColor("#564337"));
        intro.setTextSize(16);
        intro.setLineSpacing(dp(3), 1f);
        content.addView(intro, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (CookingPlanIngredientEntity ingredient : missingIngredients) {
            content.addView(createMissingIngredientRow(ingredient));
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(420)));

        new AlertDialog.Builder(this)
                .setTitle("Thiếu nguyên liệu")
                .setView(scrollView)
                .setPositiveButton("Thêm vào danh sách đi chợ", (dialog, which) -> {
                    addMissingIngredientsAndOpenShopping(planId);
                })
                .setNegativeButton("Bỏ qua", null)
                .show();
    }

    private void addMissingIngredientsAndOpenShopping(String planId) {
        setStartCookingBusy(true, "Đang thêm nguyên liệu thiếu vào danh sách đi chợ...");
        AppExecutors.runOnIo(() -> {
            try {
                int added = preparationRepository.addMissingIngredientsToShoppingList(planId);
                if (isActive()) {
                    runOnUiThread(() -> {
                        if (!isActive()) {
                            return;
                        }
                    setStartCookingBusy(false,
                            "Đã thêm " + added + " nguyên liệu cần mua vào danh sách đi chợ.");
                    startActivity(new Intent(this, ShoppingListActivity.class));
                    });
                }
            } catch (Exception exception) {
                if (isActive()) {
                    runOnUiThread(() -> {
                        if (!isActive()) {
                            return;
                        }
                    String message = "Không thể thêm nguyên liệu thiếu: " + safeErrorMessage(exception);
                    setStartCookingBusy(false, message);
                    showStartCookingErrorDialog(message);
                    });
                }
            }
        });
    }

    private void showStartCookingErrorDialog(String message) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(6), dp(18), 0);

        TextView intro = new TextView(this);
        intro.setText(message + "\n\nBạn có thể mở màn chuẩn bị để kiểm tra lại kho và danh sách còn thiếu.");
        intro.setTextColor(Color.parseColor("#564337"));
        intro.setTextSize(16);
        intro.setLineSpacing(dp(3), 1f);
        content.addView(intro, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Không thể bắt đầu nấu")
                .setView(content)
                .setPositiveButton("Mở chuẩn bị", (dialog, which) -> {
                    Intent intent = new Intent(this, CookingPreparationActivity.class);
                    intent.putExtra(CookingPreparationActivity.EXTRA_RECIPE_ID, recipeId);
                    startActivity(intent);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        if (message.contains("FOREIGN KEY")) {
            return "dữ liệu nguyên liệu chưa đồng bộ. App đã được cập nhật để tự sửa liên kết, hãy thử lại.";
        }
        return message;
    }

    private TextView createMissingIngredientRow(CookingPlanIngredientEntity ingredient) {
        TextView row = new TextView(this);
        row.setText(displayName(ingredient) + " - " + displayAmount(ingredient));
        row.setTextColor(Color.parseColor("#2E150B"));
        row.setTextSize(16);
        row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setBackground(createMissingIngredientBackground());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(params);
        return row;
    }

    private String displayName(CookingPlanIngredientEntity ingredient) {
        IngredientEntity entity = preparationRepository.getIngredient(ingredient.ingredientId);
        if (entity != null && entity.name != null && !entity.name.trim().isEmpty()) {
            return entity.name.trim();
        }
        return ingredient.ingredientId;
    }

    private String displayAmount(CookingPlanIngredientEntity ingredient) {
        if (preparationRepository.isPresenceOnly(ingredient)) {
            return "chỉ cần có";
        }
        if (Math.abs(ingredient.missingAmount - Math.round(ingredient.missingAmount)) < 0.0001d) {
            return Math.round(ingredient.missingAmount) + " " + ingredient.baseUnit;
        }
        return String.format(Locale.US, "%.1f %s", ingredient.missingAmount, ingredient.baseUnit);
    }

    private GradientDrawable createMissingIngredientBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#FFF1EC"));
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), Color.parseColor("#DCC1B1"));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isActive() {
        return !isFinishing() && !isDestroyed();
    }

    private GradientDrawable createHeroBackground(int baseColor) {
        int light = Color.rgb(
                Math.min(255, (int) (Color.red(baseColor) * 1.25f)),
                Math.min(255, (int) (Color.green(baseColor) * 1.16f)),
                Math.min(255, (int) (Color.blue(baseColor) * 1.08f)));
        int dark = Color.rgb(
                Math.max(0, (int) (Color.red(baseColor) * 0.58f)),
                Math.max(0, (int) (Color.green(baseColor) * 0.58f)),
                Math.max(0, (int) (Color.blue(baseColor) * 0.58f)));
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{light, baseColor, dark});
    }
}
