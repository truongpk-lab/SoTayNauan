package com.sotaynauan.ai.data.repository;

import android.net.Uri;

import com.sotaynauan.ai.data.local.datasource.CommunityLocalDataSource;
import com.sotaynauan.ai.data.local.entity.CommunityCommentEntity;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.mapper.CommunityMapper;
import com.sotaynauan.ai.data.model.CommunityComment;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityShare;
import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.remote.CommunityRemoteDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CommunityRepository {
    private final CommunityLocalDataSource localDataSource;
    private final CommunityMapper mapper;
    private final String currentUserId;
    private final String currentDisplayName;
    private final String currentEmail;
    private final CommunityRemoteDataSource remoteDataSource;

    public CommunityRepository(CommunityLocalDataSource localDataSource, CommunityMapper mapper) {
        this(localDataSource, mapper, "guest-local", "Khach bep nha", "guest-local@local", null);
    }

    public CommunityRepository(CommunityLocalDataSource localDataSource, CommunityMapper mapper,
                               String currentUserId, String currentDisplayName,
                               String currentEmail) {
        this(localDataSource, mapper, currentUserId, currentDisplayName, currentEmail, null);
    }

    public CommunityRepository(CommunityLocalDataSource localDataSource, CommunityMapper mapper,
                               String currentUserId, String currentDisplayName,
                               String currentEmail,
                               CommunityRemoteDataSource remoteDataSource) {
        this.localDataSource = localDataSource;
        this.mapper = mapper;
        this.currentUserId = cleanOrFallback(currentUserId, "guest-local");
        this.currentDisplayName = cleanOrFallback(currentDisplayName, "Khach bep nha");
        this.currentEmail = cleanOrFallback(currentEmail, this.currentUserId + "@local");
        this.remoteDataSource = remoteDataSource;
    }

    public CommunityState loadCommunity(String query, String statusMessage) {
        CommunityState remoteState = tryRemoteLoad(query);
        if (remoteState != null) {
            return remoteState;
        }
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
                mapper.toFriends(localDataSource.getSentInvites()),
                mapper.toFriends(localDataSource.getDiscoveries()),
                mapSharesWithComments(shares),
                statusMessage,
                buildCurrentUserQrPayload());
    }

    public CommunityState acceptInvite(String friendId) {
        CommunityState remoteState = tryRemoteAcceptInvite(friendId);
        if (remoteState != null) {
            return remoteState;
        }
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
        CommunityFriendEntity localFriend = localDataSource.findFriend(friendId);
        if (localFriend != null) {
            CommunityState remoteState = tryRemoteInvite(localFriend.id, localFriend.name, localFriend.email);
            if (remoteState != null) {
                return remoteState;
            }
        }
        CommunityFriendEntity friend = localDataSource.findFriend(friendId);
        if (friend != null) {
            friend.status = CommunityFriend.STATUS_INVITE_SENT;
            friend.note = "Đã gửi lời mời kết bạn từ thiết bị này.";
            localDataSource.updateFriend(friend);
            return loadCommunity("", "Đã gửi lời mời cho " + friend.name + ".");
        }
        return loadCommunity("", "Không tìm thấy gợi ý bạn bè này.");
    }

    public CommunityState inviteByQrPayload(String payload) {
        QrInvite invite = parseQrInvite(payload);
        if (invite == null) {
            return loadCommunity("", "Mã QR này không phải lời mời của Sổ Tay Nấu Ăn.");
        }
        if (currentUserId.equals(invite.userId)) {
            return loadCommunity("", "Đây là mã QR của chính tài khoản hiện tại.");
        }
        CommunityState remoteState = tryRemoteInvite(invite.userId, invite.name, invite.email);
        if (remoteState != null) {
            return remoteState;
        }

        CommunityFriendEntity friend = localDataSource.findFriend(invite.userId);
        if (friend == null && !invite.email.isEmpty()) {
            friend = localDataSource.findFriendByEmail(invite.email);
        }
        if (friend == null) {
            friend = new CommunityFriendEntity();
            friend.id = invite.userId;
            friend.name = invite.name;
            friend.email = invite.email;
            friend.note = "Đã gửi lời mời qua mã QR.";
            friend.status = CommunityFriend.STATUS_INVITE_SENT;
            friend.sharedRecipeCount = 0;
            friend.joinedAtMillis = System.currentTimeMillis();
            localDataSource.insertFriend(friend);
            return loadCommunity("", "Đã gửi lời mời kết bạn cho " + friend.name + " qua mã QR.");
        }

        if (CommunityFriend.STATUS_FRIEND.equals(friend.status)) {
            return loadCommunity("", friend.name + " đã có trong danh sách bạn bè.");
        }
        if (CommunityFriend.STATUS_INVITE_SENT.equals(friend.status)) {
            return loadCommunity("", "Bạn đã gửi lời mời cho " + friend.name + " rồi.");
        }
        if (CommunityFriend.STATUS_INVITE_RECEIVED.equals(friend.status)) {
            return loadCommunity("", friend.name + " đã gửi lời mời cho bạn. Bạn có thể bấm Nhận lời.");
        }

        friend.name = invite.name;
        friend.email = invite.email;
        friend.note = "Đã gửi lời mời qua mã QR.";
        friend.status = CommunityFriend.STATUS_INVITE_SENT;
        localDataSource.updateFriend(friend);
        return loadCommunity("", "Đã gửi lời mời kết bạn cho " + friend.name + " qua mã QR.");
    }

    public CommunityState toggleLike(String shareId) {
        CommunityState remoteState = tryRemoteToggleLike(shareId);
        if (remoteState != null) {
            return remoteState;
        }
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
        CommunityState remoteState = tryRemoteComment(shareId, comment);
        if (remoteState != null) {
            return remoteState;
        }
        CommunityShareEntity share = localDataSource.findShare(shareId);
        if (share != null) {
            CommunityCommentEntity entity = new CommunityCommentEntity();
            entity.id = "comment-" + shareId + "-" + System.currentTimeMillis();
            entity.shareId = shareId;
            entity.authorName = currentDisplayName;
            entity.body = comment == null ? "" : comment.trim();
            entity.createdAtMillis = System.currentTimeMillis();
            localDataSource.insertComment(entity);
            share.commentCount = localDataSource.getCommentsForShare(shareId).size();
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
        CommunityState remoteState = tryRemoteToggleSave(shareId);
        if (remoteState != null) {
            return remoteState;
        }
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
        CommunityState remoteState = tryRemoteShareRecipe(friendId, recipe);
        if (remoteState != null) {
            return remoteState;
        }
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
        if (remoteReady() && recipes != null && !recipes.isEmpty()) {
            CommunityState lastState = null;
            boolean sharedAny = false;
            for (Recipe recipe : recipes) {
                CommunityState state = tryRemoteShareRecipe(friendId, recipe);
                if (state != null) {
                    lastState = state;
                    sharedAny = true;
                }
            }
            if (sharedAny && lastState != null) {
                return lastState;
            }
        }
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

    private List<CommunityShare> mapSharesWithComments(List<CommunityShareEntity> entities) {
        List<CommunityShare> shares = new ArrayList<>();
        for (CommunityShareEntity entity : entities) {
            List<CommunityComment> comments =
                    mapper.toComments(localDataSource.getCommentsForShare(entity.id));
            shares.add(mapper.toShare(entity, comments));
        }
        return shares;
    }

    private String buildCurrentUserQrPayload() {
        Uri.Builder builder = Uri.parse("sotaynauan://community/invite").buildUpon()
                .appendQueryParameter("userId", currentUserId)
                .appendQueryParameter("name", currentDisplayName)
                .appendQueryParameter("email", currentEmail);
        if (remoteReady()) {
            builder.appendQueryParameter("serverUrl", remoteDataSource.getBaseUrl());
        }
        return builder.build().toString();
    }

    private QrInvite parseQrInvite(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            Uri uri = Uri.parse(payload.trim());
            if (!"sotaynauan".equals(uri.getScheme())
                    || !"community".equals(uri.getHost())
                    || !"/invite".equals(uri.getPath())) {
                return null;
            }
            String userId = cleanOrFallback(uri.getQueryParameter("userId"), "");
            String name = cleanOrFallback(uri.getQueryParameter("name"), "Bạn bếp nhà");
            String email = cleanOrFallback(uri.getQueryParameter("email"), "");
            String serverUrl = cleanOrFallback(uri.getQueryParameter("serverUrl"), "");
            if (userId.isEmpty()) {
                userId = "qr-" + UUID.nameUUIDFromBytes(payload.getBytes()).toString();
            }
            return new QrInvite(userId, name, email, serverUrl);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean remoteReady() {
        return remoteDataSource != null && remoteDataSource.isConfigured();
    }

    private CommunityState tryRemoteLoad(String query) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.loadCommunity(query, buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private CommunityState tryRemoteInvite(String targetUserId, String targetName, String targetEmail) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.sendInvite(targetUserId, targetName, targetEmail,
                    buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private CommunityState tryRemoteAcceptInvite(String friendId) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.acceptInvite(friendId, buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private CommunityState tryRemoteShareRecipe(String friendId, Recipe recipe) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.shareRecipe(friendId, recipe, buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private CommunityState tryRemoteToggleLike(String shareId) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.toggleLike(shareId, buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private CommunityState tryRemoteToggleSave(String shareId) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.toggleSave(shareId, buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private CommunityState tryRemoteComment(String shareId, String comment) {
        if (!remoteReady()) {
            return null;
        }
        try {
            return remoteDataSource.addComment(shareId, comment, buildCurrentUserQrPayload());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String cleanOrFallback(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }

    private static class QrInvite {
        final String userId;
        final String name;
        final String email;
        final String serverUrl;

        QrInvite(String userId, String name, String email, String serverUrl) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.serverUrl = serverUrl;
        }
    }
}
