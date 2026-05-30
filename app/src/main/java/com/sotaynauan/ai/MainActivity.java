package com.sotaynauan.ai;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.sotaynauan.ai.data.local.datasource.SessionLocalDataSource;
import com.sotaynauan.ai.data.repository.SessionRepository;
import com.sotaynauan.ai.ui.auth.LoginActivity;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.ui.splash.SplashDestination;
import com.sotaynauan.ai.ui.splash.SplashViewModel;

public class MainActivity extends Activity {
    private static final long SPLASH_DELAY_MS = 1600L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SplashViewModel viewModel;

    private final Runnable routeRunnable = new Runnable() {
        @Override
        public void run() {
            openDestination(viewModel.resolveDestination());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionRepository repository = new SessionRepository(new SessionLocalDataSource(this));
        viewModel = new SplashViewModel(repository);

        animateSplash();
        handler.postDelayed(routeRunnable, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(routeRunnable);
        super.onDestroy();
    }

    private void openDestination(SplashDestination destination) {
        Intent intent = destination == SplashDestination.HOME
                ? new Intent(this, HomeActivity.class)
                : new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void animateSplash() {
        View logo = findViewById(R.id.splashLogo);
        View title = findViewById(R.id.splashTitle);
        View subtitle = findViewById(R.id.splashSubtitle);
        View dots = findViewById(R.id.loadingDots);

        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.82f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.82f, 1f);
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f);
        ObjectAnimator titleMove = ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, 24f, 0f);
        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f);
        ObjectAnimator dotsAlpha = ObjectAnimator.ofFloat(dots, View.ALPHA, 0f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(logoAlpha, logoScaleX, logoScaleY, titleAlpha, titleMove, subtitleAlpha, dotsAlpha);
        set.setDuration(900L);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}
