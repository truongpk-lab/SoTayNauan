package com.sotaynauan.ai.ui.voice;

import com.sotaynauan.ai.data.model.VoiceSettings;
import com.sotaynauan.ai.data.repository.VoiceSettingsRepository;

public class VoiceSettingsViewModel {
    private final VoiceSettingsRepository repository;

    public VoiceSettingsViewModel(VoiceSettingsRepository repository) {
        this.repository = repository;
    }

    public VoiceSettings loadSettings() {
        return repository.getSettings();
    }

    public VoiceSettings setVoiceEnabled(boolean enabled) {
        return repository.setVoiceEnabled(enabled);
    }

    public VoiceSettings setVoiceControlEnabled(boolean enabled) {
        return repository.setVoiceControlEnabled(enabled);
    }

    public VoiceSettings setAutoReadEnabled(boolean enabled) {
        return repository.setAutoReadEnabled(enabled);
    }

    public VoiceSettings setTimerAlertEnabled(boolean enabled) {
        return repository.setTimerAlertEnabled(enabled);
    }

    public VoiceSettings setVoiceProfile(String voiceProfile) {
        return repository.setVoiceProfile(voiceProfile);
    }

    public VoiceSettings setSpeechSpeedLevel(int speedLevel) {
        return repository.setSpeechSpeedLevel(speedLevel);
    }

    public VoiceSettings setVolumePercent(int volumePercent) {
        return repository.setVolumePercent(volumePercent);
    }
}
