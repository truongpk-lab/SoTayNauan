package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IngredientConfirmState {
    private final List<ConfirmedIngredient> ingredients;
    private final long updatedAtMillis;
    private final String lastAction;

    public IngredientConfirmState(List<ConfirmedIngredient> ingredients,
                                  long updatedAtMillis,
                                  String lastAction) {
        this.ingredients = new ArrayList<>(ingredients);
        this.updatedAtMillis = updatedAtMillis;
        this.lastAction = lastAction;
    }

    public List<ConfirmedIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public String getLastAction() {
        return lastAction;
    }

    public int getTotalCount() {
        return ingredients.size();
    }

    public int getSelectedCount() {
        int count = 0;
        for (ConfirmedIngredient ingredient : ingredients) {
            if (ingredient.isSelected()) {
                count++;
            }
        }
        return count;
    }
}
