package com.sotaynauan.ai.ui.profile;

import com.sotaynauan.ai.data.model.ProfileState;
import com.sotaynauan.ai.data.repository.ProfileRepository;

public class ProfileViewModel {
    private final ProfileRepository repository;

    public ProfileViewModel(ProfileRepository repository) {
        this.repository = repository;
    }

    public ProfileState loadProfile() {
        return repository.loadProfile("Hồ sơ cá nhân được tải từ dữ liệu local.");
    }

    public ProfileState updateDisplayName(String displayName) {
        return repository.updateDisplayName(displayName);
    }

    public ProfileState toggleNotifications() {
        return repository.toggleNotifications();
    }

    public ProfileState toggleCompactMode() {
        return repository.toggleCompactMode();
    }

    public void logout() {
        repository.logout();
    }
}

