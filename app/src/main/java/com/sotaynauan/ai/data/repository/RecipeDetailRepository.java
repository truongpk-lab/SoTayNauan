package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.RecipeDetailLocalDataSource;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.model.RecipeDetailState;
import com.sotaynauan.ai.data.model.ShoppingPlanState;

import java.util.List;

public class RecipeDetailRepository {
    private final RecipeRepository recipeRepository;
    private final RecipeDetailLocalDataSource localDataSource;
    private final ShoppingRepository shoppingRepository;

    public RecipeDetailRepository(RecipeRepository recipeRepository,
                                  RecipeDetailLocalDataSource localDataSource,
                                  ShoppingRepository shoppingRepository) {
        this.recipeRepository = recipeRepository;
        this.localDataSource = localDataSource;
        this.shoppingRepository = shoppingRepository;
    }

    public RecipeDetailState loadRecipeDetail(long recipeId) {
        Recipe recipe = recipeRepository.findRecipe(recipeId);
        if (recipe == null) {
            return new RecipeDetailState(null, false, java.util.Collections.emptyList(),
                    "Không tìm thấy công thức trong kho local.");
        }
        return createState(recipe, "Công thức được tải từ Room qua Repository.");
    }

    public RecipeDetailState toggleFavorite(long recipeId) {
        Recipe recipe = recipeRepository.findRecipe(recipeId);
        if (recipe == null) {
            return loadRecipeDetail(recipeId);
        }
        boolean favorite = localDataSource.toggleFavoriteRecipe(recipeId);
        return createState(recipe, favorite
                ? "Đã lưu món này vào yêu thích local."
                : "Đã bỏ món này khỏi yêu thích local.");
    }

    public RecipeDetailState toggleIngredient(long recipeId, String ingredient) {
        Recipe recipe = recipeRepository.findRecipe(recipeId);
        if (recipe == null) {
            return loadRecipeDetail(recipeId);
        }
        List<String> checked = localDataSource.toggleCheckedIngredient(recipeId, ingredient);
        return new RecipeDetailState(recipe, localDataSource.isFavoriteRecipe(recipeId), checked,
                "Đã tick " + checked.size() + "/" + recipe.getIngredients().size()
                        + " nguyên liệu và lưu trên thiết bị.");
    }

    public RecipeDetailState addIngredientsToShopping(long recipeId) {
        Recipe recipe = recipeRepository.findRecipe(recipeId);
        if (recipe == null) {
            return loadRecipeDetail(recipeId);
        }
        ShoppingPlanState planState = shoppingRepository.saveIngredientsForRecipe(recipe);
        return createState(recipe, "Đã lưu " + planState.getMissingIngredients().size()
                + " nguyên liệu vào danh sách đi chợ local.");
    }

    private RecipeDetailState createState(Recipe recipe, String statusMessage) {
        return new RecipeDetailState(recipe,
                localDataSource.isFavoriteRecipe(recipe.getId()),
                localDataSource.getCheckedIngredients(recipe.getId()),
                statusMessage);
    }
}
