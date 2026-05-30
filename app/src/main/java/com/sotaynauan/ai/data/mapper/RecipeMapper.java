package com.sotaynauan.ai.data.mapper;

import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.model.Recipe;

import java.util.ArrayList;
import java.util.List;

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
                entity.category,
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
