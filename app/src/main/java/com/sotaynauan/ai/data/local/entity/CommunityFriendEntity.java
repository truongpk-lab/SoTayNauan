package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_friends")
public class CommunityFriendEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public String name;
    public String email;
    public String note;
    public String status;
    public int sharedRecipeCount;
    public long joinedAtMillis;
}
