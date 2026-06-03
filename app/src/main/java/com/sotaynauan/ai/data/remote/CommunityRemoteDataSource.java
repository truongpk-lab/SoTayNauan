package com.sotaynauan.ai.data.remote;

import com.sotaynauan.ai.data.model.CommunityComment;
import com.sotaynauan.ai.data.model.CommunityFriend;
import com.sotaynauan.ai.data.model.CommunityShare;
import com.sotaynauan.ai.data.model.CommunityState;
import com.sotaynauan.ai.data.model.Recipe;

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
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CommunityRemoteDataSource {
    private static final int CONNECT_TIMEOUT_MS = 3500;
    private static final int READ_TIMEOUT_MS = 5000;

    private final String baseUrl;
    private final String userId;
    private final String displayName;
    private final String email;

    public CommunityRemoteDataSource(String baseUrl, String userId, String displayName, String email) {
        String safeBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (safeBaseUrl.endsWith("/")) {
            safeBaseUrl = safeBaseUrl.substring(0, safeBaseUrl.length() - 1);
        }
        this.baseUrl = safeBaseUrl;
        this.userId = cleanOrFallback(userId, "guest-local");
        this.displayName = cleanOrFallback(displayName, "Khach bep nha");
        this.email = cleanOrFallback(email, this.userId + "@local");
    }

    public boolean isConfigured() {
        return !baseUrl.isEmpty();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public CommunityState loadCommunity(String query, String qrPayload) throws IOException, JSONException {
        JSONObject response = request(() -> getJson("/api/community/state"
                + "?userId=" + encode(userId)
                + "&name=" + encode(displayName)
                + "&email=" + encode(email)
                + "&query=" + encode(query)));
        return parseState(response, qrPayload);
    }

    public CommunityState sendInvite(String targetUserId, String targetName, String targetEmail,
                                     String qrPayload) throws IOException, JSONException {
        JSONObject response = request(() -> postJson("/api/community/invites/send", baseUserBody()
                .put("targetUserId", cleanOrFallback(targetUserId, ""))
                .put("targetName", cleanOrFallback(targetName, "Bạn bếp nhà"))
                .put("targetEmail", cleanOrFallback(targetEmail, ""))));
        return parseState(response, qrPayload);
    }

    public CommunityState acceptInvite(String friendId, String qrPayload)
            throws IOException, JSONException {
        JSONObject response = request(() -> postJson("/api/community/invites/accept", baseUserBody()
                .put("friendId", cleanOrFallback(friendId, ""))));
        return parseState(response, qrPayload);
    }

    public CommunityState shareRecipe(String friendId, Recipe recipe, String qrPayload)
            throws IOException, JSONException {
        String recipeName = recipe == null ? "Mâm cơm bếp nhà" : recipe.getName();
        long recipeId = recipe == null ? 0L : recipe.getId();
        JSONObject response = request(() -> postJson("/api/community/shares", baseUserBody()
                .put("friendId", cleanOrFallback(friendId, ""))
                .put("recipeId", recipeId)
                .put("recipeName", cleanOrFallback(recipeName, "Mâm cơm bếp nhà"))
                .put("message", "Bạn vừa chia sẻ công thức " + recipeName + ".")));
        return parseState(response, qrPayload);
    }

    public CommunityState toggleLike(String shareId, String qrPayload)
            throws IOException, JSONException {
        JSONObject response = request(() -> postJson("/api/community/shares/"
                + encodePath(shareId) + "/like", baseUserBody()));
        return parseState(response, qrPayload);
    }

    public CommunityState toggleSave(String shareId, String qrPayload)
            throws IOException, JSONException {
        JSONObject response = request(() -> postJson("/api/community/shares/"
                + encodePath(shareId) + "/save", baseUserBody()));
        return parseState(response, qrPayload);
    }

    public CommunityState addComment(String shareId, String comment, String qrPayload)
            throws IOException, JSONException {
        JSONObject response = request(() -> postJson("/api/community/shares/"
                + encodePath(shareId) + "/comment", baseUserBody()
                .put("comment", cleanOrFallback(comment, ""))));
        return parseState(response, qrPayload);
    }

    private JSONObject baseUserBody() throws JSONException {
        return new JSONObject()
                .put("userId", userId)
                .put("name", displayName)
                .put("email", email);
    }

    private JSONObject request(Callable<JSONObject> callable) throws IOException, JSONException {
        if (!isConfigured()) {
            throw new IOException("Community backend chưa được cấu hình.");
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<JSONObject> future = executor.submit(callable);
        try {
            return future.get(READ_TIMEOUT_MS + 1000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new IOException("Community backend phản hồi quá chậm.");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof JSONException) {
                throw (JSONException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException(cause == null ? "Community backend lỗi." : cause.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Đã hủy kết nối community backend.");
        } finally {
            executor.shutdownNow();
        }
    }

    private JSONObject getJson(String path) throws IOException, JSONException {
        return openJson(path, "GET", null);
    }

    private JSONObject postJson(String path, JSONObject body) throws IOException, JSONException {
        return openJson(path, "POST", body);
    }

    private JSONObject openJson(String path, String method, JSONObject body)
            throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (body != null) {
                connection.setDoOutput(true);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        connection.getOutputStream(), StandardCharsets.UTF_8));
                writer.write(body.toString());
                writer.flush();
                writer.close();
            }
            int statusCode = connection.getResponseCode();
            String response = readStream(statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException(response.isEmpty()
                        ? "Community backend lỗi " + statusCode
                        : response);
            }
            return new JSONObject(response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private CommunityState parseState(JSONObject response, String qrPayload) {
        return new CommunityState(
                parseFriends(response.optJSONArray("friends")),
                parseFriends(response.optJSONArray("invites")),
                parseFriends(response.optJSONArray("sentInvites")),
                parseFriends(response.optJSONArray("discoveries")),
                parseShares(response.optJSONArray("shares")),
                response.optString("statusMessage", "Cộng đồng LAN đã sẵn sàng."),
                qrPayload);
    }

    private List<CommunityFriend> parseFriends(JSONArray rows) {
        List<CommunityFriend> friends = new ArrayList<>();
        if (rows == null) {
            return friends;
        }
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            friends.add(new CommunityFriend(
                    row.optString("id", ""),
                    row.optString("name", "Bạn bếp nhà"),
                    row.optString("email", ""),
                    row.optString("note", ""),
                    row.optString("status", CommunityFriend.STATUS_DISCOVER),
                    row.optInt("sharedRecipeCount", 0)));
        }
        return friends;
    }

    private List<CommunityShare> parseShares(JSONArray rows) {
        List<CommunityShare> shares = new ArrayList<>();
        if (rows == null) {
            return shares;
        }
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            shares.add(new CommunityShare(
                    row.optString("id", ""),
                    row.optString("friendId", ""),
                    row.optString("friendName", ""),
                    row.optLong("recipeId", 0L),
                    row.optString("recipeName", "Mâm cơm bếp nhà"),
                    row.optString("message", ""),
                    row.optInt("likeCount", 0),
                    row.optInt("commentCount", 0),
                    row.optBoolean("liked", false),
                    row.optBoolean("saved", false),
                    row.optBoolean("fromMe", false),
                    parseComments(row.optJSONArray("comments"))));
        }
        return shares;
    }

    private List<CommunityComment> parseComments(JSONArray rows) {
        List<CommunityComment> comments = new ArrayList<>();
        if (rows == null) {
            return comments;
        }
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            comments.add(new CommunityComment(
                    row.optString("id", ""),
                    row.optString("shareId", ""),
                    row.optString("authorName", "Bạn bếp nhà"),
                    row.optString("body", ""),
                    row.optLong("createdAtMillis", 0L)));
        }
        return comments;
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

    private String encode(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private String encodePath(String value) throws IOException {
        return encode(value).replace("+", "%20");
    }

    private String cleanOrFallback(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? fallback : clean;
    }
}
