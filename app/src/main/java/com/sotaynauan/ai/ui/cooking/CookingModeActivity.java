package com.sotaynauan.ai.ui.cooking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.cooking.CookingStepProgressAdapter;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.VoiceLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CookingSessionState;
import com.sotaynauan.ai.data.model.CookingTimerState;
import com.sotaynauan.ai.data.model.Recipe;
import com.sotaynauan.ai.data.repository.CookingRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;
import com.sotaynauan.ai.service.voice.VoiceSpeaker;
import com.sotaynauan.ai.ui.voice.VoiceAssistantActivity;
import com.sotaynauan.ai.util.RecipeImageResolver;

import java.util.Locale;

public class CookingModeActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private CookingModeViewModel viewModel;
    private CookingStepProgressAdapter stepProgressAdapter;
    private CookingTimerRingView timerRing;
    private TextView titleText;
    private TextView currentStepText;
    private TextView stepBadge;
    private TextView timerText;
    private TextView statusText;
    private TextView playPauseButton;
    private Button completeStepButton;
    private LinearLayout stepsContainer;
    private FrameLayout photoFrame;
    private ImageView stepPhoto;
    private int stepTotalSeconds;
    private int remainingSeconds;
    private boolean timerRunning;
    private long activeRecipeId = -1L;
    private CookingSessionState currentState;
    private VoiceLocalDataSource voiceLocalDataSource;
    private VoiceSpeaker voiceSpeaker;
    private String lastAutoSpokenKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_mode);

        viewModel = new CookingModeViewModel(createCookingRepository());
        voiceLocalDataSource = new VoiceLocalDataSource(this);
        voiceSpeaker = new VoiceSpeaker(this, voiceLocalDataSource);
        stepProgressAdapter = new CookingStepProgressAdapter(this);
        bindViews();
        bindActions();

        activeRecipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        bindSession(viewModel.loadSession(activeRecipeId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTimerFromRepository();
    }

    @Override
    protected void onDestroy() {
        timerRunning = false;
        if (voiceSpeaker != null) {
            voiceSpeaker.shutdown();
        }
        super.onDestroy();
    }

    private CookingRepository createCookingRepository() {
        RecipeRepository recipeRepository = new RecipeRepository(
                new RecipeLocalDataSource(
                        AppDatabase.getInstance(this).recipeDao(),
                        new SeedDataProvider()),
                new RecipeMapper());
        return new CookingRepository(new CookingLocalDataSource(this), recipeRepository,
                new ShoppingRepository(new ShoppingLocalDataSource(this)));
    }

    private void bindViews() {
        timerRing = findViewById(R.id.cookingTimerRing);
        titleText = findViewById(R.id.cookingRecipeTitle);
        currentStepText = findViewById(R.id.cookingCurrentStep);
        stepBadge = findViewById(R.id.cookingStepBadge);
        timerText = findViewById(R.id.cookingTimerText);
        statusText = findViewById(R.id.cookingStatus);
        playPauseButton = findViewById(R.id.cookingPlayPauseButton);
        completeStepButton = findViewById(R.id.cookingCompleteStepButton);
        stepsContainer = findViewById(R.id.cookingStepsContainer);
        photoFrame = findViewById(R.id.cookingPhotoFrame);
        stepPhoto = findViewById(R.id.cookingStepPhoto);
    }

    private void bindActions() {
        findViewById(R.id.cookingBackButton).setOnClickListener(view -> finish());
        View.OnClickListener replayListener = view -> {
            bindSessionKeepTimer(viewModel.replayCurrentInstruction());
            speakCurrentInstruction(true);
            timerRunning = false;
            playPauseButton.setText("▶");
        };
        findViewById(R.id.cookingReplayButton).setOnClickListener(replayListener);
        findViewById(R.id.cookingReplayTopButton).setOnClickListener(replayListener);
        findViewById(R.id.cookingAskAiButton).setOnClickListener(view -> openVoiceAssistant());
        findViewById(R.id.cookingAskTopButton).setOnClickListener(view -> openVoiceAssistant());
        playPauseButton.setOnClickListener(view -> openTimer());
        completeStepButton.setOnClickListener(view -> bindSession(viewModel.completeCurrentStep()));
    }

    private void bindSession(CookingSessionState state) {
        currentState = state;
        timerRunning = false;
        stepTotalSeconds = Math.max(0, state.getCurrentStepSeconds());
        remainingSeconds = stepTotalSeconds;
        playPauseButton.setText("▶");
        bindSessionKeepTimer(state);
    }

    private void bindSessionKeepTimer(CookingSessionState state) {
        currentState = state;
        statusText.setText(state.getStatusMessage());
        if (!state.hasRecipe()) {
            titleText.setText("Chế độ nấu");
            currentStepText.setText("Chưa có công thức đang nấu.");
            stepBadge.setText("Bước 0 / 0");
            completeStepButton.setEnabled(false);
            playPauseButton.setEnabled(false);
            stepPhoto.setImageResource(R.drawable.cooking_step_preview);
            updateTimerViews();
            return;
        }

        Recipe recipe = state.getRecipe();
        stepPhoto.setImageResource(RecipeImageResolver.resolve(this, recipe));
        stepPhoto.setContentDescription(recipe.getName());
        titleText.setText(state.isCompleted() ? "Hoàn tất món ăn" : createStepTitle(state.getCurrentStepText()));
        currentStepText.setText(state.isCompleted()
                ? "Món " + recipe.getName() + " đã sẵn sàng. Bạn có thể quay lại công thức hoặc hỏi AI để biến tấu món tiếp theo."
                : state.getCurrentStepText());
        stepBadge.setText("•  Bước " + (state.getCurrentStepIndex() + 1) + " / " + Math.max(1, state.getStepCount()));
        stepProgressAdapter.bind(stepsContainer, recipe.getSteps(), state.getCurrentStepIndex(), state.isCompleted());
        completeStepButton.setText(state.isCompleted() ? "Đã hoàn thành món" : "Hoàn thành bước  →");
        completeStepButton.setEnabled(!state.isCompleted() && !recipe.getSteps().isEmpty());
        playPauseButton.setEnabled(!state.isCompleted() && stepTotalSeconds > 0);
        photoFrame.setAlpha(state.isCompleted() ? 0.72f : 1f);
        updateTimerViews();
        speakCurrentInstruction(false);
    }

    private void updateTimerViews() {
        int safeRemaining = Math.max(0, remainingSeconds);
        int minutes = safeRemaining / 60;
        int seconds = safeRemaining % 60;
        timerText.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        float progress = stepTotalSeconds <= 0 ? 0f : safeRemaining / (float) stepTotalSeconds;
        timerRing.setProgress(progress);
    }

    private void refreshTimerFromRepository() {
        if (viewModel == null) {
            return;
        }
        CookingTimerState timerState = viewModel.getTimerState();
        if (timerState.getTotalSeconds() <= 0 || currentState == null
                || !currentState.hasRecipe()
                || timerState.getRecipeId() != currentState.getRecipe().getId()
                || timerState.getStepIndex() != currentState.getCurrentStepIndex()) {
            return;
        }
        timerRunning = timerState.isRunning();
        stepTotalSeconds = timerState.getTotalSeconds();
        remainingSeconds = timerState.getRemainingSeconds();
        playPauseButton.setText(timerRunning ? "Ⅱ" : "▶");
        statusText.setText(timerState.getAssistantMessage());
        updateTimerViews();
        if (timerState.isExpired() && !timerState.isAlarmAcknowledged()) {
            Intent intent = new Intent(this, CookingTimerDoneActivity.class);
            intent.putExtra(CookingTimerDoneActivity.EXTRA_RECIPE_ID, timerState.getRecipeId());
            startActivity(intent);
        }
    }

    private String createStepTitle(String stepText) {
        if (stepText == null || stepText.trim().isEmpty()) {
            return "Bước nấu";
        }
        String trimmed = stepText.trim();
        int commaIndex = trimmed.indexOf(',');
        if (commaIndex > 4 && commaIndex < 24) {
            return trimmed.substring(0, commaIndex);
        }
        int periodIndex = trimmed.indexOf('.');
        if (periodIndex > 4 && periodIndex < 24) {
            return trimmed.substring(0, periodIndex);
        }
        return trimmed.length() > 24 ? trimmed.substring(0, 24).trim() : trimmed;
    }

    private void openVoiceAssistant() {
        Intent intent = new Intent(this, VoiceAssistantActivity.class);
        long recipeId = currentState != null && currentState.hasRecipe()
                ? currentState.getRecipe().getId()
                : activeRecipeId;
        intent.putExtra(EXTRA_RECIPE_ID, recipeId);
        startActivity(intent);
    }

    private void openTimer() {
        if (currentState == null || currentState.isCompleted() || stepTotalSeconds <= 0) {
            return;
        }
        Intent intent = new Intent(this, CookingTimerActivity.class);
        long recipeId = currentState.hasRecipe() ? currentState.getRecipe().getId() : activeRecipeId;
        intent.putExtra(CookingTimerActivity.EXTRA_RECIPE_ID, recipeId);
        startActivity(intent);
    }

    private void speakCurrentInstruction(boolean force) {
        if (voiceLocalDataSource == null || voiceSpeaker == null || currentState == null
                || !currentState.hasRecipe() || currentState.isCompleted()) {
            return;
        }
        if (!voiceLocalDataSource.isVoiceEnabled()) {
            voiceSpeaker.stop();
            return;
        }
        if (!force && !voiceLocalDataSource.isAutoReadEnabled()) {
            return;
        }
        String speakKey = currentState.getRecipe().getId()
                + ":" + currentState.getCurrentStepIndex()
                + ":" + currentState.getCurrentStepText();
        if (!force && speakKey.equals(lastAutoSpokenKey)) {
            return;
        }
        lastAutoSpokenKey = speakKey;
        voiceSpeaker.speak("Bước " + (currentState.getCurrentStepIndex() + 1)
                + ". " + currentState.getCurrentStepText());
    }
}
