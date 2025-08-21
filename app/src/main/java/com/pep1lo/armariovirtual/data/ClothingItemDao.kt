package com.pep1lo.armariovirtual.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClothingItem)

    @Delete
    suspend fun deleteItem(item: ClothingItem)

    @Update
    suspend fun updateItem(item: ClothingItem)

    @Query("SELECT * FROM clothing_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<ClothingItem>>

    @Query("SELECT * FROM clothing_items WHERE id = :id")
    fun getItemById(id: Int): Flow<ClothingItem?>

    // --- FUNCIONES DE BACKUP RESTAURADAS ---
    @Query("SELECT * FROM clothing_items")
    suspend fun getAllItemsForBackup(): List<ClothingItem>

    @Query("DELETE FROM clothing_items")
    suspend fun clearAllItems()
    // --- FIN DE LA CORRECCIÓN ---
}
