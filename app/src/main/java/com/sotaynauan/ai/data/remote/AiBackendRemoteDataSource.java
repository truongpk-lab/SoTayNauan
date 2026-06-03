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
import java.util.ArrayList;
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

    public List<RecipeNameSuggestion> suggestRelatedRecipes(String dishName,
                                                            List<String> existingRecipeNames)
            throws IOException, JSONException {
        JSONArray existingRows = new JSONArray();
        if (existingRecipeNames != null) {
            for (String existingRecipeName : existingRecipeNames) {
                existingRows.put(existingRecipeName == null ? "" : existingRecipeName);
            }
        }
        JSONObject response = postForJson("/api/ai/related-recipes", new JSONObject()
                .put("dishName", dishName == null ? "" : dishName)
                .put("existingRecipeNames", existingRows));
        JSONArray rows = response.optJSONArray("suggestions");
        List<RecipeNameSuggestion> suggestions = new ArrayList<>();
        if (rows == null) {
            return suggestions;
        }
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            String name = row.optString("name", "").trim();
            if (!name.isEmpty()) {
                suggestions.add(new RecipeNameSuggestion(name,
                        row.optString("reason", "").trim()));
            }
        }
        return suggestions;
    }

    public GeneratedRecipe generateRecipeFromWeb(String recipeName)
            throws IOException, JSONException {
        JSONObject response = postForJson("/api/ai/import-recipe", new JSONObject()
                .put("recipeName", recipeName == null ? "" : recipeName));
        return GeneratedRecipe.fromJson(response.optJSONObject("recipe"));
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
                throw new IOException(createReadableBackendError(statusCode, response));
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

    private String createReadableBackendError(int statusCode, String response) {
        String fallback = "AI backend lỗi " + statusCode + ".";
        if (response == null || response.trim().isEmpty()) {
            return fallback;
        }
        try {
            JSONObject object = new JSONObject(response);
            String message = object.optString("message", "").trim();
            if (message.isEmpty()) {
                message = object.optString("error", "").trim();
            }
            int retryAfterSeconds = object.optInt("retryAfterSeconds", 0);
            if (message.isEmpty()) {
                return fallback;
            }
            if (retryAfterSeconds > 0 && !message.contains(String.valueOf(retryAfterSeconds))) {
                message += " Thử lại sau khoảng " + retryAfterSeconds + " giây.";
            }
            return message;
        } catch (JSONException exception) {
            String compact = response.trim().replaceAll("\\s+", " ");
            if (compact.length() > 240) {
                compact = compact.substring(0, 240) + "...";
            }
            return fallback + " " + compact;
        }
    }

    public static class RecipeNameSuggestion {
        private final String name;
        private final String reason;

        public RecipeNameSuggestion(String name, String reason) {
            this.name = name;
            this.reason = reason;
        }

        public String getName() {
            return name;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class GeneratedRecipe {
        private final String name;
        private final String description;
        private final int totalMinutes;
        private final String difficulty;
        private final String category;
        private final String serving;
        private final String calories;
        private final String cost;
        private final String imageUrl;
        private final List<String> ingredients;
        private final List<String> steps;

        public GeneratedRecipe(String name, String description, int totalMinutes,
                               String difficulty, String category, String serving,
                               String calories, String cost, String imageUrl, List<String> ingredients,
                               List<String> steps) {
            this.name = name;
            this.description = description;
            this.totalMinutes = totalMinutes;
            this.difficulty = difficulty;
            this.category = category;
            this.serving = serving;
            this.calories = calories;
            this.cost = cost;
            this.imageUrl = imageUrl == null ? "" : imageUrl;
            this.ingredients = new ArrayList<>(ingredients);
            this.steps = new ArrayList<>(steps);
        }

        private static GeneratedRecipe fromJson(JSONObject object) throws IOException {
            if (object == null) {
                throw new IOException("AI backend chưa trả về công thức.");
            }
            String name = object.optString("name", "").trim();
            List<String> ingredients = readStringArray(object.optJSONArray("ingredients"));
            List<String> steps = readStringArray(object.optJSONArray("steps"));
            if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
                throw new IOException("Công thức AI thiếu tên món, nguyên liệu hoặc bước nấu.");
            }
            return new GeneratedRecipe(name,
                    object.optString("description", "").trim(),
                    Math.max(5, object.optInt("totalMinutes", 30)),
                    object.optString("difficulty", "Trung bình").trim(),
                    object.optString("category", "Món gia đình").trim(),
                    object.optString("serving", "2 người").trim(),
                    object.optString("calories", "").trim(),
                    object.optString("cost", "").trim(),
                    object.optString("imageUrl", "").trim(),
                    ingredients,
                    steps);
        }

        private static List<String> readStringArray(JSONArray rows) {
            List<String> values = new ArrayList<>();
            if (rows == null) {
                return values;
            }
            for (int index = 0; index < rows.length(); index++) {
                String value = rows.optString(index, "").trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
            return values;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getTotalMinutes() { return totalMinutes; }
        public String getDifficulty() { return difficulty; }
        public String getCategory() { return category; }
        public String getServing() { return serving; }
        public String getCalories() { return calories; }
        public String getCost() { return cost; }
        public String getImageUrl() { return imageUrl; }
        public List<String> getIngredients() { return new ArrayList<>(ingredients); }
        public List<String> getSteps() { return new ArrayList<>(steps); }
    }
}
