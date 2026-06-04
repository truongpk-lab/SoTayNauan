package com.sotaynauan.ai.data.repository;

import android.util.Patterns;

import com.sotaynauan.ai.data.local.datasource.AuthLocalDataSource;
import com.sotaynauan.ai.data.model.AppSession;
import com.sotaynauan.ai.data.model.AuthCredentials;
import com.sotaynauan.ai.data.model.AuthResult;
import com.sotaynauan.ai.data.model.AuthUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthRepository {
    private static final String DEMO_EMAIL = "demo@local.test";
    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_DISPLAY_NAME = "Demo Chef";
    private static final Pattern EMAIL_STRUCTURE_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE);

    private final AuthLocalDataSource authLocalDataSource;
    private final SessionRepository sessionRepository;

    public AuthRepository(AuthLocalDataSource authLocalDataSource, SessionRepository sessionRepository) {
        this.authLocalDataSource = authLocalDataSource;
        this.sessionRepository = sessionRepository;
        ensureDemoAccount();
    }

    public AuthResult login(AuthCredentials credentials) {
        AuthResult validation = validateCredentials(credentials);
        if (!validation.isSuccess()) {
            return validation;
        }

        String email = normalizeEmail(credentials.getEmail());
        AuthUser user = authLocalDataSource.findUserByEmail(email);
        if (user == null) {
            return AuthResult.error("Email này chưa có tài khoản local. Hãy đăng ký trước.");
        }
        if (!user.getPasswordHash().equals(hashPassword(credentials.getPassword(), email))) {
            return AuthResult.error("Mật khẩu chưa đúng. Kiểm tra lại giúp mình nhé.");
        }

        sessionRepository.saveSession(new AppSession(user.getUserId(), user.getDisplayName(), true));
        return AuthResult.success("Đăng nhập thành công. Bếp nhà đã sẵn sàng.", user);
    }

    public AuthResult register(AuthCredentials credentials) {
        AuthResult validation = validateRegistrationRequest(credentials);
        if (!validation.isSuccess()) {
            return validation;
        }

        String email = normalizeEmail(credentials.getEmail());
        String displayName = buildDisplayName(email);
        AuthUser user = new AuthUser(
                UUID.randomUUID().toString(),
                email,
                displayName,
                hashPassword(credentials.getPassword(), email),
                System.currentTimeMillis()
        );
        authLocalDataSource.saveUser(user);
        sessionRepository.saveSession(new AppSession(user.getUserId(), user.getDisplayName(), true));
        return AuthResult.success("Tạo tài khoản local thành công.", user);
    }

    public AuthResult validateRegistrationRequest(AuthCredentials credentials) {
        AuthResult validation = validateCredentials(credentials);
        if (!validation.isSuccess()) {
            return validation;
        }
        String email = normalizeEmail(credentials.getEmail());
        if (authLocalDataSource.findUserByEmail(email) != null) {
            return AuthResult.error("Email này đã được đăng ký trên thiết bị.");
        }
        return AuthResult.success("Email hợp lệ. OTP sẽ được gửi về email để xác nhận đăng ký.", null);
    }

    public AuthResult recoverPassword(String email) {
        if (!isValidEmail(email)) {
            return AuthResult.error("Nhập email hợp lệ để kiểm tra tài khoản local.");
        }
        AuthUser user = authLocalDataSource.findUserByEmail(normalizeEmail(email));
        if (user == null) {
            return AuthResult.error("Chưa tìm thấy tài khoản local với email này.");
        }
        return AuthResult.success("Tài khoản có trên thiết bị. Vì dữ liệu local đã mã hóa mật khẩu, hãy tạo tài khoản mới nếu bạn quên mật khẩu.", user);
    }

    private void ensureDemoAccount() {
        String email = normalizeEmail(DEMO_EMAIL);
        if (authLocalDataSource.findUserByEmail(email) != null) {
            return;
        }

        AuthUser demoUser = new AuthUser(
                UUID.randomUUID().toString(),
                email,
                DEMO_DISPLAY_NAME,
                hashPassword(DEMO_PASSWORD, email),
                System.currentTimeMillis()
        );
        authLocalDataSource.saveUser(demoUser);
    }

    private AuthResult validateCredentials(AuthCredentials credentials) {
        if (!isValidEmail(credentials.getEmail())) {
            return AuthResult.error("Email chưa hợp lệ.");
        }
        if (credentials.getPassword().length() < 6) {
            return AuthResult.error("Mật khẩu cần ít nhất 6 ký tự.");
        }
        return AuthResult.success("", null);
    }

    private boolean isValidEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()
                || normalizedEmail.contains(" ")
                || normalizedEmail.indexOf("@") != normalizedEmail.lastIndexOf("@")) {
            return false;
        }
        int atIndex = normalizedEmail.indexOf("@");
        if (atIndex <= 0 || atIndex >= normalizedEmail.length() - 1) {
            return false;
        }
        String domain = normalizedEmail.substring(atIndex + 1);
        return domain.contains(".")
                && EMAIL_STRUCTURE_PATTERN.matcher(normalizedEmail).matches()
                && Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private String buildDisplayName(String email) {
        int atIndex = email.indexOf("@");
        String name = atIndex > 0 ? email.substring(0, atIndex) : "Ban bep nha";
        return name.length() == 0 ? "Ban bep nha" : name;
    }

    private String hashPassword(String password, String emailSalt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((emailSalt + ":" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format(Locale.US, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString((emailSalt + ":" + password).hashCode());
        }
    }
}
