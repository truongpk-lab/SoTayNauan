package com.sotaynauan.ai.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.model.Recipe;

import java.util.Locale;

public final class RecipeImageResolver {
    private static final int FALLBACK_DRAWABLE = R.drawable.img_gallery;

    private RecipeImageResolver() {
        // Utility class.
    }

    public static int resolve(Context context, Recipe recipe) {
        if (recipe == null) {
            return FALLBACK_DRAWABLE;
        }
        return resolve(context, recipe.getImageName());
    }

    public static void apply(ImageView imageView, Recipe recipe) {
        if (imageView == null) {
            return;
        }
        String imageName = recipe == null ? "" : recipe.getImageName();
        if (isUri(imageName)) {
            imageView.setImageURI(Uri.parse(imageName));
            return;
        }
        imageView.setImageResource(resolve(imageView.getContext(), imageName));
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

    public static boolean isUri(String imageName) {
        return imageName != null
                && (imageName.startsWith("content://") || imageName.startsWith("file://"));
    }

    private static String sanitizeDrawableName(String imageName) {
        String normalized = imageName.trim().toLowerCase(Locale.US);
        normalized = normalized.replaceAll("[^a-z0-9_]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "");
        return normalized;
    }
}
