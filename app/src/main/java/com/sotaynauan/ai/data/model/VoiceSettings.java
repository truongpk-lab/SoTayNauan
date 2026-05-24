package com.sotaynauan.ai.data.model;

public class VoiceSettings {
    public static final String PROFILE_FEMALE_SOUTH = "female_south";
    public static final String PROFILE_MALE_NORTH = "male_north";
    public static final String RESPONSE_FRIENDLY = "friendly";

    private final boolean voiceEnabled;
    private final boolean voiceControlEnabled;
    private final boolean autoReadEnabled;
    private final boolean timerAlertEnabled;
    private final String voiceProfile;
    private final int speechSpeedLevel;
    private final int volumePercent;
    private final String responseStyle;

    public VoiceSettings(boolean voiceEnabled,
                         boolean voiceControlEnabled,
                         boolean autoReadEnabled,
                         boolean timerAlertEnabled,
                         String voiceProfile,
                         int speechSpeedLevel,
                         int volumePercent,
                         String responseStyle) {
        this.voiceEnabled = voiceEnabled;
        this.voiceControlEnabled = voiceControlEnabled;
        this.autoReadEnabled = autoReadEnabled;
        this.timerAlertEnabled = timerAlertEnabled;
        this.voiceProfile = voiceProfile;
        this.speechSpeedLevel = Math.max(1, Math.min(5, speechSpeedLevel));
        this.volumePercent = Math.max(0, Math.min(100, volumePercent));
        this.responseStyle = responseStyle;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public boolean isVoiceControlEnabled() {
        return voiceControlEnabled;
    }

    public boolean isAutoReadEnabled() {
        return autoReadEnabled;
    }

    public boolean isTimerAlertEnabled() {
        return timerAlertEnabled;
    }

    public String getVoiceProfile() {
        return voiceProfile;
    }

    public int getSpeechSpeedLevel() {
        return speechSpeedLevel;
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public String getResponseStyle() {
        return responseStyle;
    }

    public float getSpeechRate() {
        return 0.72f + (speechSpeedLevel * 0.11f);
    }

    public float getVolumeScale() {
        return volumePercent / 100f;
    }

    public String getSpeedLabel() {
        if (speechSpeedLevel <= 2) {
            return "Chậm";
        }
        if (speechSpeedLevel == 3) {
            return "Bình thường";
        }
        return "Nhanh";
    }

    public String getVoiceProfileLabel() {
        if (PROFILE_MALE_NORTH.equals(voiceProfile)) {
            return "Nam (Miền Bắc)";
        }
        return "Nữ (Miền Nam)";
    }
}
