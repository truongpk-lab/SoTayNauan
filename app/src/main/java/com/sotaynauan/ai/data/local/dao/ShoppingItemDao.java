package com.sotaynauan.ai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.sotaynauan.ai.data.local.entity.ShoppingItemEntity;

import java.util.List;

@Dao
public interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY category ASC, status ASC, name ASC")
    List<ShoppingItemEntity> getItems();

    @Query("SELECT * FROM shopping_items WHERE committed = :committed ORDER BY category ASC, status ASC, name ASC")
    List<ShoppingItemEntity> getItemsByCommitted(boolean committed);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<ShoppingItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ShoppingItemEntity item);

    @Query("DELETE FROM shopping_items")
    void clear();

    @Query("DELETE FROM shopping_items WHERE committed = :committed")
    void clearByCommitted(boolean committed);

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    void deleteById(String itemId);
}
