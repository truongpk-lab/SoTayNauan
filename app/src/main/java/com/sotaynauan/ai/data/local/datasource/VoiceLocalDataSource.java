package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;

public class VoiceLocalDataSource {
    private static final String PREFS_NAME = "voice_assistant_state";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_VOICE_CONTROL_ENABLED = "voice_control_enabled";
    private static final String KEY_AUTO_READ = "auto_read";
    private static final String KEY_TIMER_ALERT_ENABLED = "timer_alert_enabled";
    private static final String KEY_VOICE_PROFILE = "voice_profile";
    private static final String KEY_SPEECH_SPEED_LEVEL = "speech_speed_level";
    private static final String KEY_VOLUME_PERCENT = "volume_percent";
    private static final String KEY_RESPONSE_STYLE = "response_style";
    private static final String KEY_LAST_TRANSCRIPT = "last_transcript";
    private static final String KEY_LAST_RESPONSE = "last_response";
    private static final String KEY_UPDATED_AT = "updated_at";

    private final SharedPreferences preferences;

    public VoiceLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isVoiceEnabled() {
        return preferences.getBoolean(KEY_VOICE_ENABLED, true);
    }

    public void setVoiceEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_VOICE_ENABLED, enabled)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean isVoiceControlEnabled() {
        return preferences.getBoolean(KEY_VOICE_CONTROL_ENABLED, false);
    }

    public void setVoiceControlEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_VOICE_CONTROL_ENABLED, enabled)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean isAutoReadEnabled() {
        return preferences.getBoolean(KEY_AUTO_READ, true);
    }

    public void setAutoReadEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_AUTO_READ, enabled)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean isTimerAlertEnabled() {
        return preferences.getBoolean(KEY_TIMER_ALERT_ENABLED, true);
    }

    public void setTimerAlertEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_TIMER_ALERT_ENABLED, enabled)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public String getVoiceProfile() {
        return preferences.getString(KEY_VOICE_PROFILE, "female_south");
    }

    public void setVoiceProfile(String voiceProfile) {
        preferences.edit()
                .putString(KEY_VOICE_PROFILE, voiceProfile)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public int getSpeechSpeedLevel() {
        return preferences.getInt(KEY_SPEECH_SPEED_LEVEL, 3);
    }

    public void setSpeechSpeedLevel(int speedLevel) {
        preferences.edit()
                .putInt(KEY_SPEECH_SPEED_LEVEL, Math.max(1, Math.min(5, speedLevel)))
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public int getVolumePercent() {
        return preferences.getInt(KEY_VOLUME_PERCENT, 80);
    }

    public void setVolumePercent(int volumePercent) {
        preferences.edit()
                .putInt(KEY_VOLUME_PERCENT, Math.max(0, Math.min(100, volumePercent)))
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public String getResponseStyle() {
        return preferences.getString(KEY_RESPONSE_STYLE, "friendly");
    }

    public void setResponseStyle(String responseStyle) {
        preferences.edit()
                .putString(KEY_RESPONSE_STYLE, responseStyle)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public String getLastTranscript() {
        return preferences.getString(KEY_LAST_TRANSCRIPT,
                "Thêm 5 phút vào thời gian hầm xương");
    }

    public String getLastResponse() {
        return preferences.getString(KEY_LAST_RESPONSE,
                "Chạm micro để hỏi về bước nấu, timer hoặc yêu cầu đọc lại hướng dẫn.");
    }

    public void saveExchange(String transcript, String response) {
        preferences.edit()
                .putString(KEY_LAST_TRANSCRIPT, transcript)
                .putString(KEY_LAST_RESPONSE, response)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }
}
