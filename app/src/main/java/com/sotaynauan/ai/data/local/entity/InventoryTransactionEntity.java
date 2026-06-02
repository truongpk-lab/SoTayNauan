package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "inventory_transactions",
        indices = {
                @Index("ingredientId"),
                @Index("planId"),
                @Index("type"),
                @Index("createdAt")
        })
public class InventoryTransactionEntity {
    public static final String TYPE_ADD_PURCHASE = "ADD_PURCHASE";
    public static final String TYPE_RESERVE_FOR_PLAN = "RESERVE_FOR_PLAN";
    public static final String TYPE_RELEASE_RESERVATION = "RELEASE_RESERVATION";
    public static final String TYPE_CONSUME_COOKING = "CONSUME_COOKING";
    public static final String TYPE_ADJUST = "ADJUST";
    public static final String TYPE_EXPIRE = "EXPIRE";
    public static final String TYPE_WASTE = "WASTE";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String ingredientId = "";

    public String planId;
    public Long batchId;

    @NonNull
    public String type = "";

    public double deltaAmount;

    @NonNull
    public String baseUnit = "";

    public String note;
    public long createdAt;
}
