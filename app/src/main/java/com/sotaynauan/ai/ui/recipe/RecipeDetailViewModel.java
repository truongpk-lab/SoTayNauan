package com.sotaynauan.ai.ui.recipe;

import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.RecipeDetailState;
import com.sotaynauan.ai.data.repository.CookingRepository;
import com.sotaynauan.ai.data.repository.RecipeDetailRepository;

public class RecipeDetailViewModel {
    private final RecipeDetailRepository recipeDetailRepository;
    private final CookingRepository cookingRepository;

    public RecipeDetailViewModel(RecipeDetailRepository recipeDetailRepository,
                                 CookingRepository cookingRepository) {
        this.recipeDetailRepository = recipeDetailRepository;
        this.cookingRepository = cookingRepository;
    }

    public RecipeDetailState loadRecipeDetail(long recipeId) {
        return recipeDetailRepository.loadRecipeDetail(recipeId);
    }

    public RecipeDetailState toggleFavorite(long recipeId) {
        return recipeDetailRepository.toggleFavorite(recipeId);
    }

    public RecipeDetailState toggleIngredient(long recipeId, String ingredient) {
        return recipeDetailRepository.toggleIngredient(recipeId, ingredient);
    }

    public RecipeDetailState addIngredientsToShopping(long recipeId) {
        return recipeDetailRepository.addIngredientsToShopping(recipeId);
    }

    public CookingSessionState startCooking(long recipeId) {
        return cookingRepository.startSession(recipeId);
    }
}
