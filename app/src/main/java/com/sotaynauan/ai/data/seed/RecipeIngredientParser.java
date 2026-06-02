package com.sotaynauan.ai.data.seed;

import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.local.entity.RecipeIngredientEntity;
import com.sotaynauan.ai.util.UnitConverter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeIngredientParser {
    private static final Pattern INGREDIENT_PATTERN = Pattern.compile(
            "^(.+?)(?:\\s*:\\s*|\\s+)(\\d+(?:[\\.,]\\d+)?|\\d+\\s*/\\s*\\d+)\\s*([^\\d]+?)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_UNIT_PATTERN = Pattern.compile(
            "^(\\d+(?:[\\.,]\\d+)?|\\d+\\s*/\\s*\\d+)\\s*([^\\d]+?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final UnitConverter unitConverter = new UnitConverter();

    public ParsedIngredient parse(long recipeId, String raw, int sortOrder) {
        String value = raw == null ? "" : raw.trim();
        String name = value;
        double amount = 1d;
        String unit = "piece";
        int colonIndex = value.indexOf(':');
        if (colonIndex > 0 && colonIndex < value.length() - 1) {
            name = value.substring(0, colonIndex).trim();
            Matcher amountMatcher = AMOUNT_UNIT_PATTERN.matcher(value.substring(colonIndex + 1).trim());
            if (amountMatcher.matches()) {
                amount = parseAmount(amountMatcher.group(1));
                unit = amountMatcher.group(2).trim();
            }
        } else {
            Matcher matcher = INGREDIENT_PATTERN.matcher(value);
            if (matcher.matches()) {
                name = matcher.group(1).trim();
                amount = parseAmount(matcher.group(2));
                unit = matcher.group(3).trim();
            }
        }
        name = removePrefix(name);
        String baseUnit = unitConverter.baseUnitFor(unit);
        double baseAmount = unitConverter.toBase(amount, unit, baseUnit);
        IngredientEntity ingredient = createIngredient(name, baseUnit);
        RecipeIngredientEntity recipeIngredient = new RecipeIngredientEntity();
        recipeIngredient.recipeId = recipeId;
        recipeIngredient.ingredientId = ingredient.id;
        recipeIngredient.amount = baseAmount;
        recipeIngredient.unit = baseUnit;
        recipeIngredient.isOptional = false;
        recipeIngredient.note = value;
        recipeIngredient.sortOrder = sortOrder;
        return new ParsedIngredient(ingredient, recipeIngredient);
    }

    private IngredientEntity createIngredient(String name, String baseUnit) {
        long now = System.currentTimeMillis();
        IngredientSeedData seedData = new IngredientSeedData();
        String safeName = name == null || name.trim().isEmpty() ? "Nguyên liệu" : name.trim();
        String normalized = IngredientSeedData.normalizeName(safeName);
        IngredientEntity entity = seedData.ingredient(slug(normalized), safeName,
                inferCategory(normalized), baseUnit, "[]", now);
        return entity;
    }

    private String removePrefix(String name) {
        String value = name == null ? "" : name.trim();
        int dashIndex = value.indexOf(" - ");
        if (dashIndex >= 0 && dashIndex < value.length() - 3) {
            return value.substring(dashIndex + 3).trim();
        }
        return value;
    }

    private String inferCategory(String normalizedName) {
        if (normalizedName.contains("thit") || normalizedName.contains("bo")
                || normalizedName.contains("ga") || normalizedName.contains("ca")
                || normalizedName.contains("tom") || normalizedName.contains("muc")
                || normalizedName.contains("trung")) {
            return "meat";
        }
        if (normalizedName.contains("rau") || normalizedName.contains("hanh")
                || normalizedName.contains("ca chua") || normalizedName.contains("dua")
                || normalizedName.contains("gia") || normalizedName.contains("nam")) {
            return "vegetable";
        }
        if (normalizedName.contains("nuoc mam") || normalizedName.contains("dau")
                || normalizedName.contains("muoi") || normalizedName.contains("duong")
                || normalizedName.contains("tieu")) {
            return "spice";
        }
        return "other";
    }

    private double parseAmount(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.contains("/")) {
            String[] parts = safeValue.split("/");
            if (parts.length == 2) {
                try {
                    return Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException exception) {
                    return 1d;
                }
            }
        }
        try {
            return Double.parseDouble(safeValue.replace(",", "."));
        } catch (NumberFormatException exception) {
            return 1d;
        }
    }

    private String slug(String normalizedName) {
        String slug = normalizedName.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "ingredient" : slug;
    }

    public static class ParsedIngredient {
        public final IngredientEntity ingredient;
        public final RecipeIngredientEntity recipeIngredient;

        private ParsedIngredient(IngredientEntity ingredient, RecipeIngredientEntity recipeIngredient) {
            this.ingredient = ingredient;
            this.recipeIngredient = recipeIngredient;
        }
    }
}
