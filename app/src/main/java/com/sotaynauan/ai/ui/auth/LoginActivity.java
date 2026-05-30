package com.sotaynauan.ai.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.auth.AuthFormAdapter;
import com.sotaynauan.ai.data.local.datasource.AuthLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.SessionLocalDataSource;
import com.sotaynauan.ai.data.model.AuthResult;
import com.sotaynauan.ai.data.repository.AuthRepository;
import com.sotaynauan.ai.data.repository.SessionRepository;
import com.sotaynauan.ai.ui.home.HomeActivity;

public class LoginActivity extends Activity {
    private LoginViewModel viewModel;
    private AuthFormAdapter formAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_minimal);

        SessionRepository sessionRepository = new SessionRepository(new SessionLocalDataSource(this));
        AuthRepository authRepository = new AuthRepository(new AuthLocalDataSource(this), sessionRepository);
        viewModel = new LoginViewModel(authRepository);

        ImageView heroImage = findViewById(R.id.authHeroImage);
        heroImage.post(() -> alignHeroPreviewToTop(heroImage));

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        TextView statusText = findViewById(R.id.authStatusText);
        TextView forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerButton);

        formAdapter = new AuthFormAdapter(emailInput, passwordInput, statusText);

        loginButton.setOnClickListener(view -> handleAuthResult(viewModel.login(formAdapter.getCredentials())));
        registerButton.setOnClickListener(view -> handleAuthResult(viewModel.register(formAdapter.getCredentials())));
        forgotPasswordButton.setOnClickListener(view -> {
            AuthResult result = viewModel.recoverPassword(formAdapter.getEmail());
            formAdapter.bindResult(result);
        });
    }

    private void handleAuthResult(AuthResult result) {
        formAdapter.bindResult(result);
        if (result.isSuccess()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    private void alignHeroPreviewToTop(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null || imageView.getWidth() == 0) {
            return;
        }
        float scale = (float) imageView.getWidth() / (float) drawable.getIntrinsicWidth();
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(0f, -64f * getResources().getDisplayMetrics().density);
        imageView.setImageMatrix(matrix);
    }
}
