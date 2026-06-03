package com.sotaynauan.ai.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sotaynauan.ai.data.local.entity.IngredientEntity;

import java.util.List;

@Dao
public interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE isActive = 1 ORDER BY name")
    List<IngredientEntity> getAllActive();

    @Query("SELECT * FROM ingredients ORDER BY name")
    List<IngredientEntity> getAll();

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    IngredientEntity findById(String id);

    @Query("SELECT * FROM ingredients WHERE normalizedName = :normalizedName LIMIT 1")
    IngredientEntity findByNormalizedName(String normalizedName);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void upsert(IngredientEntity entity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void upsertAll(List<IngredientEntity> entities);

    @Update
    void update(IngredientEntity entity);
}
