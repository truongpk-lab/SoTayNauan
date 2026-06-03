package com.sotaynauan.ai.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.sotaynauan.ai.R;
import com.sotaynauan.ai.data.model.Recipe;

import java.text.Normalizer;
import java.util.Locale;

public final class RecipeImageResolver {
    private static final int FALLBACK_DRAWABLE = R.drawable.img_kho_qua_nhoi_thit;

    private RecipeImageResolver() {
        // Utility class.
    }

    public static int resolve(Context context, Recipe recipe) {
        if (recipe == null) {
            return FALLBACK_DRAWABLE;
        }
        int explicitImage = resolveExplicit(context, recipe.getImageName());
        return explicitImage == 0 ? resolveByRecipe(context, recipe) : explicitImage;
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
        imageView.setImageResource(resolve(imageView.getContext(), recipe));
    }

    public static int resolve(Context context, String imageName) {
        int explicitImage = resolveExplicit(context, imageName);
        return explicitImage == 0 ? FALLBACK_DRAWABLE : explicitImage;
    }

    private static int resolveExplicit(Context context, String imageName) {
        if (context == null || imageName == null || imageName.trim().isEmpty()) {
            return 0;
        }
        String sanitizedName = sanitizeDrawableName(imageName);
        if ("img_gallery".equals(sanitizedName)) {
            return 0;
        }

        return resolveDrawable(context, sanitizedName);
    }

    private static int resolveByRecipe(Context context, Recipe recipe) {
        if (context == null || recipe == null) {
            return FALLBACK_DRAWABLE;
        }
        String text = normalize(recipe.getName() + " " + recipe.getCategory() + " "
                + recipe.getDescription());
        String drawableName = drawableNameFor(text);
        int imageResourceId = resolveDrawable(context, drawableName);
        return imageResourceId == 0 ? FALLBACK_DRAWABLE : imageResourceId;
    }

    private static String drawableNameFor(String text) {
        if (containsAny(text, "kho qua", "muop dang")) {
            return "img_kho_qua_nhoi_thit";
        }
        if (containsAny(text, "canh")) {
            return "img_canh_chua";
        }
        if (containsAny(text, "lau")) {
            return "img_lau_thai";
        }
        if (containsAny(text, "kho", "rim")) {
            return "img_thit_kho_tau";
        }
        if (containsAny(text, "xao")) {
            return "img_mi_xao_bo";
        }
        if (containsAny(text, "chien", "ran")) {
            return "img_ga_chien_nuoc_mam";
        }
        if (containsAny(text, "nuong")) {
            return "img_bun_thit_nuong";
        }
        if (containsAny(text, "bun", "pho", "hu tieu")) {
            return "img_pho_bo";
        }
        if (containsAny(text, "com")) {
            return "img_com_tam";
        }
        if (containsAny(text, "goi", "salad")) {
            return "img_goi_cuon";
        }
        if (containsAny(text, "banh")) {
            return "img_banh_xeo";
        }
        if (containsAny(text, "che", "trang mieng", "flan")) {
            return "img_banh_flan";
        }
        if (containsAny(text, "nuoc uong", "sinh to", "ca phe", "cam")) {
            return "img_sinh_to_bo";
        }
        return "img_kho_qua_nhoi_thit";
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

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String safeValue = value == null ? "" : value;
        String normalized = Normalizer.normalize(safeValue, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.US).replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }
}
