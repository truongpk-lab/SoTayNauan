package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.CommunityLocalDataSource;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.mapper.CommunityMapper;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.Recipe;

import java.util.List;
import java.util.Locale;

public class CommunityRepository {
    private final CommunityLocalDataSource localDataSource;
    private final CommunityMapper mapper;

    public CommunityRepository(CommunityLocalDataSource localDataSource, CommunityMapper mapper) {
        this.localDataSource = localDataSource;
        this.mapper = mapper;
    }

    public CommunityState loadCommunity(String query, String statusMessage) {
        String normalized = query == null ? "" : query.trim();
        List<CommunityFriendEntity> friends = normalized.isEmpty()
                ? localDataSource.getFriends()
                : localDataSource.searchFriends(normalized);
        List<CommunityShareEntity> shares = normalized.isEmpty()
                ? localDataSource.getShares()
                : localDataSource.searchShares(normalized);
        return new CommunityState(
                mapper.toFriends(friends),
                mapper.toFriends(localDataSource.getInvites()),
                mapper.toFriends(localDataSource.getDiscoveries()),
                mapper.toShares(shares),
                statusMessage);
    }

    public CommunityState acceptInvite(String friendId) {
        CommunityFriendEntity friend = localDataSource.findFriend(friendId);
        if (friend != null) {
            friend.status = CommunityFriend.STATUS_FRIEND;
            friend.note = "Đã là thành viên bếp nhà của bạn.";
            friend.joinedAtMillis = System.currentTimeMillis();
            localDataSource.updateFriend(friend);
            return loadCommunity("", "Đã thêm " + friend.name + " vào danh sách bạn bè local.");
        }
        return loadCommunity("", "Không tìm thấy lời mời này trong dữ liệu local.");
    }

    public CommunityState inviteDiscovery(String friendId) {
        CommunityFriendEntity friend = localDataSource.findFriend(friendId);
        if (friend != null) {
            friend.status = CommunityFriend.STATUS_INVITED;
            friend.note = "Đã gửi lời mời kết bạn từ thiết bị này.";
            localDataSource.updateFriend(friend);
            return loadCommunity("", "Đã gửi lời mời cho " + friend.name + ".");
        }
        return loadCommunity("", "Không tìm thấy gợi ý bạn bè này.");
    }

    public CommunityState toggleLike(String shareId) {
        CommunityShareEntity share = localDataSource.findShare(shareId);
        if (share != null) {
            share.liked = !share.liked;
            share.likeCount = Math.max(0, share.likeCount + (share.liked ? 1 : -1));
            localDataSource.updateShare(share);
            return loadCommunity("", share.liked
                    ? "Đã thích chia sẻ " + share.recipeName + "."
                    : "Đã bỏ thích chia sẻ " + share.recipeName + ".");
        }
        return loadCommunity("", "Không tìm thấy chia sẻ này.");
    }

    public CommunityState addComment(String shareId, String comment) {
        CommunityShareEntity share = localDataSource.findShare(shareId);
        if (share != null) {
            share.commentCount += 1;
            localDataSource.updateShare(share);
            return loadCommunity("", "Đã thêm bình luận vào " + share.recipeName
                    + ": " + safeCommentPreview(comment));
        }
        return loadCommunity("", "Không tìm thấy chia sẻ này.");
    }

    private String safeCommentPreview(String comment) {
        String value = comment == null ? "" : comment.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            return "Bình luận mới";
        }
        return value.length() > 42 ? value.substring(0, 42) + "..." : value;
    }

    public CommunityState toggleSave(String shareId) {
        CommunityShareEntity share = localDataSource.findShare(shareId);
        if (share != null) {
            share.saved = !share.saved;
            localDataSource.updateShare(share);
            return loadCommunity("", share.saved
                    ? "Đã lưu " + share.recipeName + " vào sổ tay local."
                    : "Đã bỏ lưu " + share.recipeName + ".");
        }
        return loadCommunity("", "Không tìm thấy chia sẻ này.");
    }

    public CommunityState shareRecipeWithFriend(String friendId) {
        return shareRecipeWithFriend(friendId, null);
    }

    public CommunityState shareRecipeWithFriend(String friendId, Recipe recipe) {
        CommunityFriendEntity friend = localDataSource.findFriend(friendId);
        if (friend == null) {
            return loadCommunity("", "Không tìm thấy bạn bè để chia sẻ.");
        }
        String recipeName = recipe == null ? "Mâm cơm bếp nhà" : recipe.getName();
        friend.sharedRecipeCount += 1;
        localDataSource.updateFriend(friend);

        CommunityShareEntity share = new CommunityShareEntity();
        share.id = "my-share-" + friendId + "-" + System.currentTimeMillis();
        share.friendId = friend.id;
        share.friendName = "Bạn → " + friend.name;
        share.recipeId = recipe == null ? 0L : recipe.getId();
        share.recipeName = recipeName;
        share.message = String.format(Locale.US,
                "Bạn vừa chia sẻ công thức %s cho %s.", recipeName, friend.name);
        share.likeCount = 0;
        share.commentCount = 0;
        share.liked = false;
        share.saved = false;
        share.fromMe = true;
        share.createdAtMillis = System.currentTimeMillis();
        localDataSource.insertShare(share);
        return loadCommunity("", "Đã gửi công thức " + recipeName + " cho " + friend.name + ".");
    }

    public CommunityState shareRecipesWithFriend(String friendId, List<Recipe> recipes) {
        CommunityFriendEntity friend = localDataSource.findFriend(friendId);
        if (friend == null) {
            return loadCommunity("", "Không tìm thấy bạn bè để chia sẻ.");
        }
        if (recipes == null || recipes.isEmpty()) {
            return loadCommunity("", "Bạn chưa chọn món nào để chia sẻ.");
        }

        int sharedCount = 0;
        long now = System.currentTimeMillis();
        for (Recipe recipe : recipes) {
            if (recipe == null) {
                continue;
            }
            CommunityShareEntity share = new CommunityShareEntity();
            share.id = "my-share-" + friendId + "-" + now + "-" + sharedCount;
            share.friendId = friend.id;
            share.friendName = "Bạn → " + friend.name;
            share.recipeId = recipe.getId();
            share.recipeName = recipe.getName();
            share.message = String.format(Locale.US,
                    "Bạn vừa chia sẻ công thức %s cho %s.", recipe.getName(), friend.name);
            share.likeCount = 0;
            share.commentCount = 0;
            share.liked = false;
            share.saved = false;
            share.fromMe = true;
            share.createdAtMillis = now + sharedCount;
            localDataSource.insertShare(share);
            sharedCount += 1;
        }

        if (sharedCount == 0) {
            return loadCommunity("", "Bạn chưa chọn món nào để chia sẻ.");
        }
        friend.sharedRecipeCount += sharedCount;
        localDataSource.updateFriend(friend);
        String status = sharedCount == 1
                ? "Đã gửi 1 công thức cho " + friend.name + "."
                : "Đã gửi " + sharedCount + " công thức cho " + friend.name + ".";
        return loadCommunity("", status);
    }
}
