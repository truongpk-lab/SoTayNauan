package com.sotaynauan.ai.ui.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.sotaynauan.ai.BuildConfig;
import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.voice.VoiceCommandAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.VoiceLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.VoiceAssistantState;
import com.sotaynauan.ai.data.remote.AiBackendRemoteDataSource;
import com.sotaynauan.ai.data.repository.CookingRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.data.repository.VoiceAssistantRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.service.voice.VoiceSpeaker;
import com.sotaynauan.ai.ui.cooking.CookingModeActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VoiceAssistantActivity extends Activity {
    private static final int REQUEST_RECORD_AUDIO = 1601;
    private static final long LISTENING_PULSE_MS = 140L;
    private static final long AMPLITUDE_POLL_MS = 90L;
    private static final long MIN_RECORDING_MS = 1200L;
    private static final long SILENCE_STOP_MS = 1700L;
    private static final long MAX_RECORDING_MS = 12000L;
    private static final long HANDS_FREE_RESTART_MS = 1700L;

    private final Handler waveHandler = new Handler(Looper.getMainLooper());
    private VoiceAssistantViewModel viewModel;
    private VoiceCommandAdapter commandAdapter;
    private VoiceSpeaker voiceSpeaker;
    private MediaRecorder mediaRecorder;
    private File recordingFile;
    private TextView statusTitle;
    private TextView statusSubtitle;
    private TextView transcriptText;
    private TextView responseText;
    private TextView contextText;
    private TextView micButton;
    private TextView waveOne;
    private TextView waveTwo;
    private TextView waveThree;
    private TextView waveFour;
    private TextView waveFive;
    private Switch autoReadSwitch;
    private LinearLayout quickCommands;
    private long activeRecipeId = -1L;
    private boolean bindingSwitch;
    private boolean listening;
    private boolean handsFreeMode;
    private boolean analyzingAudio;
    private int pulseFrame;
    private long recordingStartedAt;
    private long lastLoudAudioAt;
    private boolean heardAudio;

    private final Runnable listeningPulse = new Runnable() {
        @Override
        public void run() {
            if (!listening) {
                return;
            }
            pulseFrame = (pulseFrame + 1) % 6;
            int center = pulseFrame < 3 ? pulseFrame : 5 - pulseFrame;
            setWaveBars(12 + center * 4,
                    22 + center * 3,
                    34 + center * 4,
                    24 + center * 5,
                    14 + center * 4);
            waveHandler.postDelayed(this, LISTENING_PULSE_MS);
        }
    };

    private final Runnable amplitudePoll = new Runnable() {
        @Override
        public void run() {
            if (!listening || mediaRecorder == null) {
                return;
            }
            int amplitude = 0;
            try {
                amplitude = mediaRecorder.getMaxAmplitude();
            } catch (RuntimeException ignored) {
                // Recorder may be stopping; keep UI stable.
            }
            updateWaveFromAmplitude(amplitude);
            long now = System.currentTimeMillis();
            long duration = now - recordingStartedAt;
            if (amplitude > 900) {
                heardAudio = true;
                lastLoudAudioAt = now;
            }
            if (duration >= MAX_RECORDING_MS
                    || (heardAudio && duration >= MIN_RECORDING_MS && now - lastLoudAudioAt >= SILENCE_STOP_MS)) {
                stopRecordingAndAnalyze();
                return;
            }
            waveHandler.postDelayed(this, AMPLITUDE_POLL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        activeRecipeId = getIntent().getLongExtra(CookingModeActivity.EXTRA_RECIPE_ID, -1L);
        viewModel = new VoiceAssistantViewModel(createVoiceRepository());
        commandAdapter = new VoiceCommandAdapter(this,
                command -> {
                    runCommandWithBackend(command.getCommandText());
                });
        voiceSpeaker = new VoiceSpeaker(this);

        bindViews();
        bindActions();
        bindState(viewModel.loadState(activeRecipeId), false);
    }

    @Override
    protected void onDestroy() {
        waveHandler.removeCallbacks(listeningPulse);
        waveHandler.removeCallbacks(amplitudePoll);
        releaseRecorder();
        if (voiceSpeaker != null) {
            voiceSpeaker.shutdown();
        }
        super.onDestroy();
    }

    private VoiceAssistantRepository createVoiceRepository() {
        RecipeRepository recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        CookingRepository cookingRepository = new CookingRepository(
                new CookingLocalDataSource(this),
                recipeRepository,
                new ShoppingRepository(new ShoppingLocalDataSource(this)));
        return new VoiceAssistantRepository(
                new VoiceLocalDataSource(this),
                cookingRepository,
                new AiBackendRemoteDataSource(BuildConfig.AI_BACKEND_BASE_URL));
    }

    private void bindViews() {
        statusTitle = findViewById(R.id.voiceStatusTitle);
        statusSubtitle = findViewById(R.id.voiceStatusSubtitle);
        transcriptText = findViewById(R.id.voiceTranscriptText);
        responseText = findViewById(R.id.voiceResponseText);
        contextText = findViewById(R.id.voiceContextText);
        micButton = findViewById(R.id.voiceMicButton);
        autoReadSwitch = findViewById(R.id.voiceAutoReadSwitch);
        quickCommands = findViewById(R.id.voiceQuickCommands);
        waveOne = findViewById(R.id.voiceWaveOne);
        waveTwo = findViewById(R.id.voiceWaveTwo);
        waveThree = findViewById(R.id.voiceWaveThree);
        waveFour = findViewById(R.id.voiceWaveFour);
        waveFive = findViewById(R.id.voiceWaveFive);
    }

    private void bindActions() {
        findViewById(R.id.voiceCloseButton).setOnClickListener(view -> finish());
        findViewById(R.id.voiceSettingsButton).setOnClickListener(view ->
                startActivity(new Intent(this, VoiceSettingsActivity.class)));
        micButton.setOnClickListener(view -> startVoiceInput());
        autoReadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitch) {
                return;
            }
            bindState(viewModel.setAutoReadEnabled(isChecked, activeRecipeId), isChecked);
        });
    }

    private void bindState(VoiceAssistantState state, boolean speakResponse) {
        statusTitle.setText(state.getStatusTitle());
        statusSubtitle.setText(state.getStatusSubtitle());
        transcriptText.setText("\"" + state.getTranscript() + "\"");
        responseText.setText(state.getResponse());
        contextText.setText(state.getContextLabel());
        micButton.setText(state.isListening() ? "●" : "🎙");
        micButton.setAlpha(state.isListening() ? 0.88f : 1f);
        if (state.isListening()) {
            startWavePulse();
        } else {
            stopWavePulse();
        }
        bindingSwitch = true;
        autoReadSwitch.setChecked(state.isAutoReadEnabled());
        bindingSwitch = false;
        commandAdapter.bind(quickCommands, state.getQuickCommands());

        if (speakResponse && state.isAutoReadEnabled()) {
            voiceSpeaker.speak(state.getResponse());
        } else if (!state.isAutoReadEnabled()) {
            voiceSpeaker.stop();
        }
        if (handsFreeMode && !state.isListening() && !analyzingAudio) {
            scheduleHandsFreeRestart(speakResponse ? 2600L : HANDS_FREE_RESTART_MS);
        }
    }

    private void runCommandWithBackend(String command) {
        new Thread(() -> {
            VoiceAssistantState state = viewModel.handleCommandWithAiBackend(command, activeRecipeId);
            runOnUiThread(() -> bindState(state, true));
        }).start();
    }

    private void startVoiceInput() {
        if (listening) {
            stopRecordingAndAnalyze();
            return;
        }
        if (analyzingAudio) {
            handsFreeMode = false;
            statusSubtitle.setText("Đã tạm dừng nghe liên tục");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        voiceSpeaker.stop();
        handsFreeMode = true;
        startRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput();
        } else if (requestCode == REQUEST_RECORD_AUDIO) {
            bindState(viewModel.handleSpokenCommandWithAiBackend("", activeRecipeId), false);
        }
    }

    private void startRecording() {
        releaseRecorder();
        try {
            recordingFile = File.createTempFile("voice-command-", ".m4a", getCacheDir());
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            listening = true;
            analyzingAudio = false;
            heardAudio = false;
            recordingStartedAt = System.currentTimeMillis();
            lastLoudAudioAt = recordingStartedAt;
            bindState(viewModel.startListening(activeRecipeId), false);
            statusTitle.setText("Đang ghi âm");
            statusSubtitle.setText("Nói xong tôi tự xử lý và nghe tiếp");
            transcriptText.setText("\"Đang ghi âm audio...\"");
            responseText.setText("Bạn có thể rảnh tay. Tôi sẽ tự kết thúc khi bạn im lặng.");
            micButton.setText("■");
            startWavePulse();
            waveHandler.postDelayed(amplitudePoll, AMPLITUDE_POLL_MS);
        } catch (IOException | RuntimeException exception) {
            listening = false;
            releaseRecorder();
            bindRecordingError("Không mở được micro: " + exception.getMessage());
        }
    }

    private void stopRecordingAndAnalyze() {
        if (!listening) {
            return;
        }
        listening = false;
        analyzingAudio = true;
        waveHandler.removeCallbacks(amplitudePoll);
        stopWavePulse();
        File audioFile = recordingFile;
        if (!heardAudio) {
            releaseRecorder();
            analyzingAudio = false;
            statusTitle.setText("Đang nghe tiếp");
            statusSubtitle.setText("Chưa có tiếng nói rõ, tôi tiếp tục chờ");
            transcriptText.setText("\"Chưa nghe thấy câu lệnh\"");
            responseText.setText("Hãy nói gần micro hơn một chút.");
            if (handsFreeMode) {
                scheduleHandsFreeRestart(600L);
            }
            return;
        }
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (RuntimeException exception) {
            releaseRecorder();
            analyzingAudio = false;
            bindRecordingError("Audio quá ngắn hoặc micro chưa thu được tiếng. Bạn thử nói lâu hơn một chút.");
            return;
        }
        releaseRecorderOnly();
        statusTitle.setText("Đang gửi audio...");
        statusSubtitle.setText("Gemini đang nghe và chép lời tiếng Việt");
        transcriptText.setText(heardAudio ? "\"Đã thu được âm thanh\"" : "\"Âm thanh còn nhỏ\"");
        responseText.setText("Đang phân tích file ghi âm, vui lòng chờ một chút.");
        micButton.setText("…");
        runRecordedAudioWithBackend(audioFile);
    }

    private void runRecordedAudioWithBackend(File audioFile) {
        new Thread(() -> {
            byte[] audioBytes;
            try {
                audioBytes = readFileBytes(audioFile);
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    analyzingAudio = false;
                    bindRecordingError("Không đọc được file ghi âm: " + exception.getMessage());
                });
                return;
            } finally {
                if (audioFile != null) {
                    audioFile.delete();
                }
            }
            VoiceAssistantState state = viewModel.handleRecordedAudioWithAiBackend(audioBytes, activeRecipeId);
            runOnUiThread(() -> {
                analyzingAudio = false;
                bindState(state, true);
            });
        }).start();
    }

    private byte[] readFileBytes(File file) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, count);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private void releaseRecorder() {
        releaseRecorderOnly();
        if (recordingFile != null) {
            recordingFile.delete();
            recordingFile = null;
        }
    }

    private void releaseRecorderOnly() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (RuntimeException ignored) {
                // Recorder can already be released after stop failures.
            }
            mediaRecorder = null;
        }
    }

    private void setWaveHeight(TextView view, int dp) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.height = (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
        view.setLayoutParams(params);
    }

    private void setWaveBars(int one, int two, int three, int four, int five) {
        setWaveHeight(waveOne, one);
        setWaveHeight(waveTwo, two);
        setWaveHeight(waveThree, three);
        setWaveHeight(waveFour, four);
        setWaveHeight(waveFive, five);
    }

    private void startWavePulse() {
        waveHandler.removeCallbacks(listeningPulse);
        setWaveBars(12, 24, 36, 26, 14);
        waveHandler.postDelayed(listeningPulse, LISTENING_PULSE_MS);
    }

    private void stopWavePulse() {
        waveHandler.removeCallbacks(listeningPulse);
        setWaveBars(10, 22, 14, 24, 12);
    }

    private void updateWaveFromAmplitude(int amplitude) {
        if (!listening) {
            return;
        }
        float level = Math.max(0f, Math.min(1f, amplitude / 12000f));
        if (level < 0.04f) {
            return;
        }
        int boost = Math.round(level * 34f);
        setWaveBars(
                10 + Math.round(boost * 0.42f),
                16 + Math.round(boost * 0.78f),
                22 + boost,
                18 + Math.round(boost * 0.86f),
                12 + Math.round(boost * 0.58f));
        if (amplitude > 900) {
            statusTitle.setText("Đang ghi âm");
            statusSubtitle.setText("Âm thanh đã vào micro");
            transcriptText.setText("\"Đang thu tiếng Việt...\"");
        }
    }

    private void bindRecordingError(String message) {
        analyzingAudio = false;
        VoiceAssistantState state = viewModel.loadState(activeRecipeId);
        statusTitle.setText("Chưa ghi được");
        statusSubtitle.setText("Bấm mic để thử lại");
        transcriptText.setText("\"Chưa có audio hợp lệ\"");
        responseText.setText(message);
        contextText.setText(state.getContextLabel());
        micButton.setText("🎙");
        micButton.setAlpha(1f);
        commandAdapter.bind(quickCommands, state.getQuickCommands());
        if (handsFreeMode) {
            scheduleHandsFreeRestart(HANDS_FREE_RESTART_MS);
        }
    }

    private void scheduleHandsFreeRestart(long delayMs) {
        waveHandler.removeCallbacks(amplitudePoll);
        waveHandler.postDelayed(() -> {
            if (handsFreeMode && !listening && !analyzingAudio && !isFinishing()) {
                startRecording();
            }
        }, delayMs);
    }

}
