package com.sotaynauan.ai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.local.entity.RecipeIngredientEntity;

import java.util.List;

@Dao
public interface RecipeDao {
    @Query("SELECT COUNT(*) FROM recipes")
    int countRecipes();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<RecipeEntity> recipes);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(RecipeEntity recipe);

    @Update
    void update(RecipeEntity recipe);

    @Query("SELECT * FROM recipes WHERE name = :name LIMIT 1")
    RecipeEntity findByName(String name);

    @Query("SELECT * FROM recipes WHERE lower(trim(name)) = lower(trim(:name)) LIMIT 1")
    RecipeEntity findByNormalizedName(String name);

    @Query("SELECT * FROM recipes WHERE todaySuggestion = 1 ORDER BY popularityScore DESC")
    List<RecipeEntity> getTodaySuggestions();

    @Query("SELECT * FROM recipes ORDER BY popularityScore DESC LIMIT 5")
    List<RecipeEntity> getPopularRecipes();

    @Query("SELECT * FROM recipes ORDER BY popularityScore DESC")
    List<RecipeEntity> getAllRecipes();

    @Query("SELECT * FROM recipes WHERE category = 'Công thức của tôi' OR (todaySuggestion = 0 AND friendNote = '') ORDER BY id DESC")
    List<RecipeEntity> getUserSavedRecipes();

    @Query("SELECT * FROM recipes WHERE friendNote != '' ORDER BY popularityScore DESC")
    List<RecipeEntity> getFriendShares();

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    RecipeEntity findById(long recipeId);

    @Query("SELECT * FROM recipes WHERE totalMinutes <= 20 ORDER BY popularityScore DESC LIMIT 1")
    RecipeEntity findQuickSuggestion();

    @Query("SELECT * FROM recipes WHERE totalMinutes <= 30 ORDER BY RANDOM() LIMIT 1")
    RecipeEntity findRandomQuickSuggestion();

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder ASC, id ASC")
    List<RecipeIngredientEntity> getRecipeIngredients(long recipeId);

    @Query("SELECT * FROM recipe_ingredients ORDER BY recipeId ASC, sortOrder ASC, id ASC")
    List<RecipeIngredientEntity> getAllRecipeIngredients();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertRecipeIngredients(List<RecipeIngredientEntity> ingredients);

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    void deleteRecipeIngredients(long recipeId);
}
