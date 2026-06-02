package com.sotaynauan.ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;
import com.sotaynauan.ai.data.local.entity.PantryStockEntity;
import com.sotaynauan.ai.data.local.entity.RecipeEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CookingPreparationRepositoryTest {
    private AppDatabase database;
    private CookingPreparationRepository preparationRepository;
    private InventoryRepository inventoryRepository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        preparationRepository = new CookingPreparationRepository(database);
        inventoryRepository = new InventoryRepository(database);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void reservePartialHomeAmountCalculatesMissingAmount() {
        long recipeId = insertRecipe("Thịt kho test", "Thịt heo: 500 g");
        inventoryRepository.addPurchase("thit_heo", 200d, "g", "seed", null);

        CookingPlanIngredientEntity item = onlyItem(preparationRepository
                .createPlanFromRecipe(recipeId, 2).id);
        preparationRepository.markHaveAtHome(item.planId, item.ingredientId, 200d, "g");

        CookingPlanIngredientEntity updated = onlyItem(item.planId);
        assertEquals(200d, updated.homeSelectedAmount, 0.0001d);
        assertEquals(300d, updated.missingAmount, 0.0001d);
    }

    @Test
    public void reserveEnoughHomeStockThenCompleteLeavesRemainder() {
        long recipeId = insertRecipe("Thịt kho test", "Thịt heo: 500 g");
        inventoryRepository.addPurchase("thit_heo", 700d, "g", "seed", null);
        String planId = preparationRepository.createPlanFromRecipe(recipeId, 2).id;
        CookingPlanIngredientEntity item = onlyItem(planId);

        preparationRepository.markHaveAtHome(planId, item.ingredientId, 500d, "g");
        assertTrue(preparationRepository.isPlanReadyToCook(planId));
        preparationRepository.completeCooking(planId);

        PantryStockEntity stock = database.pantryDao().getStock("thit_heo");
        assertEquals(200d, stock.totalAmount, 0.0001d);
        assertEquals(0d, stock.reservedAmount, 0.0001d);
    }

    @Test
    public void buyMoreThanMissingLeavesExtraAfterCooking() {
        long recipeId = insertRecipe("Thịt kho test", "Thịt heo: 500 g");
        inventoryRepository.addPurchase("thit_heo", 200d, "g", "seed", null);
        String planId = preparationRepository.createPlanFromRecipe(recipeId, 2).id;
        CookingPlanIngredientEntity item = onlyItem(planId);

        preparationRepository.markHaveAtHome(planId, item.ingredientId, 200d, "g");
        preparationRepository.markBought(planId, item.ingredientId, 500d, "g");
        preparationRepository.completeCooking(planId);

        PantryStockEntity stock = database.pantryDao().getStock("thit_heo");
        assertEquals(200d, stock.totalAmount, 0.0001d);
    }

    @Test
    public void editingHomeSelectionReleasesReservationAndRaisesMissingAmount() {
        long recipeId = insertRecipe("Thịt kho test", "Thịt heo: 500 g");
        inventoryRepository.addPurchase("thit_heo", 700d, "g", "seed", null);
        String planId = preparationRepository.createPlanFromRecipe(recipeId, 2).id;
        CookingPlanIngredientEntity item = onlyItem(planId);

        preparationRepository.markHaveAtHome(planId, item.ingredientId, 200d, "g");
        preparationRepository.markHaveAtHome(planId, item.ingredientId, 100d, "g");

        CookingPlanIngredientEntity updated = onlyItem(planId);
        PantryStockEntity stock = database.pantryDao().getStock("thit_heo");
        assertEquals(100d, updated.reservedAmount, 0.0001d);
        assertEquals(400d, updated.missingAmount, 0.0001d);
        assertEquals(100d, stock.reservedAmount, 0.0001d);
    }

    @Test
    public void cancelPlanReleasesReservation() {
        long recipeId = insertRecipe("Thịt kho test", "Thịt heo: 500 g");
        inventoryRepository.addPurchase("thit_heo", 700d, "g", "seed", null);
        String planId = preparationRepository.createPlanFromRecipe(recipeId, 2).id;
        CookingPlanIngredientEntity item = onlyItem(planId);
        preparationRepository.markHaveAtHome(planId, item.ingredientId, 500d, "g");

        preparationRepository.cancelPlan(planId);

        PantryStockEntity stock = database.pantryDao().getStock("thit_heo");
        assertEquals(700d, stock.totalAmount, 0.0001d);
        assertEquals(0d, stock.reservedAmount, 0.0001d);
        assertFalse(preparationRepository.isPlanReadyToCook(planId));
    }

    @Test
    public void failedCompleteRollsBackConsumedBatchesAndPlanStatus() {
        long now = System.currentTimeMillis();
        inventoryRepository.addPurchase("thit_heo", 500d, "g", "seed", null);
        CookingPlanEntity plan = new CookingPlanEntity();
        plan.id = "rollback_plan";
        plan.recipeId = 99L;
        plan.status = CookingPlanEntity.STATUS_READY_TO_COOK;
        plan.targetServings = 2;
        plan.servingMultiplier = 1d;
        plan.createdAt = now;
        plan.updatedAt = now;
        database.cookingPlanDao().upsertPlan(plan);
        database.cookingPlanDao().upsertPlanIngredient(item(plan.id, "thit_heo", 500d, 0d, "g"));
        database.cookingPlanDao().upsertPlanIngredient(item(plan.id, "chicken", 500d, 0d, "g"));

        try {
            preparationRepository.completeCooking(plan.id);
            fail("Expected stock failure");
        } catch (IllegalStateException expected) {
            // Expected: second ingredient has no batch, transaction must roll back first deduction.
        }

        PantryStockEntity stock = database.pantryDao().getStock("thit_heo");
        assertEquals(500d, stock.totalAmount, 0.0001d);
        assertEquals(CookingPlanEntity.STATUS_READY_TO_COOK,
                database.cookingPlanDao().getPlan(plan.id).status);
    }

    private long insertRecipe(String name, String ingredients) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.name = name;
        recipe.description = "";
        recipe.totalMinutes = 10;
        recipe.difficulty = "Dễ";
        recipe.category = "Test";
        recipe.imageName = "";
        recipe.serving = "2 người";
        recipe.calories = "";
        recipe.cost = "";
        recipe.ingredients = ingredients;
        recipe.steps = "Nấu";
        return database.recipeDao().insert(recipe);
    }

    private CookingPlanIngredientEntity onlyItem(String planId) {
        List<CookingPlanIngredientEntity> items = preparationRepository.getPlanIngredients(planId);
        List<CookingPlanIngredientEntity> allItems = database.cookingPlanDao().getAllPlanIngredients();
        assertEquals("planId=" + planId + " allItems=" + describeItems(allItems), 1, items.size());
        return items.get(0);
    }

    private String describeItems(List<CookingPlanIngredientEntity> items) {
        StringBuilder builder = new StringBuilder();
        for (CookingPlanIngredientEntity item : items) {
            if (builder.length() > 0) {
                builder.append(";");
            }
            builder.append(item.planId).append("/").append(item.ingredientId)
                    .append("=").append(item.requiredAmount);
        }
        return builder.toString();
    }

    private CookingPlanIngredientEntity item(String planId, String ingredientId,
                                             double requiredAmount, double missingAmount,
                                             String baseUnit) {
        CookingPlanIngredientEntity item = new CookingPlanIngredientEntity();
        item.planId = planId;
        item.ingredientId = ingredientId;
        item.requiredAmount = requiredAmount;
        item.missingAmount = missingAmount;
        item.baseUnit = baseUnit;
        item.prepareStatus = CookingPlanIngredientEntity.STATUS_READY;
        item.updatedAt = System.currentTimeMillis();
        return item;
    }
}
