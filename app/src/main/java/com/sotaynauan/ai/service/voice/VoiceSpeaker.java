package com.sotaynauan.ai.service.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import com.sotaynauan.ai.data.local.datasource.VoiceLocalDataSource;
import com.sotaynauan.ai.data.model.VoiceSettings;

import java.util.Locale;

public class VoiceSpeaker implements TextToSpeech.OnInitListener {
    private final TextToSpeech textToSpeech;
    private final VoiceLocalDataSource localDataSource;
    private boolean ready;
    private String pendingText;

    public VoiceSpeaker(Context context) {
        this(context, new VoiceLocalDataSource(context));
    }

    public VoiceSpeaker(Context context, VoiceLocalDataSource localDataSource) {
        this.localDataSource = localDataSource;
        textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        ready = status == TextToSpeech.SUCCESS;
        if (ready) {
            applyLanguage();
            applySettings();
            if (pendingText != null) {
                speak(pendingText);
                pendingText = null;
            }
        }
    }

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        VoiceSettings settings = readSettings();
        if (!settings.isVoiceEnabled()) {
            stop();
            return;
        }
        if (!ready) {
            pendingText = text;
            return;
        }
        applyLanguage();
        applySettings();
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.getVolumeScale());
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "voice-assistant-reply");
    }

    public void stop() {
        if (ready) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        textToSpeech.stop();
        textToSpeech.shutdown();
    }

    private void applyLanguage() {
        int languageResult = textToSpeech.setLanguage(new Locale("vi", "VN"));
        if (languageResult == TextToSpeech.LANG_MISSING_DATA
                || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech.setLanguage(Locale.getDefault());
        }
    }

    private void applySettings() {
        VoiceSettings settings = readSettings();
        textToSpeech.setSpeechRate(settings.getSpeechRate());
        textToSpeech.setPitch(VoiceSettings.PROFILE_MALE_NORTH.equals(settings.getVoiceProfile())
                ? 0.86f
                : 1.04f);
    }

    private VoiceSettings readSettings() {
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
}
