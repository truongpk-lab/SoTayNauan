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
        addMeat(ingredients, now);
        addSeafood(ingredients, now);
        addProtein(ingredients, now);
        addGrainAndNoodles(ingredients, now);
        addVegetables(ingredients, now);
        addFruits(ingredients, now);
        addSpices(ingredients, now);
        addSaucesAndOils(ingredients, now);
        addLiquidsAndFrozen(ingredients, now);
        return ingredients;
    }

    private void addMeat(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("pork", "Thịt heo", "meat", "g", "[\"heo\",\"thịt lợn\",\"pork\"]", now));
        ingredients.add(ingredient("pork_belly", "Thịt ba chỉ", "meat", "g", "[\"ba chỉ\",\"ba rọi\"]", now));
        ingredients.add(ingredient("pork_rib", "Sườn heo", "meat", "g", "[\"sườn non\",\"sườn\"]", now));
        ingredients.add(ingredient("ground_pork", "Thịt heo xay", "meat", "g", "[\"thịt xay\",\"heo xay\"]", now));
        ingredients.add(ingredient("beef", "Thịt bò", "meat", "g", "[\"bò\",\"beef\"]", now));
        ingredients.add(ingredient("chicken", "Thịt gà", "meat", "g", "[\"gà\",\"chicken\"]", now));
        ingredients.add(ingredient("chicken_breast", "Ức gà", "meat", "g", "[\"ức gà phi lê\"]", now));
        ingredients.add(ingredient("chicken_thigh", "Đùi gà", "meat", "piece", "[\"má đùi gà\",\"đùi tỏi gà\"]", now));
    }

    private void addSeafood(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("shrimp", "Tôm", "seafood", "g", "[\"tôm tươi\",\"shrimp\"]", now));
        ingredients.add(ingredient("fish", "Cá", "seafood", "g", "[\"cá phi lê\",\"fish\"]", now));
        ingredients.add(ingredient("squid", "Mực", "seafood", "g", "[\"mực ống\",\"mực tươi\"]", now));
        ingredients.add(ingredient("crab", "Cua", "seafood", "piece", "[\"cua biển\",\"cua đồng\"]", now));
        ingredients.add(ingredient("clam", "Nghêu", "seafood", "g", "[\"ngao\",\"clam\"]", now));
    }

    private void addProtein(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("egg", "Trứng", "protein", "piece", "[\"trứng gà\",\"trứng vịt\"]", now));
        ingredients.add(ingredient("tofu", "Đậu hũ", "protein", "piece", "[\"đậu phụ\",\"tàu hũ\"]", now));
        ingredients.add(ingredient("sausage", "Xúc xích", "protein", "piece", "[\"lạp xưởng\"]", now));
    }

    private void addGrainAndNoodles(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("rice", "Gạo", "grain", "g", "[\"cơm\",\"rice\"]", now));
        ingredients.add(ingredient("sticky_rice", "Gạo nếp", "grain", "g", "[\"nếp\"]", now));
        ingredients.add(ingredient("bread", "Bánh mì", "grain", "piece", "[\"ổ bánh mì\"]", now));
        ingredients.add(ingredient("noodle", "Mì", "noodle", "g", "[\"mì gói\",\"mì trứng\"]", now));
        ingredients.add(ingredient("rice_noodle", "Bún", "noodle", "g", "[\"bún tươi\"]", now));
        ingredients.add(ingredient("pho_noodle", "Bánh phở", "noodle", "g", "[\"phở\",\"bánh phở tươi\"]", now));
        ingredients.add(ingredient("flour", "Bột mì", "grain", "g", "[\"flour\"]", now));
    }

    private void addVegetables(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("green_onion", "Hành lá", "vegetable", "g", "[\"hành\"]", now));
        ingredients.add(ingredient("cilantro", "Ngò rí", "vegetable", "g", "[\"rau mùi\",\"ngò\"]", now));
        ingredients.add(ingredient("laksa_leaf", "Rau răm", "vegetable", "g", "[]", now));
        ingredients.add(ingredient("morning_glory", "Rau muống", "vegetable", "g", "[]", now));
        ingredients.add(ingredient("cabbage", "Bắp cải", "vegetable", "g", "[\"cải bắp\"]", now));
        ingredients.add(ingredient("bok_choy", "Cải thìa", "vegetable", "g", "[\"cải chíp\"]", now));
        ingredients.add(ingredient("tomato", "Cà chua", "vegetable", "piece", "[]", now));
        ingredients.add(ingredient("cucumber", "Dưa leo", "vegetable", "piece", "[\"dưa chuột\"]", now));
        ingredients.add(ingredient("carrot", "Cà rốt", "vegetable", "g", "[]", now));
        ingredients.add(ingredient("potato", "Khoai tây", "vegetable", "g", "[]", now));
        ingredients.add(ingredient("onion", "Hành tây", "vegetable", "piece", "[]", now));
        ingredients.add(ingredient("shallot", "Hành tím", "vegetable", "piece", "[\"củ hành tím\"]", now));
        ingredients.add(ingredient("garlic", "Tỏi", "spice", "g", "[\"tỏi băm\"]", now));
        ingredients.add(ingredient("ginger", "Gừng", "spice", "g", "[]", now));
        ingredients.add(ingredient("lemongrass", "Sả", "spice", "piece", "[\"cây sả\"]", now));
        ingredients.add(ingredient("chili", "Ớt", "spice", "piece", "[\"ớt tươi\"]", now));
        ingredients.add(ingredient("mushroom", "Nấm", "vegetable", "g", "[\"nấm rơm\",\"nấm hương\",\"nấm đông cô\"]", now));
    }

    private void addFruits(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("lime", "Chanh", "fruit", "piece", "[\"lime\"]", now));
        ingredients.add(ingredient("orange", "Cam", "fruit", "piece", "[\"cam tươi\"]", now));
        ingredients.add(ingredient("pineapple", "Thơm", "fruit", "g", "[\"dứa\",\"khóm\"]", now));
        ingredients.add(ingredient("banana", "Chuối", "fruit", "piece", "[]", now));
        ingredients.add(ingredient("avocado", "Bơ", "fruit", "piece", "[\"trái bơ\"]", now));
    }

    private void addSpices(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("salt", "Muối", "spice", "g", "[]", now));
        ingredients.add(ingredient("sugar", "Đường", "spice", "g", "[\"đường cát\",\"đường phèn\"]", now));
        ingredients.add(ingredient("pepper", "Tiêu", "spice", "g", "[\"hạt tiêu\",\"tiêu xay\"]", now));
        ingredients.add(ingredient("monosodium_glutamate", "Bột ngọt", "spice", "g", "[\"mì chính\"]", now));
        ingredients.add(ingredient("seasoning_powder", "Hạt nêm", "spice", "g", "[\"bột nêm\"]", now));
        ingredients.add(ingredient("five_spice", "Ngũ vị hương", "spice", "g", "[]", now));
        ingredients.add(ingredient("curry_powder", "Bột cà ri", "spice", "g", "[]", now));
        ingredients.add(ingredient("turmeric_powder", "Bột nghệ", "spice", "g", "[]", now));
        ingredients.add(ingredient("chili_powder", "Ớt bột", "spice", "g", "[]", now));
        ingredients.add(ingredient("honey", "Mật ong", "spice", "ml", "[]", now));
    }

    private void addSaucesAndOils(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("fish_sauce", "Nước mắm", "sauce", "ml", "[\"mắm\"]", now));
        ingredients.add(ingredient("soy_sauce", "Nước tương", "sauce", "ml", "[\"xì dầu\"]", now));
        ingredients.add(ingredient("oyster_sauce", "Dầu hào", "sauce", "ml", "[]", now));
        ingredients.add(ingredient("chili_sauce", "Tương ớt", "sauce", "ml", "[]", now));
        ingredients.add(ingredient("tomato_sauce", "Tương cà", "sauce", "ml", "[]", now));
        ingredients.add(ingredient("satay", "Sa tế", "sauce", "g", "[]", now));
        ingredients.add(ingredient("cooking_oil", "Dầu ăn", "oil", "ml", "[\"dầu\"]", now));
        ingredients.add(ingredient("sesame_oil", "Dầu mè", "oil", "ml", "[]", now));
        ingredients.add(ingredient("vinegar", "Giấm", "sauce", "ml", "[\"dấm\"]", now));
    }

    private void addLiquidsAndFrozen(List<IngredientEntity> ingredients, long now) {
        ingredients.add(ingredient("water", "Nước lọc", "liquid", "ml", "[\"nước\"]", now));
        ingredients.add(ingredient("coconut_water", "Nước dừa", "liquid", "ml", "[]", now));
        ingredients.add(ingredient("milk", "Sữa tươi", "dairy", "ml", "[\"sữa\"]", now));
        ingredients.add(ingredient("condensed_milk", "Sữa đặc", "dairy", "ml", "[]", now));
        ingredients.add(ingredient("ice_cube", "Đá viên", "frozen", "piece", "[\"đá lạnh\"]", now));
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
