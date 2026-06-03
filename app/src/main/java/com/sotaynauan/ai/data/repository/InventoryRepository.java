package com.sotaynauan.ai.data.repository;

import com.sotaynauan.ai.data.local.dao.CookingPlanDao;
import com.sotaynauan.ai.data.local.dao.IngredientDao;
import com.sotaynauan.ai.data.local.dao.PantryDao;
import com.sotaynauan.ai.data.local.database.AppDatabase;
import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;
import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.local.entity.InventoryTransactionEntity;
import com.sotaynauan.ai.data.local.entity.PantryBatchEntity;
import com.sotaynauan.ai.data.local.entity.PantryStockEntity;
import com.sotaynauan.ai.util.IngredientUsageRules;
import com.sotaynauan.ai.util.UnitConverter;

import java.util.List;

public class InventoryRepository {
    private final AppDatabase database;
    private final IngredientDao ingredientDao;
    private final PantryDao pantryDao;
    private final CookingPlanDao cookingPlanDao;
    private final UnitConverter unitConverter;

    public InventoryRepository(AppDatabase database) {
        this(database, new UnitConverter());
    }

    public InventoryRepository(AppDatabase database, UnitConverter unitConverter) {
        this.database = database;
        this.ingredientDao = database.ingredientDao();
        this.pantryDao = database.pantryDao();
        this.cookingPlanDao = database.cookingPlanDao();
        this.unitConverter = unitConverter;
    }

    public double getAvailableAmount(String ingredientId) {
        PantryStockEntity stock = pantryDao.getStock(ingredientId);
        if (stock == null) {
            return 0d;
        }
        return Math.max(0d, stock.totalAmount - stock.reservedAmount);
    }

    public void addPurchase(String ingredientId, double amount, String unit, String source, Long expiryAt) {
        database.runInTransaction(() -> {
            IngredientEntity ingredient = ensureIngredient(ingredientId, unit);
            double baseAmount = unitConverter.toBase(amount, unit, ingredient.baseUnit);
            long now = System.currentTimeMillis();

            PantryBatchEntity batch = new PantryBatchEntity();
            batch.ingredientId = ingredient.id;
            batch.remainingAmount = baseAmount;
            batch.baseUnit = ingredient.baseUnit;
            batch.source = source == null ? "manual" : source;
            batch.boughtAt = now;
            batch.expiryAt = expiryAt;
            batch.createdAt = now;
            batch.updatedAt = now;
            long batchId = pantryDao.insertBatch(batch);

            PantryStockEntity stock = ensureStock(ingredient.id, ingredient.baseUnit);
            stock.totalAmount += baseAmount;
            stock.lastUpdatedAt = now;
            pantryDao.upsertStock(stock);

            pantryDao.insertTransaction(transaction(ingredient.id, null, batchId,
                    InventoryTransactionEntity.TYPE_ADD_PURCHASE, baseAmount, ingredient.baseUnit,
                    "Added purchase to pantry", now));
        });
    }

    public double reserveForCookingPlan(String planId, String ingredientId,
                                        double requestedAmount, String unit) {
        final double[] reserved = {0d};
        database.runInTransaction(() -> {
            IngredientEntity ingredient = ensureIngredient(ingredientId, unit);
            PantryStockEntity stock = ensureStock(ingredient.id, ingredient.baseUnit);
            double requestedBase = unitConverter.toBase(requestedAmount, unit, ingredient.baseUnit);
            double available = Math.max(0d, stock.totalAmount - stock.reservedAmount);
            double nextReserved = Math.min(requestedBase, available);
            long now = System.currentTimeMillis();
            stock.reservedAmount = Math.min(stock.totalAmount, stock.reservedAmount + nextReserved);
            stock.lastUpdatedAt = now;
            pantryDao.upsertStock(stock);
            pantryDao.insertTransaction(transaction(ingredient.id, planId, null,
                    InventoryTransactionEntity.TYPE_RESERVE_FOR_PLAN, nextReserved,
                    ingredient.baseUnit, "Reserved for cooking plan", now));
            reserved[0] = nextReserved;
        });
        return reserved[0];
    }

    public void releaseReservation(String planId) {
        database.runInTransaction(() -> {
            List<CookingPlanIngredientEntity> items = cookingPlanDao.getPlanIngredients(planId);
            long now = System.currentTimeMillis();
            for (CookingPlanIngredientEntity item : items) {
                if (item.reservedAmount <= 0d) {
                    continue;
                }
                PantryStockEntity stock = ensureStock(item.ingredientId, item.baseUnit);
                stock.reservedAmount = Math.max(0d, stock.reservedAmount - item.reservedAmount);
                stock.lastUpdatedAt = now;
                pantryDao.upsertStock(stock);
                pantryDao.insertTransaction(transaction(item.ingredientId, planId, null,
                        InventoryTransactionEntity.TYPE_RELEASE_RESERVATION, -item.reservedAmount,
                        item.baseUnit, "Released cooking plan reservation", now));
                item.homeSelectedAmount = 0d;
                item.reservedAmount = 0d;
                item.missingAmount = Math.max(0d, item.requiredAmount - item.purchasedAmount);
                item.prepareStatus = item.missingAmount <= 0d
                        ? CookingPlanIngredientEntity.STATUS_READY
                        : CookingPlanIngredientEntity.STATUS_NEED_CHECK;
                item.updatedAt = now;
                cookingPlanDao.upsertPlanIngredient(item);
            }
        });
    }

