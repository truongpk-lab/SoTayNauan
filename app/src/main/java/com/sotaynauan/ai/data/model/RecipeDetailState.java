package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeDetailState {
    private final Recipe recipe;
    private final boolean favorite;
    private final List<String> checkedIngredients;
    private final String statusMessage;

    public RecipeDetailState(Recipe recipe,
                             boolean favorite,
                             List<String> checkedIngredients,
                             String statusMessage) {
        this.recipe = recipe;
        this.favorite = favorite;
        this.checkedIngredients = new ArrayList<>(checkedIngredients);
        this.statusMessage = statusMessage;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public List<String> getCheckedIngredients() {
        return Collections.unmodifiableList(checkedIngredients);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getCheckedCount() {
        return checkedIngredients.size();
    }

    public boolean hasRecipe() {
        return recipe != null;
    }
}
