package com.pep1lo.armariovirtual.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pep1lo.armariovirtual.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WardrobeViewModel(
    private val clothingItemDao: ClothingItemDao,
    private val outfitDao: OutfitDao
) : ViewModel() {

    // --- State Flows for UI ---
    val allItems: StateFlow<List<ClothingItem>> = clothingItemDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOutfits: StateFlow<List<OutfitWithItems>> = outfitDao.getAllOutfitsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _generatedOutfit = MutableStateFlow<List<ClothingItem>>(emptyList())
    val generatedOutfit: StateFlow<List<ClothingItem>> = _generatedOutfit.asStateFlow()

    private val _currentOutfit = MutableStateFlow<OutfitWithItems?>(null)
    val currentOutfit: StateFlow<OutfitWithItems?> = _currentOutfit.asStateFlow()

    private val _editingItem = MutableStateFlow<ClothingItem?>(null)
    val editingItem: StateFlow<ClothingItem?> = _editingItem.asStateFlow()

    val wardrobeStats: StateFlow<WardrobeStats> = combine(allItems, allOutfits) { items, _ ->
        WardrobeStats(
            top5MostWornItems = items.sortedByDescending { it.wearCount }.take(5),
            colorDistribution = items.groupingBy { it.color }.eachCount(),
            styleDistribution = items.groupingBy { it.style.displayName }.eachCount()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WardrobeStats())


    // --- Backup and Restore ---
    suspend fun getBackupData(): BackupData {
        return BackupData(
            clothingItems = clothingItemDao.getAllItemsForBackup(),
            outfits = outfitDao.getAllOutfitsForBackup(),
            links = outfitDao.getAllLinksForBackup()
        )
    }

    fun restoreDataFromBackup(backupData: BackupData) {
        viewModelScope.launch {
            clothingItemDao.clearAllItems()
            outfitDao.clearAllOutfits()
            outfitDao.clearAllLinks()
            backupData.clothingItems.forEach { clothingItemDao.insertItem(it) }
            backupData.outfits.forEach { outfitDao.insertOutfit(it) }
            backupData.links.forEach { outfitDao.insertOutfitClothingLink(it) }
        }
    }

    // --- Clothing Item Operations ---
    fun insertItem(item: ClothingItem) {
        viewModelScope.launch {
            clothingItemDao.insertItem(item)
        }
    }

    fun updateItem(item: ClothingItem) {
        viewModelScope.launch {
            clothingItemDao.updateItem(item)
        }
    }

    fun deleteItem(item: ClothingItem) {
        viewModelScope.launch {
            clothingItemDao.deleteItem(item)
        }
    }

    fun getItemById(id: Int) {
        viewModelScope.launch {
            _editingItem.value = clothingItemDao.getItemById(id).first()
        }
    }

    fun setEditingImageUri(uri: String) {
        _editingItem.value = _editingItem.value?.copy(imageUri = uri)
    }

    fun clearEditingItem() {
        _editingItem.value = null
    }

    fun toggleAvailability(item: ClothingItem) {
        viewModelScope.launch {
            updateItem(item.copy(isAvailable = !item.isAvailable))
        }
    }

    // --- Outfit Operations ---
    fun saveOutfit(items: List<ClothingItem>) {
        viewModelScope.launch {
            val outfit = Outfit(lastWornDate = System.currentTimeMillis())
            val outfitId = outfitDao.insertOutfit(outfit)
            items.forEach { item ->
                val link = OutfitClothingLink(outfitId = outfitId.toInt(), clothingItemId = item.id)
                outfitDao.insertOutfitClothingLink(link)
            }
        }
    }

    fun updateOutfit(outfitId: Int, items: List<ClothingItem>) {
        viewModelScope.launch {
            outfitDao.getOutfitWithItemsById(outfitId).first()?.outfit?.let {
                outfitDao.updateOutfit(it.copy(lastWornDate = System.currentTimeMillis()))
            }
            outfitDao.deleteLinksForOutfit(outfitId)
            items.forEach { item ->
                val link = OutfitClothingLink(outfitId = outfitId, clothingItemId = item.id)
                outfitDao.insertOutfitClothingLink(link)
            }
        }
    }


    fun getOutfitById(id: Int) {
        viewModelScope.launch {
            _currentOutfit.value = outfitDao.getOutfitWithItemsById(id).first()
        }
    }

    fun clearCurrentOutfit() {
        _currentOutfit.value = null
    }

    fun markOutfitAsWorn(outfit: OutfitWithItems) {
        viewModelScope.launch {
            outfitDao.updateOutfit(outfit.outfit.copy(wearCount = outfit.outfit.wearCount + 1))
            outfit.items.forEach { item ->
                clothingItemDao.updateItem(item.copy(wearCount = item.wearCount + 1))
            }
        }
    }

    fun deleteOutfit(outfit: OutfitWithItems) {
        viewModelScope.launch {
            outfitDao.deleteOutfit(outfit.outfit)
        }
    }

    // --- Outfit Generator Logic ---
    fun clearGeneratedOutfit() {
        _generatedOutfit.value = emptyList()
    }

    fun generateOutfit(season: Season, style1: Style, style2: Style?) {
        val availableItems = allItems.value.filter { it.isAvailable && it.season == season }
        val potentialItems = availableItems.filter {
            it.style == style1 || it.style == style2 || it.style == Style.BASICO
        }

        val fullBody = potentialItems.find { it.category == Category.COMPLETO }?.let { listOf(it) }
        val top = potentialItems.filter { it.category == Category.SUPERIOR }
        val bottom = potentialItems.filter { it.category == Category.INFERIOR }
        val coat = potentialItems.filter { it.category == Category.EXTERIOR }

        val neutralColors = DataSource.allColors.filter { it.isNeutral }.map { it.name }

        if (fullBody?.isNotEmpty() == true) {
            _generatedOutfit.value = fullBody + (coat.randomOrNull()?.let { listOf(it) } ?: emptyList())
            return
        }

        if (top.isNotEmpty() && bottom.isNotEmpty()) {
            val selectedBottom = bottom.shuffled().firstOrNull { it.color in neutralColors } ?: bottom.random()
            val selectedTop = if (selectedBottom.color in neutralColors) {
                top.random()
            } else {
                top.shuffled().firstOrNull { it.color in neutralColors } ?: top.random()
            }
            val selectedCoat = coat.shuffled().firstOrNull { it.color in neutralColors } ?: coat.randomOrNull()

            _generatedOutfit.value = listOfNotNull(selectedTop, selectedBottom, selectedCoat)
            return
        }
        _generatedOutfit.value = emptyList()
    }

    // --- NUEVA FUNCIÓN PARA RESETEAR ESTADÍSTICAS ---
    fun resetStats() {
        viewModelScope.launch {
            clothingItemDao.resetWearCount()
            outfitDao.resetWearCount()
        }
    }
}

