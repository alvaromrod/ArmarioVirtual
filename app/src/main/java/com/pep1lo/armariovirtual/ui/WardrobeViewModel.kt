package com.pep1lo.armariovirtual.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pep1lo.armariovirtual.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WardrobeViewModel(
    private val clothingItemDao: ClothingItemDao,
    private val outfitDao: OutfitDao
) : ViewModel() {

    private val neutralColors = setOf(
        "Negro", "Blanco", "Gris", "Beige", "Marrón", "Marron", "Crema",
        "Azul marino", "Navy"
    )

    val allItems: StateFlow<List<ClothingItem>> = clothingItemDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _generatedOutfit = MutableStateFlow<List<ClothingItem>>(emptyList())
    val generatedOutfit: StateFlow<List<ClothingItem>> = _generatedOutfit.asStateFlow()

    val savedOutfits: StateFlow<List<OutfitWithItems>> = outfitDao.getAllOutfitsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val wardrobeStats: StateFlow<WardrobeStats> = allItems.map { items ->
        val byUsageDesc = items.sortedByDescending { it.usageCount }
        val top5Most = byUsageDesc.take(5)
        val top5Least = items.sortedBy { it.usageCount }.take(5)
        val colors = items.groupBy { it.color }.mapValues { it.value.size }
        val styles = items.groupBy { it.style.displayName }.mapValues { it.value.size }
        WardrobeStats(
            top5MostWornItems = top5Most,
            top5LeastWornItems = top5Least,
            colorDistribution = colors,
            styleDistribution = styles,
            allItemsByUsageDesc = byUsageDesc
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WardrobeStats())

    private val _currentItem = MutableStateFlow<ClothingItem?>(null)
    val currentItem: StateFlow<ClothingItem?> = _currentItem.asStateFlow()

    private val _currentOutfit = MutableStateFlow<OutfitWithItems?>(null)
    val currentOutfit: StateFlow<OutfitWithItems?> = _currentOutfit.asStateFlow()

    // Flow version (kept for streaming use if needed)
    fun getBackupData(): Flow<BackupData> = flow {
        val items = clothingItemDao.getAllItemsForBackup()
        val outfits = outfitDao.getAllOutfitsForBackup()
        val links = outfitDao.getAllLinksForBackup()
        emit(BackupData(items, outfits, links))
    }

    // REINTRODUCED: Single-shot suspend retrieval (used by legacy SettingsActivity)
    suspend fun getBackupDataOnce(): BackupData = withContext(Dispatchers.IO) {
        val items = clothingItemDao.getAllItemsForBackup()
        val outfits = outfitDao.getAllOutfitsForBackup()
        val links = outfitDao.getAllLinksForBackup()
        BackupData(items, outfits, links)
    }

    // REINTRODUCED: Single-shot suspend restore (used by legacy SettingsActivity)
    suspend fun restoreDataFromBackupOnce(backupData: BackupData) = withContext(Dispatchers.IO) {
        clothingItemDao.clearAllItems()
        outfitDao.clearAllOutfits()
        outfitDao.clearAllLinks()
        backupData.clothingItems.forEach { clothingItemDao.insertItem(it) }
        backupData.outfits.forEach { outfitDao.insertOutfit(it) }
        backupData.links.forEach { outfitDao.insertOutfitClothingLink(it) }
    }

    // Convenience wrapper for callers that don’t want suspend (optional; you already use it)
    fun restoreDataFromBackup(backupData: BackupData) {
        viewModelScope.launch {
            restoreDataFromBackupOnce(backupData)
        }
    }

    fun insertItem(item: ClothingItem) {
        viewModelScope.launch { clothingItemDao.insertItem(item) }
    }

    fun updateItem(item: ClothingItem) {
        viewModelScope.launch { clothingItemDao.updateItem(item) }
    }

    fun getItemById(id: Int) {
        viewModelScope.launch {
            clothingItemDao.getItemById(id).collect { _currentItem.value = it }
        }
    }

    fun clearCurrentItem() { _currentItem.value = null }

    fun getOutfitById(id: Int) {
        viewModelScope.launch {
            outfitDao.getOutfitWithItemsById(id).collect { _currentOutfit.value = it }
        }
    }

    fun clearCurrentOutfit() { _currentOutfit.value = null }

    fun updateOutfit(outfitId: Int, newItems: List<ClothingItem>) {
        viewModelScope.launch {
            outfitDao.deleteLinksForOutfit(outfitId)
            newItems.forEach { item ->
                outfitDao.insertOutfitClothingLink(
                    OutfitClothingLink(outfitId = outfitId, clothingItemId = item.id)
                )
            }
        }
    }

    fun deleteItem(item: ClothingItem) {
        viewModelScope.launch { clothingItemDao.deleteItem(item) }
    }

    fun deleteOutfit(outfitWithItems: OutfitWithItems) {
        viewModelScope.launch { outfitDao.deleteOutfit(outfitWithItems.outfit) }
    }

    fun toggleAvailability(item: ClothingItem) {
        viewModelScope.launch {
            clothingItemDao.updateItem(item.copy(isAvailable = !item.isAvailable))
        }
    }

    fun saveOutfit(items: List<ClothingItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val newOutfitId = outfitDao.insertOutfit(Outfit()).toInt()
            items.forEach { item ->
                outfitDao.insertOutfitClothingLink(
                    OutfitClothingLink(outfitId = newOutfitId, clothingItemId = item.id)
                )
            }
            _generatedOutfit.value = emptyList()
        }
    }

    fun markOutfitAsWorn(outfitWithItems: OutfitWithItems) {
        viewModelScope.launch {
            val updatedOutfit = outfitWithItems.outfit.copy(
                usageCount = outfitWithItems.outfit.usageCount + 1,
                lastWornDate = System.currentTimeMillis()
            )
            outfitDao.updateOutfit(updatedOutfit)
            outfitWithItems.items.forEach { clothingItem ->
                clothingItemDao.updateItem(
                    clothingItem.copy(usageCount = clothingItem.usageCount + 1)
                )
            }
        }
    }

    fun generateOutfit(season: Season, style: Style) {
        viewModelScope.launch {
            val allClothingItems = allItems.value.filter { it.isAvailable }

            // Exhaustive style support (avoid non-exhaustive when errors)
            val compatibleStyles = when (style) {
                Style.BASICO -> listOf(Style.BASICO, Style.PREPPY, Style.BOHO, Style.CASUAL, Style.SPORT, Style.CUTE)
                Style.PREPPY -> listOf(Style.PREPPY, Style.BASICO, Style.CASUAL)
                Style.BOHO -> listOf(Style.BOHO, Style.BASICO, Style.CASUAL)
                Style.SPORT -> listOf(Style.SPORT, Style.BASICO, Style.CASUAL)
                Style.CASUAL -> listOf(Style.CASUAL, Style.BASICO, Style.PREPPY, Style.BOHO)
                Style.CUTE -> listOf(Style.CUTE, Style.BASICO, Style.CASUAL)
            }

            val seasonsToInclude = when (season) {
                Season.VERANO -> listOf(Season.VERANO, Season.ENTRETIEMPO)
                Season.INVIERNO -> listOf(Season.INVIERNO, Season.ENTRETIEMPO)
                Season.ENTRETIEMPO -> listOf(Season.ENTRETIEMPO)
            }

            val filteredItems = allClothingItems.filter {
                it.season in seasonsToInclude && it.style in compatibleStyles
            }

            val tops = filteredItems.filter { it.category == Category.SUPERIOR }
            val bottoms = filteredItems.filter { it.category == Category.INFERIOR }
            val fullBody = filteredItems.filter { it.category == Category.COMPLETO }
            val shoes = allClothingItems.filter { it.features == "Zapatos" }
            val coats = allClothingItems.filter { it.category == Category.EXTERIOR }

            val finalOutfit = mutableListOf<ClothingItem>()

            if (fullBody.isNotEmpty() && (tops.isEmpty() || bottoms.isEmpty())) {
                fullBody.randomOrNull()?.let { finalOutfit.add(it) }
            } else {
                val neutralBottoms = bottoms.filter { it.color in neutralColors }
                val chosenBottom = (neutralBottoms.ifEmpty { bottoms }).randomOrNull()
                if (chosenBottom != null) {
                    finalOutfit.add(chosenBottom)
                    val chosenTop = if (chosenBottom.color in neutralColors) {
                        tops.randomOrNull()
                    } else {
                        val neutralTops = tops.filter { it.color in neutralColors }
                        (neutralTops.ifEmpty { tops }).randomOrNull()
                    }
                    chosenTop?.let { finalOutfit.add(it) }
                } else {
                    tops.randomOrNull()?.let { finalOutfit.add(it) }
                }
            }

            val neutralShoes = shoes.filter { it.color in neutralColors }
            (neutralShoes.ifEmpty { shoes }).randomOrNull()?.let { finalOutfit.add(it) }

            if (season == Season.INVIERNO || season == Season.ENTRETIEMPO) {
                val neutralCoats = coats.filter { it.color in neutralColors }
                (neutralCoats.ifEmpty { coats }).randomOrNull()?.let { finalOutfit.add(it) }
            }

            _generatedOutfit.value = finalOutfit
        }
    }
}