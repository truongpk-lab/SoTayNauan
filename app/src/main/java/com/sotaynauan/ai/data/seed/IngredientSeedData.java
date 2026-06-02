package com.sotaynauan.ai.data.seed;

import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.util.UnitConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IngredientSeedData {
    public List<IngredientEntity> createIngredients() {
        long now = System.currentTimeMillis();
        List<IngredientEntity> ingredients = new ArrayList<>();
        ingredients.add(ingredient("pork", "Thịt heo", "meat", "g", "[\"heo\",\"thịt lợn\",\"pork\"]", now));
        ingredients.add(ingredient("chicken", "Thịt gà", "meat", "g", "[\"gà\",\"chicken\"]", now));
        ingredients.add(ingredient("egg", "Trứng", "protein", "piece", "[\"trứng gà\",\"trứng vịt\"]", now));
        ingredients.add(ingredient("rice", "Gạo", "grain", "g", "[\"cơm\",\"rice\"]", now));
        ingredients.add(ingredient("green_onion", "Hành lá", "vegetable", "g", "[\"hành\"]", now));
        ingredients.add(ingredient("garlic", "Tỏi", "spice", "g", "[\"tỏi băm\"]", now));
        ingredients.add(ingredient("fish_sauce", "Nước mắm", "sauce", "ml", "[\"mắm\"]", now));
        ingredients.add(ingredient("cooking_oil", "Dầu ăn", "oil", "ml", "[\"dầu\"]", now));
        ingredients.add(ingredient("salt", "Muối", "spice", "g", "[]", now));
        ingredients.add(ingredient("sugar", "Đường", "spice", "g", "[\"đường cát\",\"đường phèn\"]", now));
        return ingredients;
    }

    public IngredientEntity ingredient(String id, String name, String category, String baseUnit,
                                       String aliasesJson, long now) {
        IngredientEntity entity = new IngredientEntity();
        entity.id = id;
        entity.name = name;
        entity.normalizedName = normalizeName(name);
        entity.category = category;
        entity.baseUnit = baseUnit;
        entity.aliasesJson = aliasesJson;
        entity.isActive = true;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public static String normalizeName(String value) {
        return UnitConverter.removeVietnameseAccent(value)
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
