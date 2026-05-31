package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.HomeContent;
import com.sotaynauan.ai.data.model.Recipe;

public class RecipeRepository {
    private final RecipeLocalDataSource localDataSource;
    private final RecipeMapper mapper;

    public RecipeRepository(RecipeLocalDataSource localDataSource, RecipeMapper mapper) {
        this.localDataSource = localDataSource;
        this.mapper = mapper;
    }

    public HomeContent getHomeContent() {
        return new HomeContent(
                mapper.toModels(localDataSource.getTodaySuggestions()),
                mapper.toModels(localDataSource.getPopularRecipes()),
                mapper.toModels(localDataSource.getFriendShares())
        );
    }

    public java.util.List<Recipe> getAllRecipes() {
        return mapper.toModels(localDataSource.getAllRecipes());
    }

    public java.util.List<Recipe> getUserSavedRecipes() {
        return mapper.toModels(localDataSource.getUserSavedRecipes());
    }

    public Recipe findRecipe(long recipeId) {
        RecipeEntity entity = localDataSource.findById(recipeId);
        return entity == null ? null : mapper.toModel(entity);
    }

    public Recipe getQuickSuggestion() {
        RecipeEntity entity = localDataSource.findQuickSuggestion();
        return entity == null ? null : mapper.toModel(entity);
    }

    public Recipe getRandomQuickSuggestion() {
        RecipeEntity entity = localDataSource.findRandomQuickSuggestion();
        return entity == null ? null : mapper.toModel(entity);
    }

    public Recipe addRecipe(String name, String description, int totalMinutes, String difficulty,
                            String category, String imageName, String serving, String calories,
                            String cost, java.util.List<String> ingredients,
                            java.util.List<String> steps) {
        RecipeEntity entity = mapper.toEntity(name, description, totalMinutes, difficulty,
                category, imageName, serving, calories, cost, 0xFFC56A2C, 50,
                false, "", "", ingredients, steps);
        long id = localDataSource.addRecipe(entity);
        if (id <= 0L) {
            return null;
        }
        return findRecipe(id);
    }
}
