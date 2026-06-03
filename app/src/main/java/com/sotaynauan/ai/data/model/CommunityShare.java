package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommunityShare {
    private final String id;
    private final String friendId;
    private final String friendName;
    private final long recipeId;
    private final String recipeName;
    private final String message;
    private final int likeCount;
    private final int commentCount;
    private final boolean liked;
    private final boolean saved;
    private final boolean fromMe;
    private final List<CommunityComment> comments;

    public CommunityShare(String id, String friendId, String friendName, long recipeId,
                          String recipeName, String message, int likeCount, int commentCount,
                          boolean liked, boolean saved, boolean fromMe) {
        this(id, friendId, friendName, recipeId, recipeName, message, likeCount, commentCount,
                liked, saved, fromMe, new ArrayList<>());
    }

    public CommunityShare(String id, String friendId, String friendName, long recipeId,
                          String recipeName, String message, int likeCount, int commentCount,
                          boolean liked, boolean saved, boolean fromMe,
                          List<CommunityComment> comments) {
        this.id = id;
        this.friendId = friendId;
        this.friendName = friendName;
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.message = message;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.liked = liked;
        this.saved = saved;
        this.fromMe = fromMe;
        this.comments = new ArrayList<>(comments);
    }

    public String getId() { return id; }
    public String getFriendId() { return friendId; }
    public String getFriendName() { return friendName; }
    public long getRecipeId() { return recipeId; }
    public String getRecipeName() { return recipeName; }
    public String getMessage() { return message; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isLiked() { return liked; }
    public boolean isSaved() { return saved; }
    public boolean isFromMe() { return fromMe; }
    public List<CommunityComment> getComments() { return Collections.unmodifiableList(comments); }
}
