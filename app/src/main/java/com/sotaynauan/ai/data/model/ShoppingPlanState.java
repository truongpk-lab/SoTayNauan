package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShoppingPlanState {
    private final long recipeId;
    private final String recipeName;
    private final List<ShoppingPlanItem> items;
    private final long updatedAtMillis;
    private final String statusMessage;

    public ShoppingPlanState(long recipeId, String recipeName,
                             List<String> missingIngredients, long updatedAtMillis) {
        this(recipeId, recipeName, toItems(missingIngredients), updatedAtMillis, "");
    }

    public ShoppingPlanState(long recipeId, String recipeName, List<ShoppingPlanItem> items,
                             long updatedAtMillis, String statusMessage) {
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.items = new ArrayList<>(items);
        this.updatedAtMillis = updatedAtMillis;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public List<String> getMissingIngredients() {
        List<String> ingredients = new ArrayList<>();
        for (ShoppingPlanItem item : items) {
            if (item.getStatus() == ShoppingItemStatus.NEED_BUY) {
                ingredients.add(item.getName());
            }
        }
        return Collections.unmodifiableList(ingredients);
    }

    public List<ShoppingPlanItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getNeedBuyCount() {
        int count = 0;
        for (ShoppingPlanItem item : items) {
            if (item.getStatus() == ShoppingItemStatus.NEED_BUY) {
                count++;
            }
        }
        return count;
    }

    public int getShoppingListCount() {
        int count = 0;
        for (ShoppingPlanItem item : items) {
            if (item.isActiveForShoppingList()) {
                count++;
            }
        }
        return count;
    }

    private static List<ShoppingPlanItem> toItems(List<String> ingredients) {
        List<ShoppingPlanItem> result = new ArrayList<>();
        for (String ingredient : ingredients) {
            if (ingredient != null && !ingredient.trim().isEmpty()) {
                String name = ingredient.trim();
                result.add(new ShoppingPlanItem(name.toLowerCase(), name, 1, "phần",
                        "", "Nguyên liệu chính", ShoppingItemStatus.NEED_BUY));
            }
        }
        return result;
    }
}
