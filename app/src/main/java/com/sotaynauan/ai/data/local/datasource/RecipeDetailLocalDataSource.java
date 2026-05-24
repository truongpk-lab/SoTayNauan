package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailLocalDataSource {
    private static final String PREFS_NAME = "ai_chef_state";
    private static final String KEY_FAVORITE_RECIPE_IDS = "favorite_recipe_ids";
    private static final String KEY_CHECKED_PREFIX = "recipe_checked_";
    private static final String ITEM_SEPARATOR = "\n";

    private final SharedPreferences preferences;

    public RecipeDetailLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFavoriteRecipe(long recipeId) {
        return getFavoriteRecipeIds().contains(recipeId);
    }

    public boolean toggleFavoriteRecipe(long recipeId) {
        List<Long> ids = getFavoriteRecipeIds();
        boolean nextFavorite;
        if (ids.contains(recipeId)) {
            ids.remove(recipeId);
            nextFavorite = false;
        } else {
            ids.add(recipeId);
            nextFavorite = true;
        }
        preferences.edit()
                .putString(KEY_FAVORITE_RECIPE_IDS, encodeRecipeIds(ids))
                .apply();
        return nextFavorite;
    }

    public List<String> getCheckedIngredients(long recipeId) {
        String raw = preferences.getString(KEY_CHECKED_PREFIX + recipeId, "");
        List<String> ingredients = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return ingredients;
        }
        String[] parts = raw.split(ITEM_SEPARATOR);
        for (String part : parts) {
            String value = decode(part).trim();
            if (!value.isEmpty()) {
                ingredients.add(value);
            }
        }
        return ingredients;
    }

    public List<String> toggleCheckedIngredient(long recipeId, String ingredient) {
        List<String> checked = getCheckedIngredients(recipeId);
        boolean removed = false;
        for (int index = 0; index < checked.size(); index++) {
            if (checked.get(index).equalsIgnoreCase(ingredient)) {
                checked.remove(index);
                removed = true;
                break;
            }
        }
        if (!removed) {
            checked.add(ingredient);
        }
        preferences.edit()
                .putString(KEY_CHECKED_PREFIX + recipeId, encodeStrings(checked))
                .apply();
        return checked;
    }

    public List<Long> getFavoriteRecipeIds() {
        String raw = preferences.getString(KEY_FAVORITE_RECIPE_IDS, "");
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return ids;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // Skip old or corrupt ids so the local favorite list keeps working.
            }
        }
        return ids;
    }

    private String encodeRecipeIds(List<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(id);
        }
        return builder.toString();
    }

    private String encodeStrings(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(ITEM_SEPARATOR);
            }
            builder.append(encode(value));
        }
        return builder.toString();
    }

    private String encode(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.encodeToString(safeValue.getBytes(), Base64.NO_WRAP);
    }

    private String decode(String value) {
        try {
            return new String(Base64.decode(value, Base64.NO_WRAP));
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }
}
