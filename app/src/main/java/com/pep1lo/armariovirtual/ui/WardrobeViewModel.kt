package com.pep1lo.armariovirtual.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pep1lo.armariovirtual.data.BackupData
import com.pep1lo.armariovirtual.data.Category
import com.pep1lo.armariovirtual.data.ClothingItem
import com.pep1lo.armariovirtual.data.ClothingItemDao
import com.pep1lo.armariovirtual.data.Outfit
import com.pep1lo.armariovirtual.data.OutfitClothingLink
import com.pep1lo.armariovirtual.data.OutfitDao
import com.pep1lo.armariovirtual.data.OutfitWithItems
import com.pep1lo.armariovirtual.data.Season
import com.pep1lo.armariovirtual.data.Style
import com.pep1lo.armariovirtual.data.WardrobeStats
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

    val allItems: StateFlow<List<ClothingItem>> = clothingItemDao.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _generatedOutfit = MutableStateFlow<List<ClothingItem>>(emptyList())
    val generatedOutfit: StateFlow<List<ClothingItem>> = _generatedOutfit.asStateFlow()

    val savedOutfits: StateFlow<List<OutfitWithItems>> = outfitDao.getAllOutfitsWithItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val wardrobeStats: StateFlow<WardrobeStats> = allItems.map { items ->
        val top5 = items.filter { it.usageCount > 0 }.sortedByDescending { it.usageCount }.take(5)
        val colors = items.groupBy { it.color }.mapValues { it.value.size }
        val styles = items.groupBy { it.style.displayName }.mapValues { it.value.size }
        WardrobeStats(top5, colors, styles)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = WardrobeStats()
    )

    private val _currentItem = MutableStateFlow<ClothingItem?>(null)
    val currentItem: StateFlow<ClothingItem?> = _currentItem.asStateFlow()

    private val _currentOutfit = MutableStateFlow<OutfitWithItems?>(null)
    val currentOutfit: StateFlow<OutfitWithItems?> = _currentOutfit.asStateFlow()

    fun getBackupData(): Flow<BackupData> = flow {
        val items = clothingItemDao.getAllItemsForBackup()
        val outfits = outfitDao.getAllOutfitsForBackup()
        val links = outfitDao.getAllLinksForBackup()
        emit(BackupData(items, outfits, links))
    }

    suspend fun getBackupDataOnce(): BackupData = withContext(Dispatchers.IO) {
        val items = clothingItemDao.getAllItemsForBackup()
        val outfits = outfitDao.getAllOutfitsForBackup()
        val links = outfitDao.getAllLinksForBackup()
        BackupData(items, outfits, links)
    }

    suspend fun restoreDataFromBackupOnce(backupData: BackupData) = withContext(Dispatchers.IO) {
        clothingItemDao.clearAllItems()
        outfitDao.clearAllOutfits()
        outfitDao.clearAllLinks()
        backupData.clothingItems.forEach { clothingItemDao.insertItem(it) }
        backupData.outfits.forEach { outfitDao.insertOutfit(it) }
        backupData.links.forEach { outfitDao.insertOutfitClothingLink(it) }
    }

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

    fun getItemById(id: Int) {
        viewModelScope.launch {
            clothingItemDao.getItemById(id).collect {
                _currentItem.value = it
            }
        }
    }

    fun clearCurrentItem() {
        _currentItem.value = null
    }

    fun getOutfitById(id: Int) {
        viewModelScope.launch {
            outfitDao.getOutfitWithItemsById(id).collect {
                _currentOutfit.value = it
            }
        }
    }

    fun clearCurrentOutfit() {
        _currentOutfit.value = null
    }

    fun updateOutfit(outfitId: Int, newItems: List<ClothingItem>) {
        viewModelScope.launch {
            outfitDao.deleteLinksForOutfit(outfitId)
            newItems.forEach { item ->
                val link = OutfitClothingLink(outfitId = outfitId, clothingItemId = item.id)
                outfitDao.insertOutfitClothingLink(link)
            }
        }
    }

    fun deleteItem(item: ClothingItem) {
        viewModelScope.launch {
            clothingItemDao.deleteItem(item)
        }
    }

    fun deleteOutfit(outfitWithItems: OutfitWithItems) {
        viewModelScope.launch {
            outfitDao.deleteOutfit(outfitWithItems.outfit)
        }
    }

    fun toggleAvailability(item: ClothingItem) {
        viewModelScope.launch {
            val updatedItem = item.copy(isAvailable = !item.isAvailable)
            clothingItemDao.updateItem(updatedItem)
        }
    }

    fun saveOutfit(items: List<ClothingItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val newOutfit = Outfit()
            val newOutfitId = outfitDao.insertOutfit(newOutfit)
            items.forEach { item ->
                val link = OutfitClothingLink(outfitId = newOutfitId.toInt(), clothingItemId = item.id)
                outfitDao.insertOutfitClothingLink(link)
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
                val updatedItem = clothingItem.copy(usageCount = clothingItem.usageCount + 1)
                clothingItemDao.updateItem(updatedItem)
            }
        }
    }

    fun generateOutfit(season: Season, style: Style, allowMixAndMatch: Boolean = false) {
        viewModelScope.launch {
            val allClothingItems = allItems.value.filter { it.isAvailable }

            fun filterItems(
                seasonFilter: (Season) -> Boolean,
                styleFilter: (Style) -> Boolean
            ) = allClothingItems.filter { item ->
                seasonFilter(item.season) && styleFilter(item.style)
            }

            var filteredItems = filterItems({ it == season }, { it == style })

            if (allowMixAndMatch && filteredItems.size < 2) {
                filteredItems = filterItems({ it == season }, { _ -> true })
                if (filteredItems.size < 2) {
                    filteredItems = filterItems({ _ -> true }, { it == style })
                    if (filteredItems.size < 2) {
                        filteredItems = allClothingItems
                    }
                }
            }

            val tops = filteredItems.filter { it.category == Category.SUPERIOR }.shuffled()
            val bottoms = filteredItems.filter { it.category == Category.INFERIOR }.shuffled()
            val fullBody = filteredItems.filter { it.category == Category.COMPLETO }.shuffled()
            val coats = filteredItems.filter { it.category == Category.EXTERIOR }.shuffled()
            val shoes = filteredItems.filter { it.features == "Zapatos" }.shuffled()

            val outfit = mutableListOf<ClothingItem>()
            if (fullBody.isNotEmpty()) {
                outfit.add(fullBody.first())
                if (coats.isNotEmpty()) outfit.add(coats.first())
            } else {
                if (tops.isNotEmpty()) outfit.add(tops.first())
                if (bottoms.isNotEmpty()) outfit.add(bottoms.first())
                if (coats.isNotEmpty()) outfit.add(coats.first())
            }
            if (shoes.isNotEmpty()) outfit.add(shoes.first())

            _generatedOutfit.value = outfit
        }
    }
}