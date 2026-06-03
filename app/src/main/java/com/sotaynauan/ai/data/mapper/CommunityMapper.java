package com.sotaynauan.ai.data.mapper;

import com.sotaynauan.ai.data.local.entity.CommunityCommentEntity;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.model.CommunityComment;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityShare;

import java.util.ArrayList;
import java.util.List;

public class CommunityMapper {
    public CommunityFriend toFriend(CommunityFriendEntity entity) {
        return new CommunityFriend(entity.id, entity.name, entity.email, entity.note,
                entity.status, entity.sharedRecipeCount);
    }

    public List<CommunityFriend> toFriends(List<CommunityFriendEntity> entities) {
        List<CommunityFriend> friends = new ArrayList<>();
        for (CommunityFriendEntity entity : entities) {
            friends.add(toFriend(entity));
        }
        return friends;
    }

    public CommunityShare toShare(CommunityShareEntity entity) {
        return toShare(entity, new ArrayList<>());
    }

    public CommunityShare toShare(CommunityShareEntity entity, List<CommunityComment> comments) {
        return new CommunityShare(entity.id, entity.friendId, entity.friendName, entity.recipeId,
                entity.recipeName, entity.message, entity.likeCount, entity.commentCount,
                entity.liked, entity.saved, entity.fromMe, comments);
    }

    public List<CommunityShare> toShares(List<CommunityShareEntity> entities) {
        List<CommunityShare> shares = new ArrayList<>();
        for (CommunityShareEntity entity : entities) {
            shares.add(toShare(entity));
        }
        return shares;
    }

    public CommunityComment toComment(CommunityCommentEntity entity) {
        return new CommunityComment(entity.id, entity.shareId, entity.authorName, entity.body,
                entity.createdAtMillis);
    }

    public List<CommunityComment> toComments(List<CommunityCommentEntity> entities) {
        List<CommunityComment> comments = new ArrayList<>();
        for (CommunityCommentEntity entity : entities) {
            comments.add(toComment(entity));
        }
        return comments;
    }
}
