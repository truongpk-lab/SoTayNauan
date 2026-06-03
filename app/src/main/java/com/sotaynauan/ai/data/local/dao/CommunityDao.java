package com.sotaynauan.ai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityCommentEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;

import java.util.List;

@Dao
public interface CommunityDao {
    @Query("SELECT COUNT(*) FROM community_friends")
    int countFriends();

    @Query("SELECT COUNT(*) FROM community_shares")
    int countShares();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertFriends(List<CommunityFriendEntity> friends);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertShares(List<CommunityShareEntity> shares);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertFriend(CommunityFriendEntity friend);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertShare(CommunityShareEntity share);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(CommunityCommentEntity comment);

    @Update
    void updateFriend(CommunityFriendEntity friend);

    @Update
    void updateShare(CommunityShareEntity share);

    @Query("SELECT * FROM community_friends WHERE status = :status ORDER BY sharedRecipeCount DESC, name ASC")
    List<CommunityFriendEntity> getFriendsByStatus(String status);

    @Query("SELECT * FROM community_friends WHERE id = :friendId LIMIT 1")
    CommunityFriendEntity findFriend(String friendId);

    @Query("SELECT * FROM community_friends WHERE email = :email LIMIT 1")
    CommunityFriendEntity findFriendByEmail(String email);

    @Query("SELECT * FROM community_friends WHERE status = 'friend' AND (name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%') ORDER BY sharedRecipeCount DESC, name ASC")
    List<CommunityFriendEntity> searchFriends(String query);

    @Query("SELECT * FROM community_shares ORDER BY createdAtMillis DESC")
    List<CommunityShareEntity> getShares();

    @Query("SELECT * FROM community_shares WHERE friendName LIKE '%' || :query || '%' OR recipeName LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' ORDER BY createdAtMillis DESC")
    List<CommunityShareEntity> searchShares(String query);

    @Query("SELECT * FROM community_shares WHERE id = :shareId LIMIT 1")
    CommunityShareEntity findShare(String shareId);

    @Query("SELECT * FROM community_comments WHERE shareId = :shareId ORDER BY createdAtMillis ASC")
    List<CommunityCommentEntity> getCommentsForShare(String shareId);
}
