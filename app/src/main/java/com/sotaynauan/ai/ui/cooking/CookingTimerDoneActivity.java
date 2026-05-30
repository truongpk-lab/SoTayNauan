package com.sotaynauan.ai.ui.cooking;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.cooking.TimerQuickAddAdapter;
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

public class CookingTimerDoneActivity extends Activity {
    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private final Handler vibrationHandler = new Handler(Looper.getMainLooper());
    private CookingTimerViewModel viewModel;
    private Vibrator vibrator;
    private long recipeId;
    private boolean alarmActive;

    private final Runnable vibrationPulse = new Runnable() {
        @Override
        public void run() {
            if (!alarmActive) {
                return;
            }
            vibrateOnce();
            vibrationHandler.postDelayed(this, 1100L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_timer_done);
        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        viewModel = new CookingTimerViewModel(createCookingRepository());
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        styleSystemBars();
        bindActions();
        startAlarmIfNeeded();
        startVisualAlarm();
    }

    @Override
    protected void onDestroy() {
        stopAlarmHardware();
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

    private void bindActions() {
        findViewById(R.id.timerDoneStopAlarmButton).setOnClickListener(view -> {
            viewModel.stopAlarm();
            stopAlarmHardware();
        });

        findViewById(R.id.timerDoneCompleteButton).setOnClickListener(view -> showCompleteStepDialog());

        TimerQuickAddAdapter quickAddAdapter = new TimerQuickAddAdapter(this);
        quickAddAdapter.bind((LinearLayout) findViewById(R.id.timerDoneQuickAddContainer), minutes -> {
            viewModel.addMinutes(minutes);
            stopAlarmHardware();
            Intent intent = new Intent(this, CookingTimerActivity.class);
            intent.putExtra(CookingTimerActivity.EXTRA_RECIPE_ID, recipeId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void startAlarmIfNeeded() {
        CookingTimerState state = viewModel.getTimerState();
        if (!state.isExpired() || state.isAlarmAcknowledged()) {
            return;
        }
        if (recipeId <= 0L) {
            recipeId = state.getRecipeId();
        }
        alarmActive = true;
        vibrationHandler.removeCallbacks(vibrationPulse);
        vibrationHandler.post(vibrationPulse);
    }

    private void showCompleteStepDialog() {
        stopAlarmHardware();
        new AlertDialog.Builder(this)
                .setTitle("Hoàn thành bước này?")
                .setMessage("App sẽ chuyển sang bước nấu tiếp theo. Hãy chắc chắn bạn đã kiểm tra món ăn.")
                .setNegativeButton("Ở lại", (dialog, which) -> {
                    viewModel.stopAlarm();
                    dialog.dismiss();
                })
                .setPositiveButton("Chuyển bước", (dialog, which) -> {
                    viewModel.completeStepAfterTimer();
                    Intent intent = new Intent(this, CookingModeActivity.class);
                    intent.putExtra(CookingModeActivity.EXTRA_RECIPE_ID, recipeId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void stopAlarmHardware() {
        alarmActive = false;
        vibrationHandler.removeCallbacks(vibrationPulse);
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void vibrateOnce() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(450L, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(450L);
        }
    }

    private void startVisualAlarm() {
        TextView speaker = findViewById(R.id.timerDoneSpeaker);
        ScaleAnimation shake = new ScaleAnimation(
                0.96f, 1.04f, 0.96f, 1.04f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        shake.setDuration(360L);
        shake.setRepeatCount(Animation.INFINITE);
        shake.setRepeatMode(Animation.REVERSE);
        speaker.startAnimation(shake);

        startPulse(R.id.timerDoneWaveLarge, 900L);
        startPulse(R.id.timerDoneWaveSmall, 680L);
    }

    private void startPulse(int viewId, long duration) {
        AlphaAnimation pulse = new AlphaAnimation(0.18f, 0.42f);
        pulse.setDuration(duration);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        findViewById(viewId).startAnimation(pulse);
    }

    private void styleSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(0xFFBA1A1A);
        window.setNavigationBarColor(0xFFBA1A1A);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
            }
        }
    }
}
