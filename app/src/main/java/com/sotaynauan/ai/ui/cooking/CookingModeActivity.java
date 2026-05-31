package com.sotaynauan.ai.ui.cooking;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.io.File;
import java.io.FileOutputStream;

public class CookingModeActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";
    private static final int REQUEST_FINISHED_PHOTO = 84;

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
    private Button captureFinishedPhotoButton;
    private Button saveFinishedNoteButton;
    private LinearLayout stepsContainer;
    private LinearLayout finishedJournal;
    private FrameLayout photoFrame;
    private ImageView stepPhoto;
    private ImageView finishedPhoto;
    private EditText finishedNote;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FINISHED_PHOTO || resultCode != RESULT_OK || data == null) {
            return;
        }
        Object rawBitmap = data.getExtras() == null ? null : data.getExtras().get("data");
        if (!(rawBitmap instanceof Bitmap) || currentState == null || !currentState.hasRecipe()) {
            statusText.setText("Không lấy được ảnh món ăn sau khi nấu.");
            return;
        }
        String photoUri = saveFinishedPhoto((Bitmap) rawBitmap);
        if (photoUri.isEmpty()) {
            statusText.setText("Không lưu được ảnh món ăn sau khi nấu.");
            return;
        }
        long recipeId = currentState.getRecipe().getId();
        viewModel.saveFinishedPhoto(recipeId, photoUri);
        finishedPhoto.setImageURI(Uri.parse(photoUri));
        statusText.setText("Đã lưu ảnh thành phẩm cho món " + currentState.getRecipe().getName() + ".");
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
        captureFinishedPhotoButton = findViewById(R.id.cookingCaptureFinishedPhotoButton);
        saveFinishedNoteButton = findViewById(R.id.cookingSaveFinishedNoteButton);
        stepsContainer = findViewById(R.id.cookingStepsContainer);
        finishedJournal = findViewById(R.id.cookingFinishedJournal);
        photoFrame = findViewById(R.id.cookingPhotoFrame);
        stepPhoto = findViewById(R.id.cookingStepPhoto);
        finishedPhoto = findViewById(R.id.cookingFinishedPhoto);
        finishedNote = findViewById(R.id.cookingFinishedNote);
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
        captureFinishedPhotoButton.setOnClickListener(view -> captureFinishedPhoto());
        saveFinishedNoteButton.setOnClickListener(view -> saveFinishedNote());
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
            finishedJournal.setVisibility(View.GONE);
            updateTimerViews();
            return;
        }

        Recipe recipe = state.getRecipe();
        RecipeImageResolver.apply(stepPhoto, recipe);
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
        bindFinishedJournal(state);
        updateTimerViews();
        speakCurrentInstruction(false);
    }

    private void bindFinishedJournal(CookingSessionState state) {
        if (!state.hasRecipe() || !state.isCompleted()) {
            finishedJournal.setVisibility(View.GONE);
            return;
        }
        long recipeId = state.getRecipe().getId();
        finishedJournal.setVisibility(View.VISIBLE);
        String photoUri = viewModel.getFinishedPhoto(recipeId);
        if (photoUri == null || photoUri.trim().isEmpty()) {
            finishedPhoto.setImageResource(RecipeImageResolver.resolve(this, state.getRecipe()));
        } else {
            finishedPhoto.setImageURI(Uri.parse(photoUri));
        }
        finishedNote.setText(viewModel.getFinishedNote(recipeId));
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

    private void captureFinishedPhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            statusText.setText("Thiết bị chưa có ứng dụng camera để chụp ảnh.");
            return;
        }
        startActivityForResult(intent, REQUEST_FINISHED_PHOTO);
    }

    private void saveFinishedNote() {
        if (currentState == null || !currentState.hasRecipe()) {
            return;
        }
        viewModel.saveFinishedNote(currentState.getRecipe().getId(),
                finishedNote.getText().toString().trim());
        statusText.setText("Đã lưu ghi chú sau khi nấu cho món "
                + currentState.getRecipe().getName() + ".");
    }

    private String saveFinishedPhoto(Bitmap bitmap) {
        try {
            File directory = new File(getFilesDir(), "finished_photos");
            if (!directory.exists() && !directory.mkdirs()) {
                return "";
            }
            File photoFile = new File(directory,
                    String.format(Locale.US, "finished_%d.jpg", System.currentTimeMillis()));
            FileOutputStream outputStream = new FileOutputStream(photoFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream);
            outputStream.close();
            return Uri.fromFile(photoFile).toString();
        } catch (Exception exception) {
            return "";
        }
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
