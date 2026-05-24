package com.sotaynauan.ai.data.local.datasource;

import com.sotaynauan.ai.data.local.dao.CommunityDao;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.seed.SeedDataProvider;

import java.util.List;

public class CommunityLocalDataSource {
    private final CommunityDao communityDao;
    private final SeedDataProvider seedDataProvider;

    public CommunityLocalDataSource(CommunityDao communityDao, SeedDataProvider seedDataProvider) {
        this.communityDao = communityDao;
        this.seedDataProvider = seedDataProvider;
    }

    public void seedIfNeeded() {
        if (communityDao.countFriends() == 0) {
            communityDao.insertFriends(seedDataProvider.createCommunityFriends());
        }
        if (communityDao.countShares() == 0) {
            communityDao.insertShares(seedDataProvider.createCommunityShares());
        }
    }

    public List<CommunityFriendEntity> getFriends() {
        seedIfNeeded();
        return communityDao.getFriendsByStatus(CommunityFriend.STATUS_FRIEND);
    }

    public List<CommunityFriendEntity> getInvites() {
        seedIfNeeded();
        return communityDao.getFriendsByStatus(CommunityFriend.STATUS_INVITED);
    }

    public List<CommunityFriendEntity> getDiscoveries() {
        seedIfNeeded();
        return communityDao.getFriendsByStatus(CommunityFriend.STATUS_DISCOVER);
    }

    public List<CommunityFriendEntity> searchFriends(String query) {
        seedIfNeeded();
        return communityDao.searchFriends(query);
    }

    public List<CommunityShareEntity> getShares() {
        seedIfNeeded();
        return communityDao.getShares();
    }

    public List<CommunityShareEntity> searchShares(String query) {
        seedIfNeeded();
        return communityDao.searchShares(query);
    }

    public CommunityFriendEntity findFriend(String friendId) {
        seedIfNeeded();
        return communityDao.findFriend(friendId);
    }

    public CommunityShareEntity findShare(String shareId) {
        seedIfNeeded();
        return communityDao.findShare(shareId);
    }

    public void updateFriend(CommunityFriendEntity friend) {
        communityDao.updateFriend(friend);
    }

    public void updateShare(CommunityShareEntity share) {
        communityDao.updateShare(share);
    }

    public void insertShare(CommunityShareEntity share) {
        communityDao.insertShare(share);
    }
}
