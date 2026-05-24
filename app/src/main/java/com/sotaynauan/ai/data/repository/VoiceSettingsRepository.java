package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.datasource.VoiceLocalDataSource;
import com.sotaynauan.ai.data.model.VoiceSettings;

public class VoiceSettingsRepository {
    private final VoiceLocalDataSource localDataSource;

    public VoiceSettingsRepository(VoiceLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    public VoiceSettings getSettings() {
        return new VoiceSettings(
                localDataSource.isVoiceEnabled(),
                localDataSource.isVoiceControlEnabled(),
                localDataSource.isAutoReadEnabled(),
                localDataSource.isTimerAlertEnabled(),
                localDataSource.getVoiceProfile(),
                localDataSource.getSpeechSpeedLevel(),
                localDataSource.getVolumePercent(),
                localDataSource.getResponseStyle());
    }

    public VoiceSettings setVoiceEnabled(boolean enabled) {
        localDataSource.setVoiceEnabled(enabled);
        return getSettings();
    }

    public VoiceSettings setVoiceControlEnabled(boolean enabled) {
        localDataSource.setVoiceControlEnabled(enabled);
        return getSettings();
    }

    public VoiceSettings setAutoReadEnabled(boolean enabled) {
        localDataSource.setAutoReadEnabled(enabled);
        return getSettings();
    }

    public VoiceSettings setTimerAlertEnabled(boolean enabled) {
        localDataSource.setTimerAlertEnabled(enabled);
        return getSettings();
    }

    public VoiceSettings setVoiceProfile(String voiceProfile) {
        localDataSource.setVoiceProfile(voiceProfile);
        return getSettings();
    }

    public VoiceSettings setSpeechSpeedLevel(int speedLevel) {
        localDataSource.setSpeechSpeedLevel(speedLevel);
        return getSettings();
    }

    public VoiceSettings setVolumePercent(int volumePercent) {
        localDataSource.setVolumePercent(volumePercent);
        return getSettings();
    }

    public VoiceSettings setResponseStyle(String responseStyle) {
        localDataSource.setResponseStyle(responseStyle);
        return getSettings();
    }
}
