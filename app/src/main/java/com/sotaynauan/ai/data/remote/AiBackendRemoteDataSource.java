package com.sotaynauan.ai.data.remote;

import com.sotaynauan.ai.data.model.ConfirmedIngredient;
import com.sotaynauan.ai.data.model.DetectedIngredient;
import com.sotaynauan.ai.data.model.RecipeMatch;

import org.json.JSONArray;
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
import android.util.Base64;
import java.util.List;

public class AiBackendRemoteDataSource {
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 18000;

    private final String baseUrl;

    public AiBackendRemoteDataSource(String baseUrl) {
        String safeBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (safeBaseUrl.endsWith("/")) {
            safeBaseUrl = safeBaseUrl.substring(0, safeBaseUrl.length() - 1);
        }
        this.baseUrl = safeBaseUrl;
    }

    public boolean isConfigured() {
        return !baseUrl.isEmpty();
    }

    public String generateRecipeAdvice(List<String> selectedIngredients,
                                       List<RecipeMatch> matches) throws IOException, JSONException {
        return generateRecipeAdvice(selectedIngredients, matches, null);
    }

    public String generateRecipeAdvice(List<String> selectedIngredients,
                                       List<RecipeMatch> matches,
                                       List<JSONObject> pantryItems) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("selectedIngredients", new JSONArray(selectedIngredients));
        JSONArray pantryRows = new JSONArray();
        if (pantryItems != null) {
            for (JSONObject pantryItem : pantryItems) {
                pantryRows.put(pantryItem);
            }
        }
        body.put("pantryItems", pantryRows);
        JSONArray matchRows = new JSONArray();
        if (matches != null) {
            for (RecipeMatch match : matches) {
                matchRows.put(new JSONObject()
                        .put("recipeId", match.getRecipe().getId())
                        .put("recipeName", match.getRecipe().getName())
                        .put("scorePercent", match.getScorePercent())
                        .put("availableIngredients", new JSONArray(match.getAvailableIngredients()))
                        .put("missingIngredients", new JSONArray(match.getMissingIngredients()))
                        .put("readinessLabel", match.getReadinessLabel()));
            }
        }
        body.put("matches", matchRows);
        return postForText("/api/ai/recipe-suggestions", body);
    }

    public String generateVoiceReply(String command,
                                     String contextLabel,
                                     String localResponse) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("command", command == null ? "" : command)
                .put("contextLabel", contextLabel == null ? "" : contextLabel)
                .put("localResponse", localResponse == null ? "" : localResponse);
        return postForText("/api/ai/voice", body);
    }

    public List<DetectedIngredient> detectIngredientsFromImage(byte[] imageBytes,
                                                               String mimeType) throws IOException, JSONException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Ảnh chụp rỗng.");
        }
        JSONObject body = new JSONObject()
                .put("imageBase64", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                .put("mimeType", mimeType == null || mimeType.trim().isEmpty()
                        ? "image/jpeg"
                        : mimeType);
        JSONObject response = postForJson("/api/ai/ingredient-detection", body);
        JSONArray rows = response.optJSONArray("ingredients");
        List<DetectedIngredient> ingredients = new java.util.ArrayList<>();
        if (rows == null) {
            return ingredients;
        }
        for (int index = 0; index < rows.length(); index++) {
            Object row = rows.opt(index);
            DetectedIngredient ingredient = parseDetectedIngredient(row);
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    private DetectedIngredient parseDetectedIngredient(Object row) {
        if (row instanceof JSONObject) {
            JSONObject object = (JSONObject) row;
            String name = object.optString("name", "").trim();
            String quantity = object.optString("quantity", "").trim();
            int count = object.optInt("count", 0);
            double confidence = object.optDouble("confidence", 0d);
            String source = object.optString("source", ConfirmedIngredient.SOURCE_CAMERA);
            JSONArray boxRows = object.optJSONArray("boxes");
            List<DetectedIngredient.Box> boxes = new java.util.ArrayList<>();
            if (boxRows != null) {
                for (int index = 0; index < boxRows.length(); index++) {
                    JSONObject box = boxRows.optJSONObject(index);
                    if (box == null) {
                        continue;
                    }
                    boxes.add(new DetectedIngredient.Box(
                            box.optDouble("x1", 0d),
                            box.optDouble("y1", 0d),
                            box.optDouble("x2", 0d),
                            box.optDouble("y2", 0d),
                            box.optDouble("confidence", confidence)));
                }
            }
            return new DetectedIngredient(name, quantity, count, confidence, source, boxes);
        }
        return new DetectedIngredient(String.valueOf(row == null ? "" : row).trim(),
                "", 0, 0d, ConfirmedIngredient.SOURCE_CAMERA, new java.util.ArrayList<>());
    }

    public String matchVoiceCommand(String spokenText,
                                    String contextLabel,
                                    List<String> allowedCommands) throws IOException, JSONException {
        JSONArray commands = new JSONArray();
        if (allowedCommands != null) {
            for (String command : allowedCommands) {
                commands.put(command == null ? "" : command);
            }
        }
        JSONObject body = new JSONObject()
                .put("spokenText", spokenText == null ? "" : spokenText)
                .put("contextLabel", contextLabel == null ? "" : contextLabel)
                .put("allowedCommands", commands);
        return postForText("/api/ai/voice-command-match", body);
    }

    public VoiceAudioMatchResult matchVoiceAudioCommand(byte[] audioBytes,
                                                        String mimeType,
                                                        String contextLabel,
                                                        List<String> allowedCommands) throws IOException, JSONException {
        JSONArray commands = new JSONArray();
        if (allowedCommands != null) {
            for (String command : allowedCommands) {
                commands.put(command == null ? "" : command);
            }
        }
        JSONObject body = new JSONObject()
                .put("audioBase64", Base64.encodeToString(audioBytes, Base64.NO_WRAP))
                .put("mimeType", mimeType == null || mimeType.trim().isEmpty() ? "audio/mp4" : mimeType)
                .put("contextLabel", contextLabel == null ? "" : contextLabel)
                .put("allowedCommands", commands);
        JSONObject response = postForJson("/api/ai/voice-audio-command-match", body);
        return new VoiceAudioMatchResult(
                response.optString("transcript", "").trim(),
                response.optString("text", "").trim());
    }

    private String postForText(String path, JSONObject body) throws IOException, JSONException {
        JSONObject response = postForJson(path, body);
        String text = response.optString("text", "").trim();
        if (text.isEmpty()) {
            throw new IOException("AI backend trả về text rỗng.");
        }
        return text;
    }

    private JSONObject postForJson(String path, JSONObject body) throws IOException, JSONException {
        if (!isConfigured()) {
            throw new IOException("AI backend chưa được cấu hình.");
        }
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
                throw new IOException("AI backend lỗi " + statusCode + ": " + response);
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public static class VoiceAudioMatchResult {
        private final String transcript;
        private final String command;

        public VoiceAudioMatchResult(String transcript, String command) {
            this.transcript = transcript;
            this.command = command;
        }

        public String getTranscript() {
            return transcript;
        }

        public String getCommand() {
            return command;
        }
    }
}
