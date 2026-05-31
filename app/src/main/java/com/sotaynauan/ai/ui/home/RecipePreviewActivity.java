package com.sotaynauan.ai.ui.home;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.List;
import java.util.Locale;

public class RecipePreviewActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private HomeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_preview);

        viewModel = new HomeViewModel(createRecipeRepository());
        findViewById(R.id.backButton).setOnClickListener(view -> finish());

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        Recipe recipe = viewModel.loadRecipe(recipeId);
        if (recipe == null) {
            finish();
            return;
        }
        bindRecipe(recipe);
    }

    private RecipeRepository createRecipeRepository() {
        return new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
    }

    private void bindRecipe(Recipe recipe) {
        TextView hero = findViewById(R.id.recipeHero);
        hero.setText(recipe.getCategory());
        findViewById(R.id.recipeHeroContainer).setBackground(createHeroBackground(recipe.getColorArgb()));
        ImageView heroImage = findViewById(R.id.recipeHeroImage);
        RecipeImageResolver.apply(heroImage, recipe);
        heroImage.setContentDescription(recipe.getName());

        TextView recipeName = findViewById(R.id.recipeName);
        TextView recipeMeta = findViewById(R.id.recipeMeta);
        TextView recipeDescription = findViewById(R.id.recipeDescription);
        TextView recipeIngredients = findViewById(R.id.recipeIngredients);
        TextView recipeSteps = findViewById(R.id.recipeSteps);

        recipeName.setText(recipe.getName());
        recipeMeta.setText(String.format(Locale.US, "%d phút  •  %s  •  %s",
                recipe.getTotalMinutes(), recipe.getDifficulty(), recipe.getCategory()));
        recipeDescription.setText(recipe.getDescription());
        recipeIngredients.setText(numbered(recipe.getIngredients(), false));
        recipeSteps.setText(numbered(recipe.getSteps(), true));
    }


    private String numbered(List<String> values, boolean useNumbers) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append("\n");
            }
            if (useNumbers) {
                builder.append(index + 1).append(". ");
            } else {
                builder.append("- ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private GradientDrawable createHeroBackground(int baseColor) {
        int darker = Color.rgb(
                Math.max(0, (int) (Color.red(baseColor) * 0.68f)),
                Math.max(0, (int) (Color.green(baseColor) * 0.68f)),
                Math.max(0, (int) (Color.blue(baseColor) * 0.68f)));
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{baseColor, darker});
        drawable.setCornerRadius(Math.round(26 * getResources().getDisplayMetrics().density));
        return drawable;
    }
}
