package com.sotaynauan.ai.util;

import android.content.Context;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.model.Recipe;

import java.util.Locale;

public final class RecipeImageResolver {
    private static final int FALLBACK_DRAWABLE = R.drawable.cooking_step_preview;

    private RecipeImageResolver() {
        // Utility class.
    }

    public static int resolve(Context context, Recipe recipe) {
        if (recipe == null) {
            return FALLBACK_DRAWABLE;
        }
        return resolve(context, recipe.getImageName());
    }

    public static int resolve(Context context, String imageName) {
        if (context == null || imageName == null || imageName.trim().isEmpty()) {
            return FALLBACK_DRAWABLE;
        }

        String sanitizedName = sanitizeDrawableName(imageName);
        int imageResourceId = resolveDrawable(context, sanitizedName);
        return imageResourceId == 0 ? FALLBACK_DRAWABLE : imageResourceId;
    }

    private static int resolveDrawable(Context context, String drawableName) {
        if (drawableName == null || drawableName.isEmpty()) {
            return 0;
        }
        return context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
    }

    private static String sanitizeDrawableName(String imageName) {
        String normalized = imageName.trim().toLowerCase(Locale.US);
        normalized = normalized.replaceAll("[^a-z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized;
    }
}
