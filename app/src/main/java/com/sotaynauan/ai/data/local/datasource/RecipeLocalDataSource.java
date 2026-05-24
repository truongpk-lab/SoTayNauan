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
        if (recipeDao.countRecipes() == 0) {
            recipeDao.insertAll(seedDataProvider.createRecipes());
            return;
        }
        for (RecipeEntity recipe : seedDataProvider.createRecipes()) {
            if (recipeDao.findByName(recipe.name) == null) {
                recipeDao.insert(recipe);
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
}
