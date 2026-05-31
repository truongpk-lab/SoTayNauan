package com.sotaynauan.ai.ui.community;

import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.CommunityRepository;

public class CommunityViewModel {
    private final CommunityRepository repository;
    private String query = "";
    private String status = "Cộng đồng local đã sẵn sàng. Like, bình luận, lưu món và chia sẻ đều được lưu trong Room.";

    public CommunityViewModel(CommunityRepository repository) {
        this.repository = repository;
    }

    public CommunityState load() {
        return repository.loadCommunity(query, status);
    }

    public CommunityState search(String query) {
        this.query = query == null ? "" : query;
        status = this.query.trim().isEmpty()
                ? "Đang xem toàn bộ cộng đồng bếp nhà."
                : "Đang lọc bạn bè và bài chia sẻ theo: " + this.query.trim();
        return load();
    }

    public CommunityState acceptInvite(String friendId) {
        query = "";
        CommunityState state = repository.acceptInvite(friendId);
        status = state.getStatusMessage();
        return state;
    }

    public CommunityState inviteDiscovery(String friendId) {
        query = "";
        CommunityState state = repository.inviteDiscovery(friendId);
        status = state.getStatusMessage();
        return state;
    }

    public CommunityState shareRecipe(String friendId) {
        return shareRecipe(friendId, null);
    }

    public CommunityState shareRecipe(String friendId, Recipe recipe) {
        query = "";
        CommunityState state = repository.shareRecipeWithFriend(friendId, recipe);
        status = state.getStatusMessage();
        return state;
    }

    public CommunityState like(String shareId) {
        CommunityState state = repository.toggleLike(shareId);
        status = state.getStatusMessage();
        return state;
    }

    public CommunityState comment(String shareId, String comment) {
        CommunityState state = repository.addComment(shareId, comment);
        status = state.getStatusMessage();
        return state;
    }

    public CommunityState save(String shareId) {
        CommunityState state = repository.toggleSave(shareId);
        status = state.getStatusMessage();
        return state;
    }
}
