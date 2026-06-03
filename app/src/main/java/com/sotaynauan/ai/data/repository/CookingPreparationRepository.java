package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.dao.CookingPlanDao;
import com.sotaynauan.ai.data.local.dao.IngredientDao;
import com.sotaynauan.ai.data.local.dao.PantryDao;
import com.sotaynauan.ai.data.local.dao.RecipeDao;
import com.sotaynauan.ai.data.local.dao.ShoppingItemDao;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;
import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.local.entity.InventoryTransactionEntity;
import com.sotaynauan.ai.data.local.entity.PantryBatchEntity;
import com.sotaynauan.ai.data.local.entity.PantryStockEntity;
import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.local.entity.RecipeIngredientEntity;
import com.sotaynauan.ai.data.local.entity.ShoppingItemEntity;
import com.sotaynauan.ai.data.model.ShoppingItemStatus;
import com.sotaynauan.ai.data.seed.IngredientSeedData;
import com.sotaynauan.ai.data.seed.RecipeIngredientParser;
import com.sotaynauan.ai.util.IngredientUsageRules;
import com.sotaynauan.ai.util.UnitConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CookingPreparationRepository {
    private static volatile boolean seedSynced;

    private final AppDatabase database;
    private final RecipeDao recipeDao;
    private final IngredientDao ingredientDao;
    private final PantryDao pantryDao;
    private final CookingPlanDao cookingPlanDao;
    private final ShoppingItemDao shoppingItemDao;
    private final UnitConverter unitConverter;
    private final InventoryRepository inventoryRepository;

    public CookingPreparationRepository(AppDatabase database) {
        this.database = database;
        this.recipeDao = database.recipeDao();
        this.ingredientDao = database.ingredientDao();
        this.pantryDao = database.pantryDao();
        this.cookingPlanDao = database.cookingPlanDao();
        this.shoppingItemDao = database.shoppingItemDao();
        this.unitConverter = new UnitConverter();
        this.inventoryRepository = new InventoryRepository(database, unitConverter);
    }

    public CookingPlanEntity createPlanFromRecipe(long recipeId, int targetServings) {
        seedBaseIngredientsIfNeeded();
        CookingPlanEntity openPlan = cookingPlanDao.getOpenPlanForRecipe(recipeId);
        if (openPlan != null && !cookingPlanDao.getPlanIngredients(openPlan.id).isEmpty()) {
            return openPlan;
        }
        RecipeEntity recipe = recipeDao.findById(recipeId);
        if (recipe == null) {
            throw new IllegalStateException("Recipe not found: " + recipeId);
        }
        List<RecipeIngredientEntity> recipeIngredients = parseRecipeIngredientsForPlan(recipe);
        if (recipeIngredients.isEmpty()) {
            recipeIngredients = ensureRecipeIngredients(recipe);
        }
        if (recipeIngredients.isEmpty()) {
            throw new IllegalStateException("Recipe has no parsed ingredients: " + recipe.ingredients);
        }
        int defaultServings = parseDefaultServings(recipe.serving);
        int safeTargetServings = targetServings > 0 ? targetServings : defaultServings;
        double multiplier = defaultServings <= 0 ? 1d : safeTargetServings / (double) defaultServings;
        long now = System.currentTimeMillis();

        CookingPlanEntity plan = new CookingPlanEntity();
        plan.id = "plan_" + recipeId + "_" + now;
        plan.recipeId = recipeId;
        plan.targetServings = safeTargetServings;
        plan.servingMultiplier = multiplier;
        plan.status = CookingPlanEntity.STATUS_PREPARING;
        plan.createdAt = now;
        plan.updatedAt = now;
        cookingPlanDao.upsertPlan(plan);

        List<CookingPlanIngredientEntity> items = new ArrayList<>();
        for (RecipeIngredientEntity recipeIngredient : recipeIngredients) {
            IngredientEntity ingredient = ingredientDao.findById(recipeIngredient.ingredientId);
            boolean presenceOnly = isPresenceOnly(ingredient, recipeIngredient.note, recipeIngredient.unit);
            CookingPlanIngredientEntity item = new CookingPlanIngredientEntity();
            item.planId = plan.id;
            item.ingredientId = recipeIngredient.ingredientId;
            item.requiredAmount = presenceOnly ? 1d : Math.max(0d, recipeIngredient.amount * multiplier);
            item.missingAmount = item.requiredAmount;
            item.baseUnit = presenceOnly ? "piece" : recipeIngredient.unit;
            item.prepareStatus = CookingPlanIngredientEntity.STATUS_NEED_CHECK;
            item.userNote = recipeIngredient.note;
            item.updatedAt = now;
            items.add(item);
        }
        cookingPlanDao.upsertPlanIngredients(items);
        return plan;
    }

    public CookingPlanEntity preparePlanFromRecipe(long recipeId, int targetServings) {
        CookingPlanEntity plan = createPlanFromRecipe(recipeId, targetServings);
        applyCommittedShoppingProgress(plan.id);
        return cookingPlanDao.getPlan(plan.id);
    }

    public CookingPlanEntity preparePlanFromRecipe(long recipeId, int targetServings,
                                                   List<String> checkedIngredients) {
        CookingPlanEntity plan = preparePlanFromRecipe(recipeId, targetServings);
        applyCheckedIngredients(plan.id, checkedIngredients);
        return cookingPlanDao.getPlan(plan.id);
    }

    public void markHaveAtHome(String planId, String ingredientId, double selectedAmount, String unit) {
        database.runInTransaction(() -> {
            CookingPlanIngredientEntity item = requirePlanIngredient(planId, ingredientId);
            PantryStockEntity stock = ensureStock(item.ingredientId, item.baseUnit);
            double selectedBase = unitConverter.toBase(selectedAmount, unit, item.baseUnit);
            double oldReserved = item.reservedAmount;
            double availableIncludingOld = Math.max(0d,
                    stock.totalAmount - stock.reservedAmount + oldReserved);
            double newReserved = Math.min(Math.min(selectedBase, item.requiredAmount), availableIncludingOld);
            double deltaReserved = newReserved - oldReserved;
            long now = System.currentTimeMillis();

            stock.reservedAmount = Math.max(0d, Math.min(stock.totalAmount,
                    stock.reservedAmount + deltaReserved));
            stock.lastUpdatedAt = now;
            pantryDao.upsertStock(stock);

            item.homeSelectedAmount = newReserved;
            item.reservedAmount = newReserved;
            recalculateItem(item, now);
            cookingPlanDao.upsertPlanIngredient(item);
            pantryDao.insertTransaction(transaction(item.ingredientId, planId, null,
                    deltaReserved >= 0d
                            ? InventoryTransactionEntity.TYPE_RESERVE_FOR_PLAN
                            : InventoryTransactionEntity.TYPE_RELEASE_RESERVATION,
                    deltaReserved, item.baseUnit, "User marked have at home", now));
            syncShoppingItem(item, now);
            updatePlanReadiness(planId, now);
        });
    }

    public void markNeedBuy(String planId, String ingredientId) {
        database.runInTransaction(() -> {
            CookingPlanIngredientEntity item = requirePlanIngredient(planId, ingredientId);
            long now = System.currentTimeMillis();
            syncShoppingItem(item, now);
            updatePlanReadiness(planId, now);
        });
    }

    public void markBought(String planId, String ingredientId, double boughtAmount, String unit) {
        database.runInTransaction(() -> {
            CookingPlanIngredientEntity item = requirePlanIngredient(planId, ingredientId);
            double boughtBase = unitConverter.toBase(boughtAmount, unit, item.baseUnit);
            long now = System.currentTimeMillis();

            PantryBatchEntity batch = new PantryBatchEntity();
            batch.ingredientId = item.ingredientId;
            batch.remainingAmount = boughtBase;
            batch.baseUnit = item.baseUnit;
            batch.source = "shopping";
            batch.boughtAt = now;
            batch.createdAt = now;
            batch.updatedAt = now;
            long batchId = pantryDao.insertBatch(batch);

            PantryStockEntity stock = ensureStock(item.ingredientId, item.baseUnit);
            stock.totalAmount += boughtBase;
            stock.lastUpdatedAt = now;
            pantryDao.upsertStock(stock);
            pantryDao.insertTransaction(transaction(item.ingredientId, planId, batchId,
                    InventoryTransactionEntity.TYPE_ADD_PURCHASE, boughtBase, item.baseUnit,
                    "Bought for cooking plan", now));

            item.purchasedAmount += boughtBase;
            recalculateItem(item, now);
            cookingPlanDao.upsertPlanIngredient(item);
            syncBoughtShoppingItem(item, boughtBase, now);
            updatePlanReadiness(planId, now);
        });
    }

    public boolean isPlanReadyToCook(String planId) {
        List<CookingPlanIngredientEntity> items = cookingPlanDao.getPlanIngredients(planId);
        if (items.isEmpty()) {
            return false;
        }
        for (CookingPlanIngredientEntity item : items) {
            if (item.missingAmount > 0.0001d) {
                return false;
            }
        }
        return true;
    }

    public List<CookingPlanIngredientEntity> getMissingIngredients(String planId) {
        List<CookingPlanIngredientEntity> missing = new ArrayList<>();
        for (CookingPlanIngredientEntity item : cookingPlanDao.getPlanIngredients(planId)) {
            if (item.missingAmount > 0.0001d) {
                missing.add(item);
            }
        }
        return missing;
    }

    public int addMissingIngredientsToShoppingList(String planId) {
        final int[] added = {0};
        database.runInTransaction(() -> {
            long now = System.currentTimeMillis();
            for (CookingPlanIngredientEntity item : cookingPlanDao.getPlanIngredients(planId)) {
                if (item.missingAmount <= 0.0001d) {
                    continue;
                }
                syncShoppingListItem(item, now);
                added[0]++;
            }
        });
        return added[0];
    }

    public void completeCooking(String planId) {
        inventoryRepository.consumeForCompletedCooking(planId);
    }

    public void cancelPlan(String planId) {
        database.runInTransaction(() -> {
            inventoryRepository.releaseReservation(planId);
            CookingPlanEntity plan = cookingPlanDao.getPlan(planId);
            if (plan != null && !CookingPlanEntity.STATUS_COMPLETED.equals(plan.status)) {
                long now = System.currentTimeMillis();
                plan.status = CookingPlanEntity.STATUS_CANCELLED;
                plan.updatedAt = now;
                cookingPlanDao.updatePlan(plan);
            }
        });
    }

    public CookingPlanEntity getPlan(String planId) {
        return cookingPlanDao.getPlan(planId);
    }

    public CookingPlanEntity getLatestCookablePlan() {
        return cookingPlanDao.getLatestCookablePlan();
    }

    public List<CookingPlanIngredientEntity> getPlanIngredients(String planId) {
        return cookingPlanDao.getPlanIngredients(planId);
    }

    public IngredientEntity getIngredient(String ingredientId) {
        return ingredientDao.findById(ingredientId);
    }

    public boolean isPresenceOnly(CookingPlanIngredientEntity item) {
        return isPresenceOnly(ingredientDao.findById(item.ingredientId), item.userNote, item.baseUnit);
    }

    public double getAvailableAmount(String ingredientId) {
        PantryStockEntity stock = pantryDao.getStock(ingredientId);
        return stock == null ? 0d : Math.max(0d, stock.totalAmount - stock.reservedAmount);
    }

    private void applyCommittedShoppingProgress(String planId) {
        List<ShoppingItemEntity> shoppingItems = shoppingItemDao.getItemsByCommitted(true);
        if (shoppingItems.isEmpty()) {
            return;
        }
        for (CookingPlanIngredientEntity item : cookingPlanDao.getPlanIngredients(planId)) {
            if (item.missingAmount <= 0.0001d) {
                continue;
            }
            ShoppingItemEntity shoppingItem = findShoppingProgressItem(planId, item, shoppingItems);
            if (shoppingItem == null) {
                continue;
            }
            ShoppingItemStatus status = ShoppingItemStatus.fromName(shoppingItem.status);
            if (status == ShoppingItemStatus.BOUGHT || status == ShoppingItemStatus.AT_HOME) {
                markBought(planId, item.ingredientId, amountForProgress(item, shoppingItem),
                        unitForProgress(item, shoppingItem));
            }
        }
    }

    private void applyCheckedIngredients(String planId, List<String> checkedIngredients) {
        if (checkedIngredients == null || checkedIngredients.isEmpty()) {
            return;
        }
        List<CookingPlanIngredientEntity> items = cookingPlanDao.getPlanIngredients(planId);
        for (CookingPlanIngredientEntity item : items) {
            if (item.missingAmount <= 0.0001d || !isCheckedIngredient(item, checkedIngredients)) {
                continue;
            }
            markBought(planId, item.ingredientId, amountForManualAvailability(item), item.baseUnit);
        }
    }

    private boolean isCheckedIngredient(CookingPlanIngredientEntity item, List<String> checkedIngredients) {
        String ingredientKey = normalizeName(ingredientName(item.ingredientId));
        String noteKey = normalizeName(item.userNote);
        for (String checkedIngredient : checkedIngredients) {
            String checkedKey = normalizeName(checkedIngredient);
            if (!ingredientKey.isEmpty() && ingredientKey.equals(checkedKey)) {
                return true;
            }
            if (!noteKey.isEmpty() && noteKey.equals(checkedKey)) {
                return true;
            }
        }
        return false;
    }

    private double amountForManualAvailability(CookingPlanIngredientEntity item) {
        if (isPresenceOnly(item)) {
            return 1d;
        }
        return Math.max(0d, item.missingAmount);
    }

    private ShoppingItemEntity findShoppingProgressItem(String planId, CookingPlanIngredientEntity item,
                                                       List<ShoppingItemEntity> shoppingItems) {
        String ingredientKey = normalizeName(ingredientName(item.ingredientId));
        String noteKey = normalizeName(item.userNote);
        for (ShoppingItemEntity shoppingItem : shoppingItems) {
            if (planId.equals(shoppingItem.planId) && item.ingredientId.equals(shoppingItem.ingredientId)) {
                return shoppingItem;
            }
            String itemKey = normalizeName(shoppingItem.name);
            if (!ingredientKey.isEmpty() && itemKey.equals(ingredientKey)) {
                return shoppingItem;
            }
            if (!noteKey.isEmpty() && itemKey.equals(noteKey)) {
                return shoppingItem;
            }
        }
        return null;
    }

    private double amountForProgress(CookingPlanIngredientEntity item, ShoppingItemEntity shoppingItem) {
        if (isPresenceOnly(item)) {
            return 1d;
        }
        return Math.max(item.missingAmount, shoppingItem.amount);
    }

    private String unitForProgress(CookingPlanIngredientEntity item, ShoppingItemEntity shoppingItem) {
        if (isPresenceOnly(item)) {
            return "piece";
        }
        return shoppingItem.unit == null || shoppingItem.unit.trim().isEmpty()
                ? item.baseUnit
                : shoppingItem.unit;
    }

    private void seedBaseIngredientsIfNeeded() {
        if (seedSynced) {
            return;
        }
        List<IngredientEntity> seedIngredients = new IngredientSeedData().createIngredients();
        ingredientDao.upsertAll(seedIngredients);
        for (IngredientEntity ingredient : seedIngredients) {
            if (ingredientDao.findById(ingredient.id) != null) {
                ingredientDao.update(ingredient);
            }
        }
        seedSynced = true;
    }

    private List<RecipeIngredientEntity> ensureRecipeIngredients(RecipeEntity recipe) {
        List<RecipeIngredientEntity> existing = recipeDao.getRecipeIngredients(recipe.id);
        if (!existing.isEmpty()) {
            return existing;
        }
        RecipeIngredientParser parser = new RecipeIngredientParser();
        List<RecipeIngredientEntity> recipeIngredients = new ArrayList<>();
        List<IngredientEntity> ingredients = new ArrayList<>();
        List<String> rawIngredients = split(recipe.ingredients);
        for (int index = 0; index < rawIngredients.size(); index++) {
            RecipeIngredientParser.ParsedIngredient parsed =
                    parser.parse(recipe.id, rawIngredients.get(index), index);
            ingredients.add(parsed.ingredient);
            recipeIngredients.add(parsed.recipeIngredient);
        }
        ingredientDao.upsertAll(ingredients);
        resolveRecipeIngredientReferences(ingredients, recipeIngredients);
        recipeDao.upsertRecipeIngredients(recipeIngredients);
        return recipeIngredients;
    }

    private List<RecipeIngredientEntity> parseRecipeIngredientsForPlan(RecipeEntity recipe) {
        RecipeIngredientParser parser = new RecipeIngredientParser();
        List<RecipeIngredientEntity> recipeIngredients = new ArrayList<>();
        List<IngredientEntity> ingredients = new ArrayList<>();
        List<String> rawIngredients = split(recipe.ingredients);
        for (int index = 0; index < rawIngredients.size(); index++) {
            RecipeIngredientParser.ParsedIngredient parsed =
                    parser.parse(recipe.id, rawIngredients.get(index), index);
            ingredients.add(parsed.ingredient);
            recipeIngredients.add(parsed.recipeIngredient);
        }
        if (!ingredients.isEmpty()) {
            ingredientDao.upsertAll(ingredients);
            resolveRecipeIngredientReferences(ingredients, recipeIngredients);
        }
        if (!recipeIngredients.isEmpty() && recipeDao.getRecipeIngredients(recipe.id).isEmpty()) {
            recipeDao.upsertRecipeIngredients(recipeIngredients);
        }
        return recipeIngredients;
    }

    private void resolveRecipeIngredientReferences(List<IngredientEntity> parsedIngredients,
                                                   List<RecipeIngredientEntity> recipeIngredients) {
        for (int index = 0; index < parsedIngredients.size() && index < recipeIngredients.size(); index++) {
            IngredientEntity resolved = ingredientDao.findByNormalizedName(parsedIngredients.get(index).normalizedName);
            if (resolved == null) {
                resolved = ingredientDao.findById(parsedIngredients.get(index).id);
            }
            if (resolved != null) {
                recipeIngredients.get(index).ingredientId = resolved.id;
            }
        }
    }

    private List<String> split(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }
        String[] parts = raw.split("\\|");
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private int parseDefaultServings(String serving) {
        if (serving == null || serving.trim().isEmpty()) {
            return 1;
        }
        String digits = serving.replaceAll("[^0-9]+", " ").trim();
        if (digits.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(digits.split("\\s+")[0]));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private CookingPlanIngredientEntity requirePlanIngredient(String planId, String ingredientId) {
        CookingPlanIngredientEntity item = cookingPlanDao.getPlanIngredient(planId, ingredientId);
        if (item == null) {
            throw new IllegalStateException("Plan ingredient not found: " + planId + "/" + ingredientId);
        }
        return item;
    }

    private PantryStockEntity ensureStock(String ingredientId, String baseUnit) {
        PantryStockEntity stock = pantryDao.getStock(ingredientId);
        if (stock != null) {
            return stock;
        }
        stock = new PantryStockEntity();
        stock.ingredientId = ingredientId;
        stock.baseUnit = baseUnit;
        stock.lastUpdatedAt = System.currentTimeMillis();
        pantryDao.upsertStock(stock);
        return pantryDao.getStock(ingredientId);
    }

    private void recalculateItem(CookingPlanIngredientEntity item, long now) {
        item.missingAmount = Math.max(0d,
                item.requiredAmount - item.homeSelectedAmount - item.purchasedAmount);
        if (item.missingAmount <= 0.0001d) {
            item.prepareStatus = CookingPlanIngredientEntity.STATUS_READY;
        } else if (item.homeSelectedAmount > 0d) {
            item.prepareStatus = CookingPlanIngredientEntity.STATUS_NEED_BUY;
        } else {
            item.prepareStatus = CookingPlanIngredientEntity.STATUS_NEED_BUY;
        }
        item.updatedAt = now;
    }

    private void updatePlanReadiness(String planId, long now) {
        CookingPlanEntity plan = cookingPlanDao.getPlan(planId);
        if (plan == null || CookingPlanEntity.STATUS_CANCELLED.equals(plan.status)
                || CookingPlanEntity.STATUS_COMPLETED.equals(plan.status)) {
            return;
        }
        plan.status = isPlanReadyToCook(planId)
                ? CookingPlanEntity.STATUS_READY_TO_COOK
                : CookingPlanEntity.STATUS_PREPARING;
        plan.updatedAt = now;
        cookingPlanDao.updatePlan(plan);
    }

    private void syncShoppingItem(CookingPlanIngredientEntity item, long now) {
        ShoppingItemEntity shoppingItem = shoppingItemDao.findByPlanIngredient(item.planId, item.ingredientId);
        if (shoppingItem == null) {
            shoppingItem = new ShoppingItemEntity();
            shoppingItem.id = item.planId + "_" + item.ingredientId;
            shoppingItem.planId = item.planId;
            shoppingItem.ingredientId = item.ingredientId;
            shoppingItem.recipeId = planRecipeId(item.planId);
            shoppingItem.name = ingredientName(item.ingredientId);
            shoppingItem.displayName = shoppingItem.name;
            shoppingItem.category = isPresenceOnly(item) ? "Gia vị & Khác" : "Nguyên liệu chính";
            shoppingItem.createdAt = now;
            shoppingItem.committed = false;
        }
        shoppingItem.amount = displayAmount(item);
        shoppingItem.requiredAmount = item.missingAmount;
        shoppingItem.boughtAmount = item.purchasedAmount;
        shoppingItem.status = item.missingAmount <= 0.0001d
                ? ShoppingItemStatus.AT_HOME.name()
                : ShoppingItemStatus.NEED_BUY.name();
        shoppingItem.unit = displayUnit(item);
        shoppingItem.baseUnit = item.baseUnit;
        shoppingItem.updatedAt = now;
        shoppingItem.updatedAtMillis = now;
        shoppingItemDao.upsert(shoppingItem);
    }

    private void syncShoppingListItem(CookingPlanIngredientEntity item, long now) {
        ShoppingItemEntity shoppingItem = shoppingItemDao.findById(
                "list|" + item.planId + "_" + item.ingredientId);
        if (shoppingItem == null) {
            shoppingItem = new ShoppingItemEntity();
            shoppingItem.id = "list|" + item.planId + "_" + item.ingredientId;
            shoppingItem.planId = item.planId;
            shoppingItem.ingredientId = item.ingredientId;
            shoppingItem.recipeId = planRecipeId(item.planId);
            shoppingItem.recipeName = recipeName(shoppingItem.recipeId);
            shoppingItem.name = ingredientName(item.ingredientId);
            shoppingItem.displayName = shoppingItem.name;
            shoppingItem.note = item.userNote;
            shoppingItem.category = isPresenceOnly(item) ? "Gia vị & Khác" : "Nguyên liệu chính";
            shoppingItem.createdAt = now;
            shoppingItem.committed = true;
        }
        shoppingItem.amount = displayAmount(item);
        shoppingItem.requiredAmount = item.missingAmount;
        shoppingItem.boughtAmount = item.purchasedAmount;
        shoppingItem.status = ShoppingItemStatus.NEED_BUY.name();
        shoppingItem.unit = displayUnit(item);
        shoppingItem.baseUnit = item.baseUnit;
        shoppingItem.updatedAt = now;
        shoppingItem.updatedAtMillis = now;
        shoppingItemDao.upsert(shoppingItem);
    }

    private void syncBoughtShoppingItem(CookingPlanIngredientEntity item, double boughtBase, long now) {
        ShoppingItemEntity shoppingItem = shoppingItemDao.findByPlanIngredient(item.planId, item.ingredientId);
        if (shoppingItem == null) {
            syncShoppingItem(item, now);
            shoppingItem = shoppingItemDao.findByPlanIngredient(item.planId, item.ingredientId);
        }
        if (shoppingItem == null) {
            return;
        }
        shoppingItem.amount = displayAmount(item);
        shoppingItem.requiredAmount = item.missingAmount;
        shoppingItem.boughtAmount += boughtBase;
        shoppingItem.status = item.missingAmount <= 0.0001d
                ? ShoppingItemStatus.BOUGHT.name()
                : ShoppingItemStatus.NEED_BUY.name();
        shoppingItem.unit = displayUnit(item);
        shoppingItem.baseUnit = item.baseUnit;
        shoppingItem.updatedAt = now;
        shoppingItem.updatedAtMillis = now;
        shoppingItemDao.upsert(shoppingItem);
    }

    private long planRecipeId(String planId) {
        CookingPlanEntity plan = cookingPlanDao.getPlan(planId);
        return plan == null ? -1L : plan.recipeId;
    }

    private String ingredientName(String ingredientId) {
        IngredientEntity ingredient = ingredientDao.findById(ingredientId);
        return ingredient == null ? ingredientId : ingredient.name;
    }

    private String recipeName(long recipeId) {
        RecipeEntity recipe = recipeDao.findById(recipeId);
        return recipe == null ? "" : recipe.name;
    }

    private int displayAmount(CookingPlanIngredientEntity item) {
        if (isPresenceOnly(item)) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(Math.max(0d, item.missingAmount)));
    }

    private String displayUnit(CookingPlanIngredientEntity item) {
        return isPresenceOnly(item) ? IngredientUsageRules.presenceUnit() : item.baseUnit;
    }

    private boolean isPresenceOnly(IngredientEntity ingredient, String note, String unit) {
        return IngredientUsageRules.isPresenceOnly(
                ingredient == null ? "" : ingredient.name,
                ingredient == null ? "" : ingredient.category,
                note,
                unit);
    }

    private String displayIngredientName(String raw) {
        String value = raw == null ? "" : raw.trim();
        int colonIndex = value.indexOf(':');
        if (colonIndex > 0) {
            value = value.substring(0, colonIndex);
        }
        int dashIndex = value.indexOf(" - ");
        if (dashIndex >= 0 && dashIndex < value.length() - 3) {
            value = value.substring(dashIndex + 3);
        }
        return value.trim();
    }

    private String normalizeName(String value) {
        return IngredientSeedData.normalizeName(displayIngredientName(value));
    }

    private InventoryTransactionEntity transaction(String ingredientId, String planId, Long batchId,
                                                   String type, double deltaAmount, String baseUnit,
                                                   String note, long now) {
        InventoryTransactionEntity transaction = new InventoryTransactionEntity();
        transaction.ingredientId = ingredientId;
        transaction.planId = planId;
        transaction.batchId = batchId;
        transaction.type = type;
        transaction.deltaAmount = deltaAmount;
        transaction.baseUnit = baseUnit;
        transaction.note = note;
        transaction.createdAt = now;
        return transaction;
    }
}
