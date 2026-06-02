package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "shared_cooking_plans",
        indices = {
                @Index("planId"),
                @Index("friendId"),
                @Index("status")
        })
public class SharedCookingPlanEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String planId = "";

    @NonNull
    public String friendId = "";

    public String permission;
    public String status;
    public long createdAt;
    public long updatedAt;
}
