package com.sotaynauan.ai.ui.cooking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.datasource.CookingLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.RecipeLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.ShoppingLocalDataSource;
import com.sotaynauan.ai.data.mapper.RecipeMapper;
import com.sotaynauan.ai.data.model.CookingTimerState;
import com.sotaynauan.ai.data.repository.CookingRepository;
import com.sotaynauan.ai.data.repository.RecipeRepository;
import com.sotaynauan.ai.data.repository.ShoppingRepository;
import com.sotaynauan.ai.data.seed.SeedDataProvider;

import java.util.Locale;

public class CookingTimerActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private CookingTimerViewModel viewModel;
    private CookingTimerRingView timerProgressRing;
    private TextView clockText;
    private TextView remainingText;
    private TextView assistantMessage;
    private TextView assistantStatus;
    private Button pauseResumeButton;
    private Button addMinuteButton;
    private long activeRecipeId = -1L;
    private boolean timerDoneOpened;
    private boolean destroyed;

    private final Runnable timerTicker = new Runnable() {
        @Override
        public void run() {
            if (!isActive()) {
                return;
            }
            bindTimer(viewModel.getTimerState());
            timerHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_timer);

        viewModel = new CookingTimerViewModel(createCookingRepository());
        bindViews();
        bindActions();

        activeRecipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        bindTimer(viewModel.prepareTimer(activeRecipeId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        destroyed = false;
        timerHandler.removeCallbacks(timerTicker);
        timerHandler.post(timerTicker);
    }

    @Override
    protected void onPause() {
        timerHandler.removeCallbacks(timerTicker);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        timerHandler.removeCallbacksAndMessages(null);
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
        timerProgressRing = findViewById(R.id.timerProgressRing);
        clockText = findViewById(R.id.timerClockText);
        remainingText = findViewById(R.id.timerRemainingText);
        assistantMessage = findViewById(R.id.timerAssistantMessage);
        assistantStatus = findViewById(R.id.timerAssistantStatus);
        pauseResumeButton = findViewById(R.id.timerPauseResumeButton);
        addMinuteButton = findViewById(R.id.timerAddMinuteButton);
    }

    private void bindActions() {
        findViewById(R.id.timerCloseButton).setOnClickListener(view -> finish());
        pauseResumeButton.setOnClickListener(view -> {
            CookingTimerState state = viewModel.getTimerState();
            bindTimer(state.isRunning() ? viewModel.pauseTimer() : viewModel.resumeTimer());
        });
        addMinuteButton.setOnClickListener(view -> bindTimer(viewModel.addOneMinute()));
    }

    private void bindTimer(CookingTimerState state) {
        if (!isActive()) {
            return;
        }
        int remainingSeconds = Math.max(0, state.getRemainingSeconds());
        if (state.isExpired() && !state.isAlarmAcknowledged()) {
            openTimerDone(state);
        } else if (!state.isExpired()) {
            timerDoneOpened = false;
        }
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        clockText.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        remainingText.setText(createRemainingText(remainingSeconds));
        assistantMessage.setText("\"" + state.getAssistantMessage() + "\"");
        assistantStatus.setText(state.isRunning()
                ? "●  Trợ lý AI đang nhắc nhở"
                : "●  Timer đang tạm dừng");
        pauseResumeButton.setText(state.isRunning() ? "Ⅱ  Tạm dừng" : "▶  Tiếp tục");
        addMinuteButton.setEnabled(state.getTotalSeconds() > 0);

        float progress = state.getTotalSeconds() <= 0
                ? 0f
                : remainingSeconds / (float) state.getTotalSeconds();
        timerProgressRing.setProgress(progress);

        int timerColor = state.isNearEnd() ? 0xFFA85600 : 0xFF944A00;
        if (state.isExpired()) {
            timerColor = 0xFFBA1A1A;
        }
        clockText.setTextColor(timerColor);
        remainingText.setTextColor(timerColor);
    }

    private void openTimerDone(CookingTimerState state) {
        if (timerDoneOpened) {
            return;
        }
        timerDoneOpened = true;
        Intent intent = new Intent(this, CookingTimerDoneActivity.class);
        intent.putExtra(CookingTimerDoneActivity.EXTRA_RECIPE_ID,
                state.getRecipeId() > 0 ? state.getRecipeId() : activeRecipeId);
        startActivity(intent);
    }

    private String createRemainingText(int remainingSeconds) {
        if (remainingSeconds <= 0) {
            return "Đã hết giờ";
        }
        int minutes = Math.max(1, (int) Math.ceil(remainingSeconds / 60.0));
        return "Còn " + minutes + " phút";
    }

    private boolean isActive() {
        return !destroyed && !isFinishing() && !isDestroyed();
    }
}
