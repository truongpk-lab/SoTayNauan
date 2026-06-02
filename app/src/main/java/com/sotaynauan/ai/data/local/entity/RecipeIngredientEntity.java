package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recipe_ingredients",
        foreignKeys = {
                @ForeignKey(
                        entity = RecipeEntity.class,
                        parentColumns = "id",
                        childColumns = "recipeId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(
                        entity = IngredientEntity.class,
                        parentColumns = "id",
                        childColumns = "ingredientId",
                        onDelete = ForeignKey.RESTRICT)
        },
        indices = {
                @Index("recipeId"),
                @Index("ingredientId")
        })
public class RecipeIngredientEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long recipeId;

    @NonNull
    public String ingredientId = "";

    public double amount;

    @NonNull
    public String unit = "";

    public boolean isOptional;
    public String note;
    public int sortOrder;
}
