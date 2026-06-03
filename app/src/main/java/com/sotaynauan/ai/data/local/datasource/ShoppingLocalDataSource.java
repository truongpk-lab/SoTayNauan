package com.sotaynauan.ai.data.local.datasource;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.sotaynauan.ai.data.local.dao.ShoppingItemDao;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.entity.ShoppingItemEntity;
import com.sotaynauan.ai.data.model.ShoppingPlanState;
import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.model.ShoppingPlanItem;
import com.sotaynauan.ai.data.repository.CookingPreparationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShoppingLocalDataSource {
    private static final Pattern TRAILING_QUANTITY_PATTERN = Pattern.compile(
            "(.+?)\\s+(\\d+)\\s*(kg|g|gram|l|ml|quả|trái|củ|nhánh|cây|lá|gói|bịch|chai|chai nhỏ|lọ|hũ|hộp|lon|ly|mớ|phần|bát|chén|nhúm)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_QUANTITY_PATTERN = Pattern.compile(
            "^(\\d+)\\s*(kg|g|gram|l|ml|quả|trái|củ|nhánh|cây|lá|gói|bịch|chai|chai nhỏ|lọ|hũ|hộp|lon|ly|mớ|phần|bát|chén|nhúm)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final String PREFS_NAME = "shopping_plan_state";
    private static final String KEY_RECIPE_ID = "recipe_id";
    private static final String KEY_RECIPE_NAME = "recipe_name";
    private static final String KEY_ITEMS = "items_v2";
    private static final String KEY_MISSING_INGREDIENTS = "missing_ingredients";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_PLAN_RECIPE_ID = "plan_recipe_id";
    private static final String KEY_PLAN_RECIPE_NAME = "plan_recipe_name";
    private static final String KEY_PLAN_ITEMS = "plan_items_v2";
    private static final String KEY_PLAN_MISSING_INGREDIENTS = "plan_missing_ingredients";
    private static final String KEY_PLAN_UPDATED_AT = "plan_updated_at";
    private static final String KEY_LIST_RECIPE_ID = "list_recipe_id";
    private static final String KEY_LIST_RECIPE_NAME = "list_recipe_name";
    private static final String KEY_LIST_ITEMS = "list_items_v2";
    private static final String KEY_LIST_MISSING_INGREDIENTS = "list_missing_ingredients";
    private static final String KEY_LIST_UPDATED_AT = "list_updated_at";
    private static final String KEY_SPLIT_PREFIX = "split_remainder_";
    private static final String INGREDIENT_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "\t";

    private final SharedPreferences preferences;
    private final AppDatabase database;
    private final ShoppingItemDao shoppingItemDao;

    public ShoppingLocalDataSource(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        database = AppDatabase.getInstance(context);
        shoppingItemDao = database.shoppingItemDao();
    }

    public ShoppingPlanState getCurrentPlan() {
        List<ShoppingItemEntity> entities = shoppingItemDao.getItemsByCommitted(false);
        List<ShoppingPlanItem> items = toPlanItemsFromEntities(entities);
        if (items.isEmpty()) {
            items = toPlanItems(readLegacyIngredients());
            if (!items.isEmpty()) {
                return saveItems(
                        getRecipeId(false, entities),
                        getRecipeName(false, entities),
                        items,
                        "Đã chuyển danh sách đi chợ cũ sang Room local.");
            }
        }
        return new ShoppingPlanState(
                getRecipeId(false, entities),
                getRecipeName(false, entities),
                items,
                getUpdatedAt(false, entities),
                "");
    }

    public ShoppingPlanState getShoppingList() {
        List<ShoppingItemEntity> entities = shoppingItemDao.getItemsByCommitted(true);
        return new ShoppingPlanState(
                getRecipeId(true, entities),
                getRecipeName(true, entities),
                toPlanItemsFromEntities(entities),
                getUpdatedAt(true, entities),
                "");
    }

    public ShoppingPlanState savePlan(long recipeId, String recipeName, List<String> missingIngredients) {
        ShoppingPlanState state = getCurrentPlan();
        List<ShoppingPlanItem> mergedItems = new ArrayList<>(state.getItems());
        List<ShoppingPlanItem> nextItems = toPlanItems(missingIngredients);
        for (ShoppingPlanItem item : nextItems) {
            addOrMergeItem(mergedItems, item);
        }
        String nextRecipeName = appendRecipeName(state.getRecipeName(), recipeName);
        long nextRecipeId = state.getRecipeId() > 0L ? state.getRecipeId() : recipeId;
        return saveItems(nextRecipeId, nextRecipeName, mergedItems,
                nextItems.isEmpty()
                        ? "Món này không thiếu nguyên liệu nào."
                        : "Đã thêm nguyên liệu vào danh sách đi chợ. Item trùng tên và cùng đơn vị đã được cộng dồn.");
    }

    public ShoppingPlanState saveShoppingList(long recipeId, String recipeName, List<String> ingredients) {
        ShoppingPlanState state = getShoppingList();
        List<ShoppingPlanItem> mergedItems = new ArrayList<>(state.getItems());
        List<ShoppingPlanItem> nextItems = toPlanItems(ingredients);
        for (ShoppingPlanItem item : nextItems) {
            addOrMergeItem(mergedItems, item.withCommitted(true));
        }
        String nextRecipeName = appendRecipeName(state.getRecipeName(), recipeName);
        long nextRecipeId = state.getRecipeId() > 0L ? state.getRecipeId() : recipeId;
        return saveItems(nextRecipeId, nextRecipeName, mergedItems,
                nextItems.isEmpty()
                        ? "Món này không thiếu nguyên liệu nào."
                        : "Đã thêm nguyên liệu thiếu vào danh sách đi chợ.",
                true);
    }

    public ShoppingPlanState saveItems(long recipeId, String recipeName, List<ShoppingPlanItem> items,
                                       String statusMessage) {
        return saveItems(recipeId, recipeName, items, statusMessage, false);
    }

    public ShoppingPlanState saveItems(long recipeId, String recipeName, List<ShoppingPlanItem> items,
                                       String statusMessage, boolean committed) {
        long now = System.currentTimeMillis();
        List<ShoppingItemEntity> entities = toEntities(recipeId, recipeName, items, now, committed);
        shoppingItemDao.clearByCommitted(committed);
        shoppingItemDao.upsertAll(entities);
        SharedPreferences.Editor editor = preferences.edit()
                .putLong(keyRecipeId(committed), recipeId)
                .putString(keyRecipeName(committed), recipeName)
                .putString(keyItems(committed), encodeItems(items))
                .putString(keyMissingIngredients(committed), encodeIngredients(namesWithNeedBuyStatus(items)))
                .putLong(keyUpdatedAt(committed), now);
        if (!committed) {
            editor.putLong(KEY_RECIPE_ID, recipeId)
                    .putString(KEY_RECIPE_NAME, recipeName)
                    .putString(KEY_ITEMS, encodeItems(items))
                    .putString(KEY_MISSING_INGREDIENTS, encodeIngredients(namesWithNeedBuyStatus(items)))
                    .putLong(KEY_UPDATED_AT, now);
        }
        editor.apply();
        return new ShoppingPlanState(recipeId, recipeName, items, now, statusMessage);
    }

    private List<ShoppingItemEntity> toEntities(long recipeId, String recipeName,
                                                List<ShoppingPlanItem> items, long updatedAtMillis,
                                                boolean committed) {
        List<ShoppingItemEntity> entities = new ArrayList<>();
        for (ShoppingPlanItem item : items) {
            ShoppingItemEntity entity = new ShoppingItemEntity();
            entity.id = entityId(item.getId(), committed);
            entity.recipeId = recipeId;
            entity.recipeName = recipeName;
            entity.name = item.getName();
            entity.amount = item.getAmount();
            entity.unit = item.getUnit();
            entity.note = item.getNote();
            entity.category = item.getCategory();
            entity.status = item.getStatus().name();
            entity.committed = committed;
            entity.updatedAtMillis = updatedAtMillis;
            ShoppingItemEntity source = findSourceEntity(item.getId(), committed);
            entity.planId = source == null ? null : source.planId;
            entity.ingredientId = source == null ? null : source.ingredientId;
            entity.displayName = item.getName();
            entity.requiredAmount = item.getAmount();
            entity.boughtAmount = item.getStatus() == ShoppingItemStatus.BOUGHT
                    ? item.getAmount()
                    : source == null ? 0d : source.boughtAmount;
            entity.baseUnit = item.getUnit();
            entity.createdAt = source == null || source.createdAt <= 0L ? updatedAtMillis : source.createdAt;
            entity.updatedAt = updatedAtMillis;
            entities.add(entity);
        }
        return entities;
    }

    public ShoppingPlanState updateStatus(String itemId, ShoppingItemStatus status) {
        return updateStatus(itemId, status, false);
    }

    public ShoppingPlanState updateStatus(String itemId, ShoppingItemStatus status, boolean committed) {
        ShoppingPlanState state = committed ? getShoppingList() : getCurrentPlan();
        ShoppingItemEntity sourceEntity = findSourceEntity(itemId, committed);
        if (committed && status == ShoppingItemStatus.BOUGHT
                && sourceEntity != null
                && sourceEntity.planId != null
                && sourceEntity.ingredientId != null) {
            new CookingPreparationRepository(database).markBought(sourceEntity.planId,
                    sourceEntity.ingredientId, sourceEntity.amount, sourceEntity.unit);
        } else if (!committed
                && sourceEntity != null
                && sourceEntity.planId != null
                && sourceEntity.ingredientId != null
                && (status == ShoppingItemStatus.AT_HOME || status == ShoppingItemStatus.NEED_BUY)) {
            new CookingPreparationRepository(database).markHaveAtHome(sourceEntity.planId,
                    sourceEntity.ingredientId,
                    status == ShoppingItemStatus.AT_HOME ? sourceEntity.amount : 0,
                    sourceEntity.unit);
        }
        List<ShoppingPlanItem> nextItems = new ArrayList<>();
        ShoppingPlanItem splitRemainder = null;
        for (ShoppingPlanItem item : state.getItems()) {
            if (item.getId().equals(itemId)) {
                ShoppingPlanItem updatedItem = item.withStatus(status)
                        .withId(createItemId(item.getName(), item.getUnit(), status));
                addOrMergeItem(nextItems, updatedItem);
                int remainderAmount = consumeSplitRemainder(itemId);
                if (status == ShoppingItemStatus.NEED_BUY && remainderAmount > 0) {
                    splitRemainder = new ShoppingPlanItem(
                            createItemId(item.getName(), item.getUnit(), ShoppingItemStatus.AT_HOME),
                            item.getName(),
                            remainderAmount,
                            item.getUnit(),
                            item.getNote(),
                            item.getCategory(),
                            ShoppingItemStatus.AT_HOME);
                }
            } else {
                nextItems.add(item);
            }
        }
        if (splitRemainder != null) {
            addOrMergeItem(nextItems, splitRemainder);
        }
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                splitRemainder == null
                        ? "Đã cập nhật trạng thái nguyên liệu."
                        : "Đã tách phần cần mua và phần đã có ở nhà.",
                committed);
    }

    public ShoppingPlanState adjustQuantity(String itemId, int delta) {
        return adjustQuantity(itemId, delta, false);
    }

    public ShoppingPlanState adjustQuantity(String itemId, int delta, boolean committed) {
        ShoppingPlanState state = committed ? getShoppingList() : getCurrentPlan();
        List<ShoppingPlanItem> nextItems = new ArrayList<>();
        for (ShoppingPlanItem item : state.getItems()) {
            if (item.getId().equals(itemId)) {
                int stepDelta = delta < 0 ? -quantityStep(item) : quantityStep(item);
                int nextAmount = Math.max(1, item.getAmount() + stepDelta);
                trackSplitSelection(item, item.getAmount(), nextAmount);
                nextItems.add(item.withAmount(nextAmount));
            } else {
                nextItems.add(item);
            }
        }
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                "Đã chỉnh số lượng cho kế hoạch đi chợ.", committed);
    }

    public ShoppingPlanState addCustomItem(String name, int amount, String unit, String category) {
        return addCustomItem(name, amount, unit, category, false);
    }

    public ShoppingPlanState addCustomItem(String name, int amount, String unit, String category,
                                           boolean committed) {
        ShoppingPlanState state = committed ? getShoppingList() : getCurrentPlan();
        List<ShoppingPlanItem> nextItems = new ArrayList<>(state.getItems());
        String safeName = name == null ? "" : name.trim();
        if (safeName.isEmpty()) {
            return state;
        }
        ShoppingPlanItem newItem = new ShoppingPlanItem(
                createItemId(safeName, unit, ShoppingItemStatus.NEED_BUY),
                safeName, Math.max(1, amount), unit, "", category, ShoppingItemStatus.NEED_BUY);
        ShoppingPlanItem existing = findMergeCandidate(nextItems, newItem);
        if (existing == null) {
            nextItems.add(newItem);
        } else {
            nextItems.remove(existing);
            nextItems.add(mergeItems(existing, newItem));
        }
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                "Đã thêm nguyên liệu vào danh sách đi chợ. Nếu trùng tên và cùng đơn vị, số lượng đã được cộng dồn.",
                committed);
    }

    public ShoppingPlanState updateItem(String itemId, String name, int amount, String unit, String category) {
        return updateItem(itemId, name, amount, unit, category, false);
    }

    public ShoppingPlanState updateItem(String itemId, String name, int amount, String unit, String category,
                                        boolean committed) {
        ShoppingPlanState state = committed ? getShoppingList() : getCurrentPlan();
        List<ShoppingPlanItem> nextItems = new ArrayList<>();
        String safeName = name == null ? "" : name.trim();
        ShoppingPlanItem editedItem = null;
        for (ShoppingPlanItem item : state.getItems()) {
            if (item.getId().equals(itemId) && !safeName.isEmpty()) {
                editedItem = new ShoppingPlanItem(createItemId(safeName, unit, item.getStatus()), safeName,
                        Math.max(1, amount), unit, item.getNote(), category, item.getStatus());
            } else {
                nextItems.add(item);
            }
        }
        if (editedItem != null) {
            addOrMergeItem(nextItems, editedItem);
        }
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                "Đã sửa nguyên liệu trong danh sách đi chợ. Item trùng tên và cùng đơn vị đã được gộp.",
                committed);
    }

    public ShoppingPlanState removeItem(String itemId) {
        return removeItem(itemId, false);
    }

    public ShoppingPlanState removeItem(String itemId, boolean committed) {
        ShoppingPlanState state = committed ? getShoppingList() : getCurrentPlan();
        List<ShoppingPlanItem> nextItems = new ArrayList<>();
        for (ShoppingPlanItem item : state.getItems()) {
            if (!item.getId().equals(itemId)) {
                nextItems.add(item);
            }
        }
        shoppingItemDao.deleteById(itemId);
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                "Đã xóa nguyên liệu khỏi danh sách đi chợ.", committed);
    }

    public ShoppingPlanState finishMarketTrip() {
        ShoppingPlanState state = getShoppingList();
        List<ShoppingPlanItem> nextItems = new ArrayList<>();
        int convertedCount = 0;
        for (ShoppingPlanItem item : state.getItems()) {
            if (item.getStatus() == ShoppingItemStatus.BOUGHT) {
                convertedCount++;
                addOrMergeItem(nextItems, new ShoppingPlanItem(
                        createItemId(item.getName(), item.getUnit(), ShoppingItemStatus.AT_HOME),
                        item.getName(),
                        item.getAmount(),
                        item.getUnit(),
                        item.getNote(),
                        item.getCategory(),
                        ShoppingItemStatus.AT_HOME));
            } else {
                addOrMergeItem(nextItems, item);
            }
        }
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                convertedCount == 0
                        ? "Đã tắt chế độ đi chợ. Không có nguyên liệu đã mua cần nhập vào bếp."
                        : "Đã nhập " + convertedCount + " nguyên liệu đã mua thành Đã có ở nhà.",
                true);
    }

    public ShoppingPlanState consumeIngredientsForCookedRecipe(List<String> recipeIngredients, String recipeName) {
        ShoppingPlanState state = getShoppingList();
        List<ShoppingPlanItem> nextItems = new ArrayList<>(state.getItems());
        int consumedCount = 0;
        for (ShoppingPlanItem requiredItem : toPlanItems(recipeIngredients)) {
            ShoppingPlanItem atHomeItem = findItemByKey(nextItems,
                    createItemId(requiredItem.getName(), requiredItem.getUnit(), ShoppingItemStatus.AT_HOME));
            if (atHomeItem == null) {
                continue;
            }
            nextItems.remove(atHomeItem);
            int remainingAmount = atHomeItem.getAmount() - requiredItem.getAmount();
            if (remainingAmount > 0) {
                nextItems.add(atHomeItem.withAmount(remainingAmount));
            }
            consumedCount++;
        }
        return saveItems(state.getRecipeId(), state.getRecipeName(), nextItems,
                consumedCount == 0
                        ? "Hoàn thành món " + recipeName + ". Không tìm thấy nguyên liệu tồn nhà phù hợp để trừ."
                        : "Hoàn thành món " + recipeName + ". Đã trừ " + consumedCount
                        + " nguyên liệu khỏi mục Đã có ở nhà.",
                true);
    }

    public ShoppingPlanState commitShoppingList() {
        ShoppingPlanState planState = getCurrentPlan();
        ShoppingPlanState listState = getShoppingList();
        List<ShoppingPlanItem> committedItems = new ArrayList<>(listState.getItems());
        int movedCount = 0;
        for (ShoppingPlanItem item : planState.getItems()) {
            if (item.getStatus() == ShoppingItemStatus.SKIPPED) {
                continue;
            }
            movedCount++;
            addOrMergeItem(committedItems, item.withCommitted(true));
        }
        saveItems(planState.getRecipeId(), planState.getRecipeName(), committedItems,
                movedCount + " nguyên liệu đã chuyển vào danh sách đi chợ.", true);
        return saveItems(-1L, "", new ArrayList<>(),
                "Đã tạo danh sách đi chợ và làm mới kế hoạch cho món tiếp theo.", false);
    }

    private List<String> readLegacyIngredients() {
        String raw = preferences.getString(KEY_MISSING_INGREDIENTS, "");
        List<String> ingredients = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return ingredients;
        }
        String[] parts = raw.split(INGREDIENT_SEPARATOR);
        for (String part : parts) {
            String value = decode(part).trim();
            if (!value.isEmpty()) {
                ingredients.add(value);
            }
        }
        return ingredients;
    }

    private List<ShoppingPlanItem> toPlanItemsFromEntities(List<ShoppingItemEntity> entities) {
        List<ShoppingPlanItem> items = new ArrayList<>();
        for (ShoppingItemEntity entity : entities) {
            items.add(new ShoppingPlanItem(modelId(entity.id), entity.name, entity.amount, entity.unit,
                    entity.note, entity.category, ShoppingItemStatus.fromName(entity.status),
                    entity.committed));
        }
        return items;
    }

    private long getRecipeId(boolean committed, List<ShoppingItemEntity> entities) {
        String key = keyRecipeId(committed);
        if (preferences.contains(key)) {
            return preferences.getLong(key, -1L);
        }
        if (!entities.isEmpty() && entities.get(0).recipeId > 0L) {
            return entities.get(0).recipeId;
        }
        return committed ? -1L : preferences.getLong(KEY_RECIPE_ID, -1L);
    }

    private String getRecipeName(boolean committed, List<ShoppingItemEntity> entities) {
        String key = keyRecipeName(committed);
        if (preferences.contains(key)) {
            return preferences.getString(key, "");
        }
        if (!entities.isEmpty() && entities.get(0).recipeName != null
                && !entities.get(0).recipeName.trim().isEmpty()) {
            return entities.get(0).recipeName;
        }
        return committed ? "" : preferences.getString(KEY_RECIPE_NAME, "");
    }

    private long getUpdatedAt(boolean committed, List<ShoppingItemEntity> entities) {
        String key = keyUpdatedAt(committed);
        if (preferences.contains(key)) {
            return preferences.getLong(key, 0L);
        }
        long updatedAt = 0L;
        for (ShoppingItemEntity entity : entities) {
            updatedAt = Math.max(updatedAt, entity.updatedAtMillis);
        }
        return updatedAt > 0L || committed ? updatedAt : preferences.getLong(KEY_UPDATED_AT, 0L);
    }

    private String encodeIngredients(List<String> ingredients) {
        StringBuilder builder = new StringBuilder();
        for (String ingredient : ingredients) {
            if (builder.length() > 0) {
                builder.append(INGREDIENT_SEPARATOR);
            }
            builder.append(encode(ingredient));
        }
        return builder.toString();
    }

    private String encodeItems(List<ShoppingPlanItem> items) {
        StringBuilder builder = new StringBuilder();
        for (ShoppingPlanItem item : items) {
            if (builder.length() > 0) {
                builder.append(INGREDIENT_SEPARATOR);
            }
            String raw = item.getId() + FIELD_SEPARATOR
                    + item.getName() + FIELD_SEPARATOR
                    + item.getAmount() + FIELD_SEPARATOR
                    + item.getUnit() + FIELD_SEPARATOR
                    + item.getNote() + FIELD_SEPARATOR
                    + item.getCategory() + FIELD_SEPARATOR
                    + item.getStatus().name();
            builder.append(encode(raw));
        }
        return builder.toString();
    }

    private List<ShoppingPlanItem> toPlanItems(List<String> ingredients) {
        List<ShoppingPlanItem> items = new ArrayList<>();
        for (String ingredient : ingredients) {
            if (ingredient == null || ingredient.trim().isEmpty()) {
                continue;
            }
            items.add(createDefaultItem(ingredient.trim()));
        }
        return items;
    }

    private ShoppingPlanItem createDefaultItem(String name) {
        IngredientSpec spec = parseIngredientSpec(name);
        String normalized = normalize(spec.name);
        if (spec.hasQuantity) {
            return item(spec.name, spec.amount, spec.unit, "", inferCategory(normalized));
        }
        if (normalized.contains("thit bo") || normalized.contains("bo")) {
            return item(spec.name, 300, "g", "Cắt mỏng", "Nguyên liệu chính");
        }
        if (normalized.contains("ca chua")) {
            return item(spec.name, 3, "quả", "Chọn quả chín", "Nguyên liệu chính");
        }
        if (normalized.contains("trung")) {
            return item(spec.name, 2, "quả", "", "Nguyên liệu chính");
        }
        if (normalized.contains("hanh la")) {
            return item(spec.name, 2, "nhánh", "", "Gia vị & Khác");
        }
        if (normalized.contains("hanh tim")) {
            return item(spec.name, 1, "củ", "Băm nhỏ", "Gia vị & Khác");
        }
        if (normalized.contains("toi")) {
            return item(spec.name, 1, "củ", "Băm nhỏ", "Gia vị & Khác");
        }
        if (normalized.contains("tieu")) {
            return item(spec.name, 1, "gói", "", "Gia vị & Khác");
        }
        if (normalized.contains("nuoc mam") || normalized.contains("nuoc tuong")) {
            return item(spec.name, 1, "chai nhỏ", "", "Gia vị & Khác");
        }
        return item(spec.name, 1, "phần", "", inferCategory(normalized));
    }

    private IngredientSpec parseIngredientSpec(String rawName) {
        String value = rawName == null ? "" : rawName.trim();
        Matcher trailingMatcher = TRAILING_QUANTITY_PATTERN.matcher(value);
        if (trailingMatcher.matches()) {
            return new IngredientSpec(trailingMatcher.group(1).trim(),
                    parsePositiveInt(trailingMatcher.group(2)),
                    trailingMatcher.group(3).trim(),
                    true);
        }
        Matcher leadingMatcher = LEADING_QUANTITY_PATTERN.matcher(value);
        if (leadingMatcher.matches()) {
            return new IngredientSpec(leadingMatcher.group(3).trim(),
                    parsePositiveInt(leadingMatcher.group(1)),
                    leadingMatcher.group(2).trim(),
                    true);
        }
        return new IngredientSpec(value, 1, "phần", false);
    }

    private int parsePositiveInt(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private String inferCategory(String normalizedName) {
        if (normalizedName.contains("thit") || normalizedName.contains("bo")
                || normalizedName.contains("ga") || normalizedName.contains("xuong")
                || normalizedName.contains("ca ") || normalizedName.contains("trung")) {
            return "Nguyên liệu chính";
        }
        return "Gia vị & Khác";
    }

    private static class IngredientSpec {
        private final String name;
        private final int amount;
        private final String unit;
        private final boolean hasQuantity;

        private IngredientSpec(String name, int amount, String unit, boolean hasQuantity) {
            this.name = name;
            this.amount = amount;
            this.unit = unit;
            this.hasQuantity = hasQuantity;
        }
    }

    private ShoppingPlanItem item(String name, int amount, String unit, String note, String category) {
        return new ShoppingPlanItem(createItemId(name, unit, ShoppingItemStatus.NEED_BUY),
                name, amount, unit, note, category,
                ShoppingItemStatus.NEED_BUY);
    }

    private void addOrMergeItem(List<ShoppingPlanItem> items, ShoppingPlanItem nextItem) {
        ShoppingPlanItem existing = findMergeCandidate(items, nextItem);
        if (existing == null) {
            items.add(nextItem);
            return;
        }
        items.remove(existing);
        items.add(mergeItems(existing, nextItem));
    }

    private ShoppingPlanItem mergeItems(ShoppingPlanItem existing, ShoppingPlanItem nextItem) {
        ShoppingItemStatus nextStatus = existing.getStatus() == ShoppingItemStatus.BOUGHT
                ? ShoppingItemStatus.NEED_BUY
                : existing.getStatus();
        return new ShoppingPlanItem(
                createItemId(existing.getName(), existing.getUnit(), nextStatus),
                existing.getName(),
                existing.getAmount() + nextItem.getAmount(),
                existing.getUnit(),
                mergeNotes(existing.getNote(), nextItem.getNote()),
                existing.getCategory().isEmpty() ? nextItem.getCategory() : existing.getCategory(),
                nextStatus);
    }

    private ShoppingPlanItem findMergeCandidate(List<ShoppingPlanItem> items, ShoppingPlanItem target) {
        String targetKey = mergeKey(target);
        return findItemByKey(items, targetKey);
    }

    private ShoppingPlanItem findItemByKey(List<ShoppingPlanItem> items, String targetKey) {
        for (ShoppingPlanItem item : items) {
            if (mergeKey(item).equals(targetKey)) {
                return item;
            }
        }
        return null;
    }

    private ShoppingPlanItem findByName(List<ShoppingPlanItem> items, String name) {
        String normalized = normalize(name);
        for (ShoppingPlanItem item : items) {
            if (normalize(item.getName()).equals(normalized)) {
                return item;
            }
        }
        return null;
    }

    private String appendRecipeName(String existingRecipeName, String nextRecipeName) {
        String existing = existingRecipeName == null ? "" : existingRecipeName.trim();
        String next = nextRecipeName == null ? "" : nextRecipeName.trim();
        if (next.isEmpty()) {
            return existing;
        }
        if (existing.isEmpty()) {
            return next;
        }
        for (String part : existing.split(" \\+ ")) {
            if (part.trim().equalsIgnoreCase(next)) {
                return existing;
            }
        }
        return existing + " + " + next;
    }

    private String mergeNotes(String first, String second) {
        String safeFirst = first == null ? "" : first.trim();
        String safeSecond = second == null ? "" : second.trim();
        if (safeFirst.isEmpty()) {
            return safeSecond;
        }
        if (safeSecond.isEmpty() || safeFirst.equalsIgnoreCase(safeSecond)) {
            return safeFirst;
        }
        return safeFirst + "; " + safeSecond;
    }

    private void trackSplitSelection(ShoppingPlanItem item, int oldAmount, int nextAmount) {
        if (item.getStatus() != ShoppingItemStatus.AT_HOME || oldAmount == nextAmount) {
            return;
        }
        int currentRemainder = getSplitRemainder(item.getId());
        int nextRemainder = currentRemainder + oldAmount - nextAmount;
        if (nextRemainder <= 0) {
            clearSplitRemainder(item.getId());
        } else {
            preferences.edit()
                    .putInt(KEY_SPLIT_PREFIX + item.getId(), nextRemainder)
                    .apply();
        }
    }

    private int getSplitRemainder(String itemId) {
        return preferences.getInt(KEY_SPLIT_PREFIX + itemId, 0);
    }

    private int consumeSplitRemainder(String itemId) {
        int amount = getSplitRemainder(itemId);
        clearSplitRemainder(itemId);
        return amount;
    }

    private void clearSplitRemainder(String itemId) {
        preferences.edit().remove(KEY_SPLIT_PREFIX + itemId).apply();
    }

    private int quantityStep(ShoppingPlanItem item) {
        String unit = normalizeUnit(item.getUnit());
        if ("g".equals(unit) || "gram".equals(unit) || "ml".equals(unit)) {
            return 50;
        }
        return 1;
    }

    private String createItemId(String name, String unit, ShoppingItemStatus status) {
        return mergeKey(name, unit, status);
    }

    private String entityId(String modelId, boolean committed) {
        return (committed ? "list|" : "plan|") + modelId;
    }

    private ShoppingItemEntity findSourceEntity(String modelId, boolean committed) {
        ShoppingItemEntity entity = shoppingItemDao.findById(entityId(modelId, committed));
        if (entity != null) {
            return entity;
        }
        entity = shoppingItemDao.findById(modelId);
        if (entity != null) {
            return entity;
        }
        entity = shoppingItemDao.findById(entityId(modelId, !committed));
        if (entity != null) {
            return entity;
        }
        String alternatePrefix = committed ? "plan|" : "list|";
        return shoppingItemDao.findById(alternatePrefix + modelId);
    }

    private String modelId(String entityId) {
        if (entityId == null) {
            return "";
        }
        if (entityId.startsWith("list|") || entityId.startsWith("plan|")) {
            return entityId.substring(5);
        }
        return entityId;
    }

    private String mergeKey(ShoppingPlanItem item) {
        return mergeKey(item.getName(), item.getUnit(), item.getStatus());
    }

    private String mergeKey(String name, String unit, ShoppingItemStatus status) {
        return normalize(name) + "|" + normalizeUnit(unit) + "|" + status.name();
    }

    private String normalizeUnit(String unit) {
        return normalize(unit).replace(" ", "");
    }


    private List<String> namesWithNeedBuyStatus(List<ShoppingPlanItem> items) {
        List<String> names = new ArrayList<>();
        for (ShoppingPlanItem item : items) {
            if (item.getStatus() == ShoppingItemStatus.NEED_BUY) {
                names.add(item.getName());
            }
        }
        return names;
    }

    private String keyRecipeId(boolean committed) {
        return committed ? KEY_LIST_RECIPE_ID : KEY_PLAN_RECIPE_ID;
    }

    private String keyRecipeName(boolean committed) {
        return committed ? KEY_LIST_RECIPE_NAME : KEY_PLAN_RECIPE_NAME;
    }

    private String keyItems(boolean committed) {
        return committed ? KEY_LIST_ITEMS : KEY_PLAN_ITEMS;
    }

    private String keyMissingIngredients(boolean committed) {
        return committed ? KEY_LIST_MISSING_INGREDIENTS : KEY_PLAN_MISSING_INGREDIENTS;
    }

    private String keyUpdatedAt(boolean committed) {
        return committed ? KEY_LIST_UPDATED_AT : KEY_PLAN_UPDATED_AT;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim()
                .toLowerCase(Locale.US)
                .replace('á', 'a').replace('à', 'a').replace('ả', 'a').replace('ã', 'a').replace('ạ', 'a')
                .replace('ă', 'a').replace('ắ', 'a').replace('ằ', 'a').replace('ẳ', 'a').replace('ẵ', 'a').replace('ặ', 'a')
                .replace('â', 'a').replace('ấ', 'a').replace('ầ', 'a').replace('ẩ', 'a').replace('ẫ', 'a').replace('ậ', 'a')
                .replace('é', 'e').replace('è', 'e').replace('ẻ', 'e').replace('ẽ', 'e').replace('ẹ', 'e')
                .replace('ê', 'e').replace('ế', 'e').replace('ề', 'e').replace('ể', 'e').replace('ễ', 'e').replace('ệ', 'e')
                .replace('í', 'i').replace('ì', 'i').replace('ỉ', 'i').replace('ĩ', 'i').replace('ị', 'i')
                .replace('ó', 'o').replace('ò', 'o').replace('ỏ', 'o').replace('õ', 'o').replace('ọ', 'o')
                .replace('ô', 'o').replace('ố', 'o').replace('ồ', 'o').replace('ổ', 'o').replace('ỗ', 'o').replace('ộ', 'o')
                .replace('ơ', 'o').replace('ớ', 'o').replace('ờ', 'o').replace('ở', 'o').replace('ỡ', 'o').replace('ợ', 'o')
                .replace('ú', 'u').replace('ù', 'u').replace('ủ', 'u').replace('ũ', 'u').replace('ụ', 'u')
                .replace('ư', 'u').replace('ứ', 'u').replace('ừ', 'u').replace('ử', 'u').replace('ữ', 'u').replace('ự', 'u')
                .replace('ý', 'y').replace('ỳ', 'y').replace('ỷ', 'y').replace('ỹ', 'y').replace('ỵ', 'y')
                .replace('đ', 'd');
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
