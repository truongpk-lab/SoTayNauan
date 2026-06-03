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
        if (containsAny(normalizedName, "nuoc loc", "nuoc dua", "nuoc soi", "nuoc lanh")) {
            return "liquid";
        }
        if (containsAny(normalizedName, "da vien", "da lanh")) {
            return "frozen";
        }
        if (containsAny(normalizedName, "nuoc mam", "nuoc tuong", "xi dau", "dau hao",
                "tuong ot", "tuong ca", "sa te", "giam", "dam")) {
            return "sauce";
        }
        if (containsAny(normalizedName, "dau an", "dau me", "dau oliu", "dau olive")) {
            return "oil";
        }
        if (containsAny(normalizedName, "muoi", "duong", "tieu", "bot ngot", "mi chinh",
                "hat nem", "bot nem", "ngu vi huong", "bot ca ri", "bot nghe", "ot bot",
                "toi", "gung", "sa", "ot", "mat ong")) {
            return "spice";
        }
        if (containsAny(normalizedName, "tom", "muc", "cua", "ngheu", "ngao", "so",
                "hen", "ca hoi", "ca thu", "ca basa", "ca loc", "ca ro", "ca ngu")
                || "ca".equals(normalizedName)) {
            return "seafood";
        }
        if (containsAny(normalizedName, "thit", "bo", "heo", "lon", "ga", "vit",
                "suon", "ba chi", "ba roi")) {
            return "meat";
        }
        if (containsAny(normalizedName, "trung", "dau hu", "dau phu", "tau hu", "xuc xich")) {
            return "protein";
        }
        if (containsAny(normalizedName, "gao", "nep", "bot mi", "bot gao", "com")) {
            return "grain";
        }
        if (containsAny(normalizedName, "mi", "bun", "pho", "hu tieu", "mien", "nui")) {
            return "noodle";
        }
        if (containsAny(normalizedName, "chanh", "cam", "thom", "dua", "khom", "chuoi", "bo")) {
            return "fruit";
        }
        if (containsAny(normalizedName, "rau", "hanh", "ca chua", "dua leo", "dua chuot",
                "gia", "nam", "cai", "bap cai", "ca rot", "khoai tay", "bap", "ngo")) {
            return "vegetable";
        }
        if (containsAny(normalizedName, "sua", "phomai", "pho mai", "bo lat")) {
            return "dairy";
        }
        return "other";
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
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
