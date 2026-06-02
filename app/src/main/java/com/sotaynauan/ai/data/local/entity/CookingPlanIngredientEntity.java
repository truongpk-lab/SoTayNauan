package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cooking_plan_ingredients",
        foreignKeys = @ForeignKey(
                entity = CookingPlanEntity.class,
                parentColumns = "id",
                childColumns = "planId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("planId"),
                @Index("ingredientId"),
                @Index(value = {"planId", "ingredientId"}, unique = true)
        })
public class CookingPlanIngredientEntity {
    public static final String STATUS_NEED_CHECK = "NEED_CHECK";
    public static final String STATUS_HAVE_AT_HOME = "HAVE_AT_HOME";
    public static final String STATUS_NEED_BUY = "NEED_BUY";
    public static final String STATUS_BOUGHT = "BOUGHT";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_CONSUMED = "CONSUMED";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String planId = "";

    @NonNull
    public String ingredientId = "";

    public double requiredAmount;
    public double homeSelectedAmount;
    public double reservedAmount;
    public double purchasedAmount;
    public double consumedAmount;
    public double missingAmount;

    @NonNull
    public String baseUnit = "";

    @NonNull
    public String prepareStatus = STATUS_NEED_CHECK;

    public String userNote;
    public long updatedAt;
}
