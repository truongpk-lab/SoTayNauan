package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pantry_batches",
        foreignKeys = @ForeignKey(
                entity = IngredientEntity.class,
                parentColumns = "id",
                childColumns = "ingredientId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("ingredientId"),
                @Index("expiryAt")
        })
public class PantryBatchEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String ingredientId = "";

    public double remainingAmount;

    @NonNull
    public String baseUnit = "";

    public String source;
    public long boughtAt;
    public Long expiryAt;
    public long createdAt;
    public long updatedAt;
}
