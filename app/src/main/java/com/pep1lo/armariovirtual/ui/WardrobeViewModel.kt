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
import kotlin.random.Random

class WardrobeViewModel(
    private val clothingItemDao: ClothingItemDao,
    private val outfitDao: OutfitDao
) : ViewModel() {

    private data class Pools(
        val tops: List<ClothingItem>,
        val bottoms: List<ClothingItem>,
        val full: List<ClothingItem>,
        val coats: List<ClothingItem>
    )

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

    private enum class Route { FULL_BODY, TWO_PIECE }

    private var lastGeneratedIds: Set<Int> = emptySet()

    fun generateOutfit(season: Season, style: Style, allowMixAndMatch: Boolean = false) {
        viewModelScope.launch {
            val available = allItems.value.filter { it.isAvailable }

            fun filterItems(
                seasonOk: (Season) -> Boolean,
                styleOk: (Style) -> Boolean
            ): List<ClothingItem> = available.filter { i -> seasonOk(i.season) && styleOk(i.style) }

            val strictSeason: (Season) -> Boolean = { it == season }
            val strictStyle: (Style) -> Boolean = { it == style }
            val anySeason: (Season) -> Boolean = { true }
            val anyStyle: (Style) -> Boolean = { true }

            val filterOptions = listOf(
                Pair(strictSeason, strictStyle),
                Pair(strictSeason, anyStyle),
                Pair(anySeason, strictStyle),
                Pair(anySeason, anyStyle)
            )

            fun buildPools(items: List<ClothingItem>) = Pools(
                tops = items.filter { it.category == Category.SUPERIOR },
                bottoms = items.filter { it.category == Category.INFERIOR },
                full = items.filter { it.category == Category.COMPLETO },
                coats = items.filter { it.category == Category.EXTERIOR }
            )

            fun randomDifferent(list: List<ClothingItem>, exclude: Set<Int>): ClothingItem? {
                if (list.isEmpty()) return null
                val nonExcluded = list.filter { it.id !in exclude }
                return (nonExcluded.ifEmpty { list }).random()
            }

            var chosen: List<ClothingItem> = emptyList()
            val attemptFilters = if (allowMixAndMatch) filterOptions else listOf(filterOptions.first())
            var found = false

            outerLoop@ for ((seasonOk, styleOk) in attemptFilters) {
                val filteredItems = filterItems(seasonOk, styleOk)
                val pools = buildPools(filteredItems)

                val twoPiecePossible = pools.tops.isNotEmpty() && pools.bottoms.isNotEmpty()
                val fullBodyPossible = pools.full.isNotEmpty()
                if (!twoPiecePossible && !fullBodyPossible) continue

                val routes = mutableListOf<Route>()
                if (twoPiecePossible) routes += Route.TWO_PIECE
                if (fullBodyPossible) routes += Route.FULL_BODY

                for (attempt in 0 until 12) {
                    val route = routes.random()
                    val picks = when (route) {
                        Route.TWO_PIECE -> {
                            val top = randomDifferent(pools.tops, lastGeneratedIds)
                            val bottom = randomDifferent(
                                pools.bottoms,
                                lastGeneratedIds + (top?.id ?: -1)
                            )
                            if (top != null && bottom != null) {
                                val base = mutableListOf(top, bottom)
                                if (season != Season.VERANO && pools.coats.isNotEmpty()) {
                                    if (Random.nextFloat() < 0.25f) {
                                        val coat = randomDifferent(
                                            pools.coats,
                                            lastGeneratedIds + base.map { it.id }.toSet()
                                        )
                                        if (coat != null) base += coat
                                    }
                                }
                                base
                            } else {
                                emptyList()
                            }
                        }
                        Route.FULL_BODY -> {
                            val one = randomDifferent(pools.full, lastGeneratedIds)
                            if (one != null) {
                                val base = mutableListOf(one)
                                if (season != Season.VERANO && pools.coats.isNotEmpty()) {
                                    if (Random.nextFloat() < 0.2f) {
                                        val coat = randomDifferent(
                                            pools.coats,
                                            lastGeneratedIds + base.map { it.id }.toSet()
                                        )
                                        if (coat != null) base += coat
                                    }
                                }
                                base
                            } else {
                                emptyList()
                            }
                        }
                    }

                    if (picks.isNotEmpty() && picks.map { it.id }.toSet() != lastGeneratedIds) {
                        chosen = picks
                        found = true
                        break
                    }
                }

                if (found) break@outerLoop
            }

            if (!found) {
                val anyItems = available
                val anyFull = anyItems.firstOrNull { it.category == Category.COMPLETO }
                if (anyFull != null) {
                    chosen = listOf(anyFull)
                } else {
                    val anyTop = anyItems.firstOrNull { it.category == Category.SUPERIOR }
                    val anyBottom = anyItems.firstOrNull { it.category == Category.INFERIOR && it.id != anyTop?.id }
                    if (anyTop != null && anyBottom != null) {
                        chosen = listOf(anyTop, anyBottom)
                    } else if (anyItems.isNotEmpty()) {
                        chosen = listOf(anyItems.random())
                    }
                }
            }

            if (chosen.size == 1 && chosen.first().category == Category.SUPERIOR) {
                val bottomAny = available.firstOrNull { it.category == Category.INFERIOR && it.id != chosen.first().id }
                if (bottomAny != null) {
                    chosen = listOf(chosen.first(), bottomAny)
                } else {
                    val fullAny = available.firstOrNull { it.category == Category.COMPLETO }
                    if (fullAny != null) chosen = listOf(fullAny)
                }
            }

            lastGeneratedIds = chosen.map { it.id }.toSet()
            _generatedOutfit.value = chosen
        }
    }
}