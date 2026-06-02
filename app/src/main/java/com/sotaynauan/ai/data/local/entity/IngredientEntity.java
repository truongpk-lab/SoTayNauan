package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ingredients",
        indices = {
                @Index(value = {"normalizedName"}, unique = true),
                @Index(value = {"category"})
        })
public class IngredientEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String name = "";

    @NonNull
    public String normalizedName = "";

    @NonNull
    public String category = "";

    @NonNull
    public String baseUnit = "";

    public String aliasesJson;
    public String imageUrl;
    public boolean isActive;
    public long createdAt;
    public long updatedAt;
}
