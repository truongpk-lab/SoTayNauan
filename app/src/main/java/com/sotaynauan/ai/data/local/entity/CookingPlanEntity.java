package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cooking_plans",
        indices = {
                @Index("recipeId"),
                @Index("status")
        })
public class CookingPlanEntity {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PREPARING = "PREPARING";
    public static final String STATUS_READY_TO_COOK = "READY_TO_COOK";
    public static final String STATUS_COOKING = "COOKING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @PrimaryKey
    @NonNull
    public String id = "";

    public long recipeId;
    public int targetServings;
    public double servingMultiplier;

    @NonNull
    public String status = STATUS_DRAFT;

    public long createdAt;
    public long updatedAt;
    public Long startedAt;
    public Long completedAt;
}
