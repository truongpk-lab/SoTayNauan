package com.sotaynauan.ai.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "community_comments",
        indices = {
                @Index("shareId"),
                @Index("createdAtMillis")
        }
)
public class CommunityCommentEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public String shareId;
    public String authorName;
    public String body;
    public long createdAtMillis;
}
