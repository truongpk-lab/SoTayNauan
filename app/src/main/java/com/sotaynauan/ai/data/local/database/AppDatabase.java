package com.sotaynauan.ai.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.sotaynauan.ai.data.local.dao.RecipeDao;
import com.sotaynauan.ai.data.local.dao.ShoppingItemDao;
import com.sotaynauan.ai.data.local.dao.CommunityDao;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.local.entity.ShoppingItemEntity;

@Database(entities = {
        RecipeEntity.class,
        ShoppingItemEntity.class,
        CommunityFriendEntity.class,
        CommunityShareEntity.class
}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract RecipeDao recipeDao();
    public abstract ShoppingItemDao shoppingItemDao();
    public abstract CommunityDao communityDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "so_tay_nau_an.db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
