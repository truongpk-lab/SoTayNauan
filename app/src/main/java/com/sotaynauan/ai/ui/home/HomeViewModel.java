package com.sotaynauan.ai.ui.home;

import com.sotaynauan.ai.data.model.HomeContent;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.RecipeRepository;

public class HomeViewModel {
    private final RecipeRepository recipeRepository;

    public HomeViewModel(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public HomeContent loadHomeContent() {
        return recipeRepository.getHomeContent();
    }

    public Recipe loadRecipe(long recipeId) {
        return recipeRepository.findRecipe(recipeId);
    }

    public Recipe loadQuickSuggestion() {
        return recipeRepository.getQuickSuggestion();
    }
}
