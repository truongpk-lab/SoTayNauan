package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "shopping_items",
        indices = {
                @Index("planId"),
                @Index("ingredientId"),
                @Index("status")
        })
public class ShoppingItemEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public long recipeId;
    public String recipeName;
    public String name;
    public int amount;
    public String unit;
    public String note;
    public String category;
    public String status;
    public boolean committed;
    public long updatedAtMillis;

    public String planId;
    public String ingredientId;
    public String displayName;
    public double requiredAmount;
    public double boughtAmount;
    public String baseUnit;
    public long createdAt;
    public long updatedAt;
}