    public void consumeForCompletedCooking(String planId) {
        database.runInTransaction(() -> {
            CookingPlanEntity plan = cookingPlanDao.getPlan(planId);
            if (plan == null) {
                throw new IllegalStateException("Cooking plan not found: " + planId);
            }
            if (!CookingPlanEntity.STATUS_READY_TO_COOK.equals(plan.status)
                    && !CookingPlanEntity.STATUS_COOKING.equals(plan.status)) {
                throw new IllegalStateException("Plan is not ready to complete: " + plan.status);
            }
            List<CookingPlanIngredientEntity> items = cookingPlanDao.getPlanIngredients(planId);
            long now = System.currentTimeMillis();
            for (CookingPlanIngredientEntity item : items) {
                if (item.missingAmount > 0.0001d) {
                    throw new IllegalStateException("Ingredient is still missing: " + item.ingredientId);
                }
                boolean presenceOnly = isPresenceOnly(item);
                PantryStockEntity stock = ensureStock(item.ingredientId, item.baseUnit);
                if (!presenceOnly) {
                    consumeFromBatches(planId, item.ingredientId, item.requiredAmount, item.baseUnit, now);
                    stock.totalAmount = Math.max(0d, stock.totalAmount - item.requiredAmount);
                }
                stock.reservedAmount = Math.max(0d, stock.reservedAmount - item.reservedAmount);
                stock.lastUpdatedAt = now;
                pantryDao.upsertStock(stock);

                item.consumedAmount = presenceOnly ? 0d : item.requiredAmount;
                item.prepareStatus = CookingPlanIngredientEntity.STATUS_CONSUMED;
                item.updatedAt = now;
                cookingPlanDao.upsertPlanIngredient(item);
                pantryDao.insertTransaction(transaction(item.ingredientId, planId, null,
                        InventoryTransactionEntity.TYPE_CONSUME_COOKING,
                        presenceOnly ? 0d : -item.requiredAmount,
                        item.baseUnit,
                        presenceOnly
                                ? "Presence-only item checked after cooking"
                                : "Consumed after completing cooking",
                        now));
            }
            plan.status = CookingPlanEntity.STATUS_COMPLETED;
            plan.completedAt = now;
            plan.updatedAt = now;
            cookingPlanDao.updatePlan(plan);
        });
    }

    private void consumeFromBatches(String planId, String ingredientId, double amount,
                                    String baseUnit, long now) {
        double remaining = amount;
        List<PantryBatchEntity> batches = pantryDao.getUsableBatches(ingredientId);
        for (PantryBatchEntity batch : batches) {
            if (remaining <= 0.0001d) {
                break;
            }
            if (!unitConverter.sameBaseUnit(batch.baseUnit, baseUnit)) {
                continue;
            }
            double used = Math.min(batch.remainingAmount, remaining);
            batch.remainingAmount = Math.max(0d, batch.remainingAmount - used);
            batch.updatedAt = now;
            pantryDao.updateBatch(batch);
            remaining -= used;
        }
        if (remaining > 0.0001d) {
            throw new IllegalStateException("Not enough stock to consume " + ingredientId
                    + " for plan " + planId);
        }
    }

    private IngredientEntity ensureIngredient(String ingredientId, String unit) {
        IngredientEntity ingredient = ingredientDao.findById(ingredientId);
        if (ingredient != null) {
            return ingredient;
        }
        long now = System.currentTimeMillis();
        ingredient = new IngredientEntity();
        ingredient.id = ingredientId;
        ingredient.name = ingredientId;
        ingredient.normalizedName = ingredientId;
        ingredient.category = "other";
        ingredient.baseUnit = unitConverter.baseUnitFor(unit);
        ingredient.aliasesJson = "[]";
        ingredient.isActive = true;
        ingredient.createdAt = now;
        ingredient.updatedAt = now;
        ingredientDao.upsert(ingredient);
        return ingredient;
    }

    private boolean isPresenceOnly(CookingPlanIngredientEntity item) {
        IngredientEntity ingredient = ingredientDao.findById(item.ingredientId);
        return IngredientUsageRules.isPresenceOnly(
                ingredient == null ? "" : ingredient.name,
                ingredient == null ? "" : ingredient.category,
                item.userNote,
                item.baseUnit);
    }

    private PantryStockEntity ensureStock(String ingredientId, String baseUnit) {
        PantryStockEntity stock = pantryDao.getStock(ingredientId);
        if (stock != null) {
            return stock;
        }
        stock = new PantryStockEntity();
        stock.ingredientId = ingredientId;
        stock.totalAmount = 0d;
        stock.reservedAmount = 0d;
        stock.baseUnit = baseUnit;
        stock.lastUpdatedAt = System.currentTimeMillis();
        pantryDao.upsertStock(stock);
        return pantryDao.getStock(ingredientId);
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
