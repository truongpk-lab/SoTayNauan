package com.sotaynauan.ai.ui.recipe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.recipe.RecipeIngredientAdapter;
import com.sotaynauan.ai.adapter.recipe.RecipeStepAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeDetailLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.model.RecipeDetailState;
import com.sotaynauan.ai.data.repository.CookingRepository;
import com.sotaynauan.ai.data.repository.RecipeDetailRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.ui.cooking.CookingPreparationActivity;
import com.sotaynauan.ai.ui.cooking.CookingModeActivity;
import com.sotaynauan.ai.ui.shopping.ShoppingPlanActivity;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.Locale;

public class RecipeDetailActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private RecipeDetailViewModel viewModel;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
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
            Intent intent = new Intent(this, CookingPreparationActivity.class);
            intent.putExtra(CookingPreparationActivity.EXTRA_RECIPE_ID, recipeId);
            startActivity(intent);
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
        startCookingButton.setEnabled(true);
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
