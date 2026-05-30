package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_shares")
public class CommunityShareEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public String friendId;
    public String friendName;
    public long recipeId;
    public String recipeName;
    public String message;
    public int likeCount;
    public int commentCount;
    public boolean liked;
    public boolean saved;
    public boolean fromMe;
    public long createdAtMillis;
}
