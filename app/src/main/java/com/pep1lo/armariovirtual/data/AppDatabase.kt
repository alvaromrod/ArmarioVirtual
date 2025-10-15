package com.pep1lo.armariovirtual.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ClothingItem::class, Outfit::class, OutfitClothingLink::class],
    version = 3, // DB version incremented due to type changes
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun outfitDao(): OutfitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wardrobe_database"
                )
                // NOTE: fallbackToDestructiveMigration will wipe user data on schema changes.
                // For production, implement a proper migration strategy!
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}