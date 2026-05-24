package com.sotaynauan.ai.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommunityState {
    private final List<CommunityFriend> friends;
    private final List<CommunityFriend> invites;
    private final List<CommunityFriend> discoveries;
    private final List<CommunityShare> shares;
    private final String statusMessage;

    public CommunityState(List<CommunityFriend> friends, List<CommunityFriend> invites,
                          List<CommunityFriend> discoveries, List<CommunityShare> shares,
                          String statusMessage) {
        this.friends = new ArrayList<>(friends);
        this.invites = new ArrayList<>(invites);
        this.discoveries = new ArrayList<>(discoveries);
        this.shares = new ArrayList<>(shares);
        this.statusMessage = statusMessage;
    }

    public List<CommunityFriend> getFriends() { return Collections.unmodifiableList(friends); }
    public List<CommunityFriend> getInvites() { return Collections.unmodifiableList(invites); }
    public List<CommunityFriend> getDiscoveries() { return Collections.unmodifiableList(discoveries); }
    public List<CommunityShare> getShares() { return Collections.unmodifiableList(shares); }
    public String getStatusMessage() { return statusMessage; }
}
