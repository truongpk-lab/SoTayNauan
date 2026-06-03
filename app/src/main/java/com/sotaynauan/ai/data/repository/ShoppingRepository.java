package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.model.RecipeMatch;
import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanState;

import java.util.List;

public class ShoppingRepository {
    private final ShoppingLocalDataSource localDataSource;

    public ShoppingRepository(ShoppingLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    public ShoppingPlanState getCurrentPlan() {
        return localDataSource.getCurrentPlan();
    }

    public ShoppingPlanState getShoppingList() {
        return localDataSource.getShoppingList();
    }

    public ShoppingPlanState saveMissingIngredientsForMatch(RecipeMatch match) {
        return localDataSource.savePlan(
                match.getRecipe().getId(),
                match.getRecipe().getName(),
                match.getMissingIngredients());
    }

    public ShoppingPlanState saveIngredientsForRecipe(Recipe recipe) {
        return localDataSource.savePlan(recipe.getId(), recipe.getName(), recipe.getIngredients());
    }

    public ShoppingPlanState saveShoppingListForRecipe(long recipeId, String recipeName, List<String> ingredients) {
        return localDataSource.saveShoppingList(recipeId, recipeName, ingredients);
    }

    public ShoppingPlanState updateItemStatus(String itemId, ShoppingItemStatus status) {
        return localDataSource.updateStatus(itemId, status);
    }

    public ShoppingPlanState updateShoppingListItemStatus(String itemId, ShoppingItemStatus status) {
        return localDataSource.updateStatus(itemId, status, true);
    }

    public ShoppingPlanState increaseQuantity(String itemId) {
        return localDataSource.adjustQuantity(itemId, 1);
    }

    public ShoppingPlanState increaseShoppingListQuantity(String itemId) {
        return localDataSource.adjustQuantity(itemId, 1, true);
    }

    public ShoppingPlanState decreaseQuantity(String itemId) {
        return localDataSource.adjustQuantity(itemId, -1);
    }

    public ShoppingPlanState decreaseShoppingListQuantity(String itemId) {
        return localDataSource.adjustQuantity(itemId, -1, true);
    }

    public ShoppingPlanState addCustomItem(String name, int amount, String unit, String category) {
        return localDataSource.addCustomItem(name, amount, unit, category);
    }

    public ShoppingPlanState addCustomShoppingListItem(String name, int amount, String unit, String category) {
        return localDataSource.addCustomItem(name, amount, unit, category, true);
    }

    public ShoppingPlanState updateItem(String itemId, String name, int amount, String unit, String category) {
        return localDataSource.updateItem(itemId, name, amount, unit, category);
    }

    public ShoppingPlanState updateShoppingListItem(String itemId, String name, int amount, String unit, String category) {
        return localDataSource.updateItem(itemId, name, amount, unit, category, true);
    }

    public ShoppingPlanState removeItem(String itemId) {
        return localDataSource.removeItem(itemId);
    }

    public ShoppingPlanState removeShoppingListItem(String itemId) {
        return localDataSource.removeItem(itemId, true);
    }

    public ShoppingPlanState finishMarketTrip() {
        return localDataSource.finishMarketTrip();
    }

    public ShoppingPlanState consumeIngredientsForCookedRecipe(List<String> recipeIngredients, String recipeName) {
        return localDataSource.consumeIngredientsForCookedRecipe(recipeIngredients, recipeName);
    }

    public ShoppingPlanState createShoppingListFromPlan() {
        return localDataSource.commitShoppingList();
    }
}
