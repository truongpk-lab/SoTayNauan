package com.sotaynauan.ai.ui.voice;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.voice.VoiceProfileAdapter;
import com.sotaynauan.ai.data.local.datasource.VoiceLocalDataSource;
import com.sotaynauan.ai.data.model.VoiceSettings;
import com.sotaynauan.ai.data.repository.VoiceSettingsRepository;
import com.sotaynauan.ai.service.voice.VoiceSpeaker;

public class VoiceSettingsActivity extends Activity {
    private VoiceSettingsViewModel viewModel;
    private VoiceProfileAdapter voiceProfileAdapter;
    private VoiceSpeaker voiceSpeaker;
    private Switch voiceEnabledSwitch;
    private Switch voiceControlSwitch;
    private Switch autoReadSwitch;
    private Switch timerAlertSwitch;
    private LinearLayout voiceProfileContainer;
    private SeekBar speedSeekBar;
    private SeekBar volumeSeekBar;
    private TextView speedValueText;
    private TextView volumeValueText;
    private boolean binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_settings);

        VoiceLocalDataSource localDataSource = new VoiceLocalDataSource(this);
        viewModel = new VoiceSettingsViewModel(
                new VoiceSettingsRepository(localDataSource));
        voiceProfileAdapter = new VoiceProfileAdapter(this,
                profile -> bindSettings(viewModel.setVoiceProfile(profile)));
        voiceSpeaker = new VoiceSpeaker(this, localDataSource);

        bindViews();
        bindActions();
        bindSettings(viewModel.loadSettings());
    }

    @Override
    protected void onDestroy() {
        if (voiceSpeaker != null) {
            voiceSpeaker.shutdown();
        }
        super.onDestroy();
    }

    private void bindViews() {
        voiceEnabledSwitch = findViewById(R.id.voiceSettingsEnabledSwitch);
        voiceControlSwitch = findViewById(R.id.voiceSettingsControlSwitch);
        autoReadSwitch = findViewById(R.id.voiceSettingsAutoReadSwitch);
        timerAlertSwitch = findViewById(R.id.voiceSettingsTimerAlertSwitch);
        voiceProfileContainer = findViewById(R.id.voiceSettingsProfileContainer);
        speedSeekBar = findViewById(R.id.voiceSettingsSpeedSeekBar);
        volumeSeekBar = findViewById(R.id.voiceSettingsVolumeSeekBar);
        speedValueText = findViewById(R.id.voiceSettingsSpeedValue);
        volumeValueText = findViewById(R.id.voiceSettingsVolumeValue);
    }

    private void bindActions() {
        findViewById(R.id.voiceSettingsBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.voiceSettingsPreviewButton).setOnClickListener(view -> {
            VoiceSettings settings = viewModel.loadSettings();
            if (settings.isVoiceEnabled()) {
                voiceSpeaker.speak("Xin chào, tôi sẽ đọc từng bước nấu bằng giọng "
                        + settings.getVoiceProfileLabel().toLowerCase() + ".");
            }
        });
        voiceEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!binding) {
                bindSettings(viewModel.setVoiceEnabled(isChecked));
            }
        });
        voiceControlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!binding) {
                bindSettings(viewModel.setVoiceControlEnabled(isChecked));
            }
        });
        autoReadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!binding) {
                bindSettings(viewModel.setAutoReadEnabled(isChecked));
            }
        });
        timerAlertSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!binding) {
                bindSettings(viewModel.setTimerAlertEnabled(isChecked));
            }
        });
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    bindSettings(viewModel.setSpeechSpeedLevel(progress + 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    bindSettings(viewModel.setVolumePercent(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void bindSettings(VoiceSettings settings) {
        binding = true;
        voiceEnabledSwitch.setChecked(settings.isVoiceEnabled());
        voiceControlSwitch.setChecked(settings.isVoiceControlEnabled());
        autoReadSwitch.setChecked(settings.isAutoReadEnabled());
        timerAlertSwitch.setChecked(settings.isTimerAlertEnabled());
        speedSeekBar.setProgress(settings.getSpeechSpeedLevel() - 1);
        volumeSeekBar.setProgress(settings.getVolumePercent());
        binding = false;

        voiceProfileAdapter.bind(voiceProfileContainer, settings.getVoiceProfile());
        speedValueText.setText(settings.getSpeedLabel());
        volumeValueText.setText(settings.getVolumePercent() + "%");
        voiceControlSwitch.setEnabled(settings.isVoiceEnabled());
        autoReadSwitch.setEnabled(settings.isVoiceEnabled());
        timerAlertSwitch.setEnabled(settings.isVoiceEnabled());
        speedSeekBar.setEnabled(settings.isVoiceEnabled());
        volumeSeekBar.setEnabled(settings.isVoiceEnabled());
        voiceProfileContainer.setEnabled(settings.isVoiceEnabled());
        if (!settings.isVoiceEnabled()) {
            voiceSpeaker.stop();
        }
    }
}
