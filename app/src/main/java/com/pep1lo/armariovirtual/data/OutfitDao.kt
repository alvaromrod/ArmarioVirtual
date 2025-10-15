package com.pep1lo.armariovirtual.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Insert
    suspend fun insertOutfit(outfit: Outfit): Long

    @Insert
    suspend fun insertOutfitClothingLink(link: OutfitClothingLink)

    @Update
    suspend fun updateOutfit(outfit: Outfit)

    @Delete
    suspend fun deleteOutfit(outfit: Outfit)

    @Transaction
    @Query("SELECT * FROM outfits ORDER BY id DESC")
    fun getAllOutfitsWithItems(): Flow<List<OutfitWithItems>>

    @Transaction
    @Query("SELECT * FROM outfits WHERE id = :outfitId")
    fun getOutfitWithItemsById(outfitId: Int): Flow<OutfitWithItems?>

    @Query("DELETE FROM outfit_clothing_link WHERE outfitId = :outfitId")
    suspend fun deleteLinksForOutfit(outfitId: Int)

    // --- FUNCIONES DE BACKUP ---
    @Query("SELECT * FROM outfits")
    suspend fun getAllOutfitsForBackup(): List<Outfit>

    @Query("SELECT * FROM outfit_clothing_link")
    suspend fun getAllLinksForBackup(): List<OutfitClothingLink>

    @Query("DELETE FROM outfits")
    suspend fun clearAllOutfits()

    @Query("DELETE FROM outfit_clothing_link")
    suspend fun clearAllLinks()

    // --- NUEVA FUNCIÓN PARA RESETEAR ESTADÍSTICAS ---
    @Query("UPDATE outfits SET wearCount = 0, lastWornDate = null")
    suspend fun resetWearCount()
}
