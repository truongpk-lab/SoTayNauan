package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeMatch {
    private final Recipe recipe;
    private final int scorePercent;
    private final List<String> availableIngredients;
    private final List<String> missingIngredients;
    private final String readinessLabel;
    private final boolean favorite;

    public RecipeMatch(Recipe recipe,
                       int scorePercent,
                       List<String> availableIngredients,
                       List<String> missingIngredients,
                       String readinessLabel,
                       boolean favorite) {
        this.recipe = recipe;
        this.scorePercent = scorePercent;
        this.availableIngredients = new ArrayList<>(availableIngredients);
        this.missingIngredients = new ArrayList<>(missingIngredients);
        this.readinessLabel = readinessLabel;
        this.favorite = favorite;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getScorePercent() {
        return scorePercent;
    }

    public List<String> getAvailableIngredients() {
        return Collections.unmodifiableList(availableIngredients);
    }

    public List<String> getMissingIngredients() {
        return Collections.unmodifiableList(missingIngredients);
    }

    public String getReadinessLabel() {
        return readinessLabel;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public int getRequiredCount() {
        return recipe.getIngredients().size();
    }

    public int getAvailableCount() {
        return availableIngredients.size();
    }

    public RecipeMatch withFavorite(boolean nextFavorite) {
        return new RecipeMatch(recipe, scorePercent, availableIngredients, missingIngredients,
                readinessLabel, nextFavorite);
    }
}
