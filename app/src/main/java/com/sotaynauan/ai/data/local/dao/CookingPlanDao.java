package com.sotaynauan.ai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;

import java.util.List;

@Dao
public interface CookingPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertPlan(CookingPlanEntity plan);

    @Update
    void updatePlan(CookingPlanEntity plan);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertPlanIngredient(CookingPlanIngredientEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertPlanIngredients(List<CookingPlanIngredientEntity> items);

    @Update
    void updatePlanIngredient(CookingPlanIngredientEntity item);

    @Query("SELECT * FROM cooking_plans WHERE id = :planId LIMIT 1")
    CookingPlanEntity getPlan(String planId);

    @Query("SELECT * FROM cooking_plans WHERE recipeId = :recipeId AND status IN ('DRAFT', 'PREPARING', 'READY_TO_COOK', 'COOKING') ORDER BY updatedAt DESC LIMIT 1")
    CookingPlanEntity getOpenPlanForRecipe(long recipeId);

    @Query("SELECT * FROM cooking_plans WHERE status IN ('READY_TO_COOK', 'COOKING') ORDER BY updatedAt DESC LIMIT 1")
    CookingPlanEntity getLatestCookablePlan();

    @Query("SELECT * FROM cooking_plan_ingredients WHERE planId = :planId ORDER BY id ASC")
    List<CookingPlanIngredientEntity> getPlanIngredients(String planId);

    @Query("SELECT * FROM cooking_plan_ingredients ORDER BY id ASC")
    List<CookingPlanIngredientEntity> getAllPlanIngredients();

    @Query("SELECT * FROM cooking_plan_ingredients WHERE planId = :planId AND ingredientId = :ingredientId LIMIT 1")
    CookingPlanIngredientEntity getPlanIngredient(String planId, String ingredientId);

    @Query("DELETE FROM cooking_plan_ingredients WHERE planId = :planId")
    void deletePlanIngredients(String planId);
}
