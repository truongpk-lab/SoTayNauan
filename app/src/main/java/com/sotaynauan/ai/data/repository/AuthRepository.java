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

public class AuthRepository {
    private final AuthLocalDataSource authLocalDataSource;
    private final SessionRepository sessionRepository;

    public AuthRepository(AuthLocalDataSource authLocalDataSource, SessionRepository sessionRepository) {
        this.authLocalDataSource = authLocalDataSource;
        this.sessionRepository = sessionRepository;
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
        AuthResult validation = validateCredentials(credentials);
        if (!validation.isSuccess()) {
            return validation;
        }

        String email = normalizeEmail(credentials.getEmail());
        if (authLocalDataSource.findUserByEmail(email) != null) {
            return AuthResult.error("Email này đã được đăng ký trên thiết bị.");
        }

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
        return Patterns.EMAIL_ADDRESS.matcher(normalizeEmail(email)).matches();
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
