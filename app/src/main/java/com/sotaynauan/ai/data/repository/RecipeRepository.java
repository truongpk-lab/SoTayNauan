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

    public Recipe findRecipe(long recipeId) {
        RecipeEntity entity = localDataSource.findById(recipeId);
        return entity == null ? null : mapper.toModel(entity);
    }

    public Recipe getQuickSuggestion() {
        RecipeEntity entity = localDataSource.findQuickSuggestion();
        return entity == null ? null : mapper.toModel(entity);
    }
}
