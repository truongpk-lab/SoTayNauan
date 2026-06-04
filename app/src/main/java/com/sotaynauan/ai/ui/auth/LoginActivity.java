package com.sotaynauan.ai.ui.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sotaynauan.ai.BuildConfig;
import com.sotaynauan.ai.R;
import com.sotaynauan.ai.adapter.auth.AuthFormAdapter;
import com.sotaynauan.ai.data.local.datasource.AuthLocalDataSource;
import com.sotaynauan.ai.data.local.datasource.SessionLocalDataSource;
import com.sotaynauan.ai.data.model.AuthCredentials;
import com.sotaynauan.ai.data.model.AuthResult;
import com.sotaynauan.ai.data.remote.AuthRemoteDataSource;
import com.sotaynauan.ai.data.repository.AuthRepository;
import com.sotaynauan.ai.data.repository.SessionRepository;
import com.sotaynauan.ai.ui.home.HomeActivity;
import com.sotaynauan.ai.util.AppExecutors;

import java.security.SecureRandom;
import java.util.Locale;

public class LoginActivity extends Activity {
    private LoginViewModel viewModel;
    private AuthFormAdapter formAdapter;
    private AuthRemoteDataSource authRemoteDataSource;
    private final SecureRandom secureRandom = new SecureRandom();
    private AuthCredentials pendingRegistrationCredentials;
    private String pendingRegistrationEmail = "";
    private String pendingOtp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_minimal);

        SessionRepository sessionRepository = new SessionRepository(new SessionLocalDataSource(this));
        AuthRepository authRepository = new AuthRepository(new AuthLocalDataSource(this), sessionRepository);
        viewModel = new LoginViewModel(authRepository);
        authRemoteDataSource = new AuthRemoteDataSource(BuildConfig.AI_BACKEND_BASE_URL);

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
        registerButton.setOnClickListener(view -> startRegistrationOtpFlow(formAdapter.getCredentials()));
        forgotPasswordButton.setOnClickListener(view -> {
            AuthResult result = viewModel.recoverPassword(formAdapter.getEmail());
            formAdapter.bindResult(result);
        });
    }

    private void startRegistrationOtpFlow(AuthCredentials credentials) {
        AuthResult validation = viewModel.validateRegistrationRequest(credentials);
        formAdapter.bindResult(validation);
        if (!validation.isSuccess()) {
            return;
        }

        pendingRegistrationCredentials = credentials;
        pendingRegistrationEmail = credentials.getEmail().trim().toLowerCase(Locale.US);
        pendingOtp = generateOtp();
        formAdapter.clearInputs();
        sendOtpAndShowDialog(true);
    }

    private void sendOtpAndShowDialog(boolean showDialogAfterSend) {
        if (pendingRegistrationCredentials == null || pendingRegistrationEmail.isEmpty()) {
            formAdapter.bindResult(AuthResult.error("Hãy nhập email và mật khẩu để đăng ký trước."));
            return;
        }
        formAdapter.bindResult(AuthResult.success(
                "Đang gửi OTP tới " + pendingRegistrationEmail + "...", null));
        AppExecutors.runOnIo(
                () -> {
                    authRemoteDataSource.sendRegistrationOtp(pendingRegistrationEmail, pendingOtp);
                    return true;
                },
                sent -> {
                    formAdapter.bindResult(AuthResult.success(
                            "Đã gửi OTP tới email. Hãy kiểm tra Gmail/hộp thư.", null));
                    if (showDialogAfterSend) {
                        showOtpDialog();
                    }
                },
                exception -> {
                    formAdapter.bindResult(AuthResult.error(
                            readableOtpSendError(exception)));
                });
    }

    private void showOtpDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(8), dp(22), dp(4));

        TextView emailText = new TextView(this);
        emailText.setText("Email: " + pendingRegistrationEmail);
        emailText.setTextColor(getColor(R.color.on_surface_variant));
        emailText.setTextSize(15f);
        content.addView(emailText);

        TextView instructionText = new TextView(this);
        instructionText.setText("Mã OTP đã được gửi về email. Vui lòng kiểm tra Gmail hoặc hộp thư đến.");
        instructionText.setTextColor(getColor(R.color.primary));
        instructionText.setTextSize(15f);
        instructionText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams instructionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        instructionParams.setMargins(0, dp(12), 0, 0);
        content.addView(instructionText, instructionParams);

        EditText otpInput = new EditText(this);
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        otpInput.setSingleLine(true);
        otpInput.setHint("Nhập mã OTP 6 số");
        otpInput.setBackgroundResource(R.drawable.bg_auth_input);
        otpInput.setTextColor(getColor(R.color.on_surface));
        otpInput.setHintTextColor(getColor(R.color.app_outline_variant));
        otpInput.setPadding(dp(16), 0, dp(16), 0);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56));
        inputParams.setMargins(0, dp(16), 0, 0);
        content.addView(otpInput, inputParams);

        TextView titleView = new TextView(this);
        titleView.setText("Xác nhận OTP");
        titleView.setTextColor(getColor(R.color.primary));
        titleView.setTextSize(20f);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(dp(22), dp(20), dp(22), dp(4));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(titleView)
                .setView(content)
                .setPositiveButton("Xác nhận", null)
                .setNegativeButton("Hủy", (dialogInterface, which) -> clearPendingRegistration())
                .setNeutralButton("Gửi lại", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_auth_card);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.primary));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.on_surface_variant));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.primary_container));

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String enteredOtp = otpInput.getText().toString().trim();
                if (!pendingOtp.equals(enteredOtp)) {
                    otpInput.setError("Mã OTP chưa đúng.");
                    formAdapter.bindResult(AuthResult.error(
                            "Mã OTP chưa đúng. Vui lòng kiểm tra và nhập lại."));
                    return;
                }
                AuthResult result = viewModel.register(pendingRegistrationCredentials);
                clearPendingRegistration();
                dialog.dismiss();
                handleAuthResult(result);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                pendingOtp = generateOtp();
                otpInput.setText("");
                otpInput.setError(null);
                sendOtpAndShowDialog(false);
            });
        });
        dialog.show();
    }

    private String generateOtp() {
        return String.format(Locale.US, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String readableOtpSendError(Exception exception) {
        String message = exception == null || exception.getMessage() == null
                ? ""
                : exception.getMessage();
        if (message.contains("SMTP_NOT_CONFIGURED")
                || message.contains("SMTP_HOST")
                || message.contains("SMTP_USER")
                || message.contains("SMTP_PASS")) {
            return "Chưa gửi được OTP. Hãy điền Gmail SMTP trong backend/.env rồi chạy lại backend.";
        }
        if (message.contains("SMTP_SEND_FAILED")) {
            return "Chưa gửi được OTP. Kiểm tra Gmail App Password trong backend/.env.";
        }
        return "Chưa gửi được OTP. Kiểm tra backend và cấu hình Gmail SMTP.";
    }

    private void clearPendingRegistration() {
        pendingRegistrationCredentials = null;
        pendingRegistrationEmail = "";
        pendingOtp = "";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
