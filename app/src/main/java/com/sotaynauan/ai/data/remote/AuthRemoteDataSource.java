package com.sotaynauan.ai.data.remote;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthRemoteDataSource {
    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int READ_TIMEOUT_MS = 6000;

    private final String baseUrl;

    public AuthRemoteDataSource(String baseUrl) {
        String safeBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (safeBaseUrl.endsWith("/")) {
            safeBaseUrl = safeBaseUrl.substring(0, safeBaseUrl.length() - 1);
        }
        this.baseUrl = safeBaseUrl;
    }

    public void sendRegistrationOtp(String email, String otp) throws IOException, JSONException {
        if (baseUrl.isEmpty()) {
            throw new IOException("Backend gửi OTP chưa được cấu hình.");
        }
        postJson("/api/auth/send-otp", new JSONObject()
                .put("email", email == null ? "" : email.trim())
                .put("otp", otp == null ? "" : otp.trim()));
    }

    private JSONObject postJson(String path, JSONObject body) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(body.toString());
            writer.flush();
            writer.close();

            int statusCode = connection.getResponseCode();
            String response = readStream(statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException(readBackendMessage(statusCode, response));
            }
            return new JSONObject(response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private String readBackendMessage(int statusCode, String response) {
        if (response == null || response.trim().isEmpty()) {
            return "Backend gửi OTP lỗi " + statusCode + ".";
        }
        try {
            JSONObject object = new JSONObject(response);
            String message = object.optString("message", "").trim();
            if (message.isEmpty()) {
                message = object.optString("error", "").trim();
            }
            return message.isEmpty() ? "Backend gửi OTP lỗi " + statusCode + "." : message;
        } catch (JSONException exception) {
            return response.trim();
        }
    }
}
