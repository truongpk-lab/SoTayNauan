package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.sotaynauan.ai.data.model.AiChefState;
import com.sotaynauan.ai.data.model.ConfirmedIngredient;
import com.sotaynauan.ai.data.model.IngredientConfirmState;
import com.sotaynauan.ai.data.model.IngredientInputState;

import java.util.ArrayList;
import java.util.List;

public class AiChefLocalDataSource {
    private static final String PREFS_NAME = "ai_chef_state";
    private static final String KEY_ACTIVE_FEATURE_ID = "active_feature_id";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_OPEN_COUNT = "open_count";
    private static final String KEY_INGREDIENTS = "ingredients";
    private static final String KEY_INGREDIENTS_UPDATED_AT = "ingredients_updated_at";
    private static final String KEY_INGREDIENTS_LAST_ACTION = "ingredients_last_action";
    private static final String KEY_CONFIRMED_INGREDIENTS = "confirmed_ingredients";
    private static final String KEY_CONFIRMED_UPDATED_AT = "confirmed_updated_at";
    private static final String KEY_CONFIRMED_LAST_ACTION = "confirmed_last_action";
    private static final String KEY_FAVORITE_RECIPE_IDS = "favorite_recipe_ids";
    private static final String KEY_LAST_AI_ADVICE = "last_ai_advice";
    private static final String KEY_LAST_AI_UPDATED_AT = "last_ai_updated_at";
    private static final String INGREDIENT_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "|";

    private final SharedPreferences preferences;

    public AiChefLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public AiChefState getState() {
        return new AiChefState(
                preferences.getString(KEY_ACTIVE_FEATURE_ID, ""),
                preferences.getLong(KEY_UPDATED_AT, 0L),
                preferences.getInt(KEY_OPEN_COUNT, 0));
    }

    public AiChefState selectFeature(String featureId) {
        int nextOpenCount = preferences.getInt(KEY_OPEN_COUNT, 0) + 1;
        long now = System.currentTimeMillis();
        preferences.edit()
                .putString(KEY_ACTIVE_FEATURE_ID, featureId)
                .putLong(KEY_UPDATED_AT, now)
                .putInt(KEY_OPEN_COUNT, nextOpenCount)
                .apply();
        return new AiChefState(featureId, now, nextOpenCount);
    }

    public IngredientInputState getIngredientInputState() {
        return new IngredientInputState(readIngredients(),
                preferences.getLong(KEY_INGREDIENTS_UPDATED_AT, 0L),
                preferences.getString(KEY_INGREDIENTS_LAST_ACTION,
                        "Danh sách nguyên liệu được lưu local để AI Chef dùng ở bước tiếp theo."));
    }

    public IngredientInputState saveIngredients(List<String> ingredients, String action) {
        long now = System.currentTimeMillis();
        preferences.edit()
                .putString(KEY_INGREDIENTS, encodeIngredients(ingredients))
                .putLong(KEY_INGREDIENTS_UPDATED_AT, now)
                .putString(KEY_INGREDIENTS_LAST_ACTION, action)
                .apply();
        return new IngredientInputState(ingredients, now, action);
    }

    public IngredientConfirmState getIngredientConfirmState() {
        return new IngredientConfirmState(readConfirmedIngredients(),
                preferences.getLong(KEY_CONFIRMED_UPDATED_AT, 0L),
                preferences.getString(KEY_CONFIRMED_LAST_ACTION,
                        "Kiểm tra lại nguyên liệu rồi bấm tìm món gần nhất."));
    }

    public IngredientConfirmState saveConfirmedIngredients(List<ConfirmedIngredient> ingredients,
                                                           String action) {
        long now = System.currentTimeMillis();
        preferences.edit()
                .putString(KEY_CONFIRMED_INGREDIENTS, encodeConfirmedIngredients(ingredients))
                .putLong(KEY_CONFIRMED_UPDATED_AT, now)
                .putString(KEY_CONFIRMED_LAST_ACTION, action)
                .apply();
        return new IngredientConfirmState(ingredients, now, action);
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
                // Ignore old/corrupt ids so local favorites stay readable.
            }
        }
        return ids;
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

    public String getLastAiAdvice() {
        return preferences.getString(KEY_LAST_AI_ADVICE, "");
    }

    public void saveLastAiAdvice(String advice) {
        preferences.edit()
                .putString(KEY_LAST_AI_ADVICE, advice == null ? "" : advice)
                .putLong(KEY_LAST_AI_UPDATED_AT, System.currentTimeMillis())
                .apply();
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

    private List<String> readIngredients() {
        String raw = preferences.getString(KEY_INGREDIENTS, "");
        List<String> ingredients = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return ingredients;
        }
        String[] parts = raw.split(INGREDIENT_SEPARATOR);
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                ingredients.add(value);
            }
        }
        return ingredients;
    }

    private String encodeIngredients(List<String> ingredients) {
        StringBuilder builder = new StringBuilder();
        for (String ingredient : ingredients) {
            if (builder.length() > 0) {
                builder.append(INGREDIENT_SEPARATOR);
            }
            builder.append(ingredient);
        }
        return builder.toString();
    }

    private List<ConfirmedIngredient> readConfirmedIngredients() {
        String raw = preferences.getString(KEY_CONFIRMED_INGREDIENTS, "");
        List<ConfirmedIngredient> ingredients = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return ingredients;
        }
        String[] rows = raw.split(INGREDIENT_SEPARATOR);
        for (String row : rows) {
            String[] parts = row.split("\\|", -1);
            if (parts.length < 5) {
                continue;
            }
            String id = decode(parts[0]);
            String name = decode(parts[1]);
            String quantity = decode(parts[2]);
            String source = decode(parts[3]);
            boolean selected = Boolean.parseBoolean(parts[4]);
            if (!name.trim().isEmpty()) {
                ingredients.add(new ConfirmedIngredient(id, name, quantity, source, selected));
            }
        }
        return ingredients;
    }

    private String encodeConfirmedIngredients(List<ConfirmedIngredient> ingredients) {
        StringBuilder builder = new StringBuilder();
        for (ConfirmedIngredient ingredient : ingredients) {
            if (builder.length() > 0) {
                builder.append(INGREDIENT_SEPARATOR);
            }
            builder.append(encode(ingredient.getId()))
                    .append(FIELD_SEPARATOR)
                    .append(encode(ingredient.getName()))
                    .append(FIELD_SEPARATOR)
                    .append(encode(ingredient.getQuantity()))
                    .append(FIELD_SEPARATOR)
                    .append(encode(ingredient.getSource()))
                    .append(FIELD_SEPARATOR)
                    .append(ingredient.isSelected());
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
