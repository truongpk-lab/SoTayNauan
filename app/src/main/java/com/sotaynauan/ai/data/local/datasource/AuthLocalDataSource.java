package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;

import com.sotaynauan.ai.data.model.AuthUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AuthLocalDataSource {
    private static final String PREFS_NAME = "so_tay_nau_an_auth";
    private static final String KEY_USERS = "users";

    private final SharedPreferences preferences;

    public AuthLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public AuthUser findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        for (AuthUser user : getUsers()) {
            if (user.getEmail().equals(normalizedEmail)) {
                return user;
            }
        }
        return null;
    }

    public AuthUser findUserById(String userId) {
        String safeUserId = userId == null ? "" : userId;
        for (AuthUser user : getUsers()) {
            if (user.getUserId().equals(safeUserId)) {
                return user;
            }
        }
        return null;
    }

    public void saveUser(AuthUser newUser) {
        List<AuthUser> users = getUsers();
        List<AuthUser> updatedUsers = new ArrayList<>();
        boolean replaced = false;
        for (AuthUser user : users) {
            if (user.getEmail().equals(newUser.getEmail())) {
                updatedUsers.add(newUser);
                replaced = true;
            } else {
                updatedUsers.add(user);
            }
        }
        if (!replaced) {
            updatedUsers.add(newUser);
        }
        saveUsers(updatedUsers);
    }

    private List<AuthUser> getUsers() {
        List<AuthUser> users = new ArrayList<>();
        String rawUsers = preferences.getString(KEY_USERS, "[]");
        try {
            JSONArray array = new JSONArray(rawUsers);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.getJSONObject(index);
                users.add(new AuthUser(
                        item.optString("userId"),
                        item.optString("email"),
                        item.optString("displayName"),
                        item.optString("passwordHash"),
                        item.optLong("createdAtMillis")
                ));
            }
        } catch (JSONException ignored) {
            preferences.edit().putString(KEY_USERS, "[]").apply();
        }
        return users;
    }

    private void saveUsers(List<AuthUser> users) {
        JSONArray array = new JSONArray();
        for (AuthUser user : users) {
            JSONObject item = new JSONObject();
            try {
                item.put("userId", user.getUserId());
                item.put("email", user.getEmail());
                item.put("displayName", user.getDisplayName());
                item.put("passwordHash", user.getPasswordHash());
                item.put("createdAtMillis", user.getCreatedAtMillis());
                array.put(item);
            } catch (JSONException ignored) {
                // JSONObject only receives primitive values here.
            }
        }
        preferences.edit().putString(KEY_USERS, array.toString()).apply();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
