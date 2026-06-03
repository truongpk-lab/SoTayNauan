package com.sotaynauan.ai.data.local.datasource;

import com.sotaynauan.ai.data.local.dao.RecipeDao;
import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.seed.SeedDataProvider;

import java.util.List;

public class RecipeLocalDataSource {
    private final RecipeDao recipeDao;
    private final SeedDataProvider seedDataProvider;

    public RecipeLocalDataSource(RecipeDao recipeDao, SeedDataProvider seedDataProvider) {
        this.recipeDao = recipeDao;
        this.seedDataProvider = seedDataProvider;
    }

    public void seedIfNeeded() {
        List<RecipeEntity> seedRecipes = seedDataProvider.createRecipes();
        if (recipeDao.countRecipes() == 0) {
            recipeDao.insertAll(seedRecipes);
            return;
        }
        for (RecipeEntity seedRecipe : seedRecipes) {
            RecipeEntity existingRecipe = recipeDao.findByName(seedRecipe.name);
            if (existingRecipe == null) {
                recipeDao.insert(seedRecipe);
            } else {
                seedRecipe.id = existingRecipe.id;
                recipeDao.update(seedRecipe);
            }
        }
    }

    public List<RecipeEntity> getTodaySuggestions() {
        seedIfNeeded();
        return recipeDao.getTodaySuggestions();
    }

    public List<RecipeEntity> getPopularRecipes() {
        seedIfNeeded();
        return recipeDao.getPopularRecipes();
    }

    public List<RecipeEntity> getAllRecipes() {
        seedIfNeeded();
        return recipeDao.getAllRecipes();
    }

    public List<RecipeEntity> getUserSavedRecipes() {
        seedIfNeeded();
        return recipeDao.getUserSavedRecipes();
    }

    public List<RecipeEntity> getFriendShares() {
        seedIfNeeded();
        return recipeDao.getFriendShares();
    }

    public RecipeEntity findById(long recipeId) {
        seedIfNeeded();
        return recipeDao.findById(recipeId);
    }

    public RecipeEntity findQuickSuggestion() {
        seedIfNeeded();
        return recipeDao.findQuickSuggestion();
    }

    public RecipeEntity findRandomQuickSuggestion() {
        seedIfNeeded();
        return recipeDao.findRandomQuickSuggestion();
    }

    public long addRecipe(RecipeEntity recipe) {
        seedIfNeeded();
        if (recipe == null || recipe.name == null || recipe.name.trim().isEmpty()
                || recipeDao.findByNormalizedName(recipe.name.trim()) != null) {
            return -1L;
        }
        recipe.name = recipe.name.trim();
        return recipeDao.insert(recipe);
    }
}
