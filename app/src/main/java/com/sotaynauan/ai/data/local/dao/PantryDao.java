package com.sotaynauan.ai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sotaynauan.ai.data.local.entity.InventoryTransactionEntity;
import com.sotaynauan.ai.data.local.entity.PantryBatchEntity;
import com.sotaynauan.ai.data.local.entity.PantryStockEntity;

import java.util.List;

@Dao
public interface PantryDao {
    @Query("SELECT * FROM pantry_stocks WHERE ingredientId = :ingredientId LIMIT 1")
    PantryStockEntity getStock(String ingredientId);

    @Query("SELECT * FROM pantry_stocks ORDER BY ingredientId")
    List<PantryStockEntity> getAllStocks();

    @Query("SELECT * FROM pantry_batches WHERE ingredientId = :ingredientId AND remainingAmount > 0 ORDER BY CASE WHEN expiryAt IS NULL THEN 9223372036854775807 ELSE expiryAt END ASC, boughtAt ASC, id ASC")
    List<PantryBatchEntity> getUsableBatches(String ingredientId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertStock(PantryStockEntity stock);

    @Insert
    long insertBatch(PantryBatchEntity batch);

    @Update
    void updateBatch(PantryBatchEntity batch);

    @Insert
    long insertTransaction(InventoryTransactionEntity transaction);

    @Query("SELECT * FROM inventory_transactions WHERE planId = :planId ORDER BY createdAt ASC, id ASC")
    List<InventoryTransactionEntity> getTransactionsForPlan(String planId);
}
