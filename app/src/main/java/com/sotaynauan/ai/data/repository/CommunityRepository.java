package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.CommunityLocalDataSource;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.mapper.CommunityMapper;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityState;

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

    public CommunityState addComment(String shareId) {
        CommunityShareEntity share = localDataSource.findShare(shareId);
        if (share != null) {
            share.commentCount += 1;
            localDataSource.updateShare(share);
            return loadCommunity("", "Đã thêm bình luận động viên vào " + share.recipeName + ".");
        }
        return loadCommunity("", "Không tìm thấy chia sẻ này.");
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
        CommunityFriendEntity friend = localDataSource.findFriend(friendId);
        if (friend == null) {
            return loadCommunity("", "Không tìm thấy bạn bè để chia sẻ.");
        }
        friend.sharedRecipeCount += 1;
        localDataSource.updateFriend(friend);

        CommunityShareEntity share = new CommunityShareEntity();
        share.id = "my-share-" + friendId + "-" + System.currentTimeMillis();
        share.friendId = friend.id;
        share.friendName = "Bạn → " + friend.name;
        share.recipeId = 0L;
        share.recipeName = "Mâm cơm bếp nhà";
        share.message = String.format(Locale.US,
                "Bạn vừa chia sẻ một gợi ý bữa cơm ấm áp cho %s.", friend.name);
        share.likeCount = 0;
        share.commentCount = 0;
        share.liked = false;
        share.saved = false;
        share.fromMe = true;
        share.createdAtMillis = System.currentTimeMillis();
        localDataSource.insertShare(share);
        return loadCommunity("", "Đã gửi chia sẻ local cho " + friend.name + ".");
    }
}
