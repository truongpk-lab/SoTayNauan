package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pantry_stocks",
        foreignKeys = @ForeignKey(
                entity = IngredientEntity.class,
                parentColumns = "id",
                childColumns = "ingredientId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"ingredientId"}, unique = true)})
public class PantryStockEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String ingredientId = "";

    public double totalAmount;
    public double reservedAmount;

    @NonNull
    public String baseUnit = "";

    public long lastUpdatedAt;
}
