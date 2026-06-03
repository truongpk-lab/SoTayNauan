package com.sotaynauan.ai.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sotaynauan.ai.data.local.dao.CookingPlanDao;
import com.sotaynauan.ai.data.local.dao.IngredientDao;
import com.sotaynauan.ai.data.local.dao.PantryDao;
import com.sotaynauan.ai.data.local.dao.RecipeDao;
import com.sotaynauan.ai.data.local.dao.ShoppingItemDao;
import com.sotaynauan.ai.data.local.dao.CommunityDao;
import com.sotaynauan.ai.data.local.entity.CommunityCommentEntity;
import com.sotaynauan.ai.data.local.entity.CommunityFriendEntity;
import com.sotaynauan.ai.data.local.entity.CommunityShareEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.CookingPlanIngredientEntity;
import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.local.entity.InventoryTransactionEntity;
import com.sotaynauan.ai.data.local.entity.PantryBatchEntity;
import com.sotaynauan.ai.data.local.entity.PantryStockEntity;
import com.sotaynauan.ai.data.local.entity.RecipeEntity;
import com.sotaynauan.ai.data.local.entity.RecipeIngredientEntity;
import com.sotaynauan.ai.data.local.entity.SharedCookingPlanEntity;
import com.sotaynauan.ai.data.local.entity.ShoppingItemEntity;

@Database(entities = {
        RecipeEntity.class,
        RecipeIngredientEntity.class,
        IngredientEntity.class,
        PantryStockEntity.class,
        PantryBatchEntity.class,
        CookingPlanEntity.class,
        CookingPlanIngredientEntity.class,
        ShoppingItemEntity.class,
        InventoryTransactionEntity.class,
        CommunityFriendEntity.class,
        CommunityCommentEntity.class,
        CommunityShareEntity.class,
        SharedCookingPlanEntity.class
}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract RecipeDao recipeDao();
    public abstract IngredientDao ingredientDao();
    public abstract PantryDao pantryDao();
    public abstract CookingPlanDao cookingPlanDao();
    public abstract ShoppingItemDao shoppingItemDao();
    public abstract CommunityDao communityDao();

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `ingredients` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `category` TEXT NOT NULL, `baseUnit` TEXT NOT NULL, `aliasesJson` TEXT, `imageUrl` TEXT, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ingredients_normalizedName` ON `ingredients` (`normalizedName`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ingredients_category` ON `ingredients` (`category`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `recipe_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, `ingredientId` TEXT NOT NULL, `amount` REAL NOT NULL, `unit` TEXT NOT NULL, `isOptional` INTEGER NOT NULL, `note` TEXT, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_ingredientId` ON `recipe_ingredients` (`ingredientId`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `pantry_stocks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ingredientId` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `reservedAmount` REAL NOT NULL, `baseUnit` TEXT NOT NULL, `lastUpdatedAt` INTEGER NOT NULL, FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_stocks_ingredientId` ON `pantry_stocks` (`ingredientId`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `pantry_batches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ingredientId` TEXT NOT NULL, `remainingAmount` REAL NOT NULL, `baseUnit` TEXT NOT NULL, `source` TEXT, `boughtAt` INTEGER NOT NULL, `expiryAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pantry_batches_ingredientId` ON `pantry_batches` (`ingredientId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pantry_batches_expiryAt` ON `pantry_batches` (`expiryAt`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `cooking_plans` (`id` TEXT NOT NULL, `recipeId` INTEGER NOT NULL, `targetServings` INTEGER NOT NULL, `servingMultiplier` REAL NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `startedAt` INTEGER, `completedAt` INTEGER, PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cooking_plans_recipeId` ON `cooking_plans` (`recipeId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cooking_plans_status` ON `cooking_plans` (`status`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `cooking_plan_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planId` TEXT NOT NULL, `ingredientId` TEXT NOT NULL, `requiredAmount` REAL NOT NULL, `homeSelectedAmount` REAL NOT NULL, `reservedAmount` REAL NOT NULL, `purchasedAmount` REAL NOT NULL, `consumedAmount` REAL NOT NULL, `missingAmount` REAL NOT NULL, `baseUnit` TEXT NOT NULL, `prepareStatus` TEXT NOT NULL, `userNote` TEXT, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`planId`) REFERENCES `cooking_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cooking_plan_ingredients_planId` ON `cooking_plan_ingredients` (`planId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cooking_plan_ingredients_ingredientId` ON `cooking_plan_ingredients` (`ingredientId`)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cooking_plan_ingredients_planId_ingredientId` ON `cooking_plan_ingredients` (`planId`, `ingredientId`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ingredientId` TEXT NOT NULL, `planId` TEXT, `batchId` INTEGER, `type` TEXT NOT NULL, `deltaAmount` REAL NOT NULL, `baseUnit` TEXT NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_transactions_ingredientId` ON `inventory_transactions` (`ingredientId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_transactions_planId` ON `inventory_transactions` (`planId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_transactions_type` ON `inventory_transactions` (`type`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_transactions_createdAt` ON `inventory_transactions` (`createdAt`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `shared_cooking_plans` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `friendId` TEXT NOT NULL, `permission` TEXT, `status` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_cooking_plans_planId` ON `shared_cooking_plans` (`planId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_cooking_plans_friendId` ON `shared_cooking_plans` (`friendId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shared_cooking_plans_status` ON `shared_cooking_plans` (`status`)");

            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `planId` TEXT");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `ingredientId` TEXT");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `displayName` TEXT");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `requiredAmount` REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `boughtAmount` REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `baseUnit` TEXT");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `shopping_items` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE `shopping_items` SET `displayName` = `name`, `requiredAmount` = `amount`, `boughtAmount` = CASE WHEN `status` = 'BOUGHT' THEN `amount` ELSE 0 END, `baseUnit` = `unit`, `updatedAt` = `updatedAtMillis`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_planId` ON `shopping_items` (`planId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_ingredientId` ON `shopping_items` (`ingredientId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_status` ON `shopping_items` (`status`)");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `community_comments` (`id` TEXT NOT NULL, `shareId` TEXT, `authorName` TEXT, `body` TEXT, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_community_comments_shareId` ON `community_comments` (`shareId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_community_comments_createdAtMillis` ON `community_comments` (`createdAtMillis`)");
            database.execSQL("UPDATE `community_friends` SET `status` = 'invite_sent' WHERE `status` = 'invited' AND `note` LIKE '%Đã gửi%'");
            database.execSQL("UPDATE `community_friends` SET `status` = 'invite_received' WHERE `status` = 'invited'");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "so_tay_nau_an.db")
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                            .build();
                }
            }
        }
        return instance;
    }
}
