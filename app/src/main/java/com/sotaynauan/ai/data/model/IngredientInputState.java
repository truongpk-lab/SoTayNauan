package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IngredientInputState {
    private final List<String> ingredients;
    private final long updatedAtMillis;
    private final String lastAction;

    public IngredientInputState(List<String> ingredients, long updatedAtMillis, String lastAction) {
        this.ingredients = new ArrayList<>(ingredients);
        this.updatedAtMillis = updatedAtMillis;
        this.lastAction = lastAction;
    }

    public List<String> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public String getLastAction() {
        return lastAction;
    }

    public int getCount() {
        return ingredients.size();
    }
}
