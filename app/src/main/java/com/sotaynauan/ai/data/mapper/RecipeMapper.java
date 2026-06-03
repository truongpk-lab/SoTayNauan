package com.sotaynauan.ai.data.mapper;

import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.model.Recipe;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeMapper {
    private static final String SEPARATOR = "\\|";
    private static final String JOIN_SEPARATOR = "|";

    public Recipe toModel(RecipeEntity entity) {
        return new Recipe(
                entity.id,
                entity.name,
                entity.description,
                entity.totalMinutes,
                entity.difficulty,
                resolveCategory(entity),
                nullToEmpty(entity.imageName),
                nullToEmpty(entity.serving),
                nullToEmpty(entity.calories),
                nullToEmpty(entity.cost),
                entity.colorArgb,
                entity.popularityScore,
                entity.todaySuggestion,
                entity.friendName,
                entity.friendNote,
                split(entity.ingredients),
                split(entity.steps)
        );
    }

    public List<Recipe> toModels(List<RecipeEntity> entities) {
        List<Recipe> recipes = new ArrayList<>();
        for (RecipeEntity entity : entities) {
            recipes.add(toModel(entity));
        }
        return recipes;
    }

    public RecipeEntity toEntity(String name, String description, int totalMinutes, String difficulty,
                                 String category, String imageName, String serving, String calories,
                                 String cost, int colorArgb, int popularityScore,
                                 boolean todaySuggestion, String friendName, String friendNote,
                                 List<String> ingredients, List<String> steps) {
        RecipeEntity entity = new RecipeEntity();
        entity.name = name;
        entity.description = description;
        entity.totalMinutes = totalMinutes;
        entity.difficulty = difficulty;
        entity.category = category;
        entity.imageName = imageName;
        entity.serving = serving;
        entity.calories = calories;
        entity.cost = cost;
        entity.colorArgb = colorArgb;
        entity.popularityScore = popularityScore;
        entity.todaySuggestion = todaySuggestion;
        entity.friendName = friendName;
        entity.friendNote = friendNote;
        entity.ingredients = join(ingredients);
        entity.steps = join(steps);
        return entity;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String resolveCategory(RecipeEntity entity) {
        String category = nullToEmpty(entity.category).trim();
        if (!category.isEmpty() && !"Công thức của tôi".equalsIgnoreCase(category)) {
            return category;
        }
        String inferred = inferCategory(entity);
        return inferred.isEmpty() ? category : inferred;
    }

    private String inferCategory(RecipeEntity entity) {
        String text = normalize(nullToEmpty(entity.name) + " "
                + nullToEmpty(entity.description) + " "
                + nullToEmpty(entity.ingredients));
        if (containsAny(text, "canh", "kho qua nhoi thit")) {
            return "Canh";
        }
        if (containsAny(text, "lau")) {
            return "Lẩu";
        }
        if (containsAny(text, "kho", "rim")) {
            return "Món kho";
        }
        if (containsAny(text, "xao")) {
            return "Món xào";
        }
        if (containsAny(text, "chien", "ran")) {
            return "Món chiên";
        }
        if (containsAny(text, "nuong")) {
            return "Món nướng";
        }
        if (containsAny(text, "bun", "pho", "hu tieu", "mi ")) {
            return "Món nước";
        }
        if (containsAny(text, "com")) {
            return "Món cơm";
        }
        if (containsAny(text, "goi", "salad")) {
            return "Gỏi & Salad";
        }
        if (containsAny(text, "banh")) {
            return "Món bánh";
        }
        return "";
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String safeValue = value == null ? "" : value;
        String normalized = Normalizer.normalize(safeValue, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.US).replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }

    private List<String> split(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }
        String[] parts = raw.split(SEPARATOR);
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(JOIN_SEPARATOR);
            }
            builder.append(values.get(index).replace(JOIN_SEPARATOR, " "));
        }
        return builder.toString();
    }
}
