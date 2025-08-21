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
        val styles = items.groupBy { it.style }.mapValues { it.value.size }
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
                val updatedItem = clothingItem.copy(
                    usageCount = clothingItem.usageCount + 1
                )
                clothingItemDao.updateItem(updatedItem)
            }
        }
    }

    fun generateOutfit(season: String, style: String) {
        viewModelScope.launch {
            val allClothingItems = allItems.value.filter { it.isAvailable }
            val compatibleStyles = when (style) {
                "Básico" -> listOf("Básico", "Preppy", "Boho")
                "Preppy" -> listOf("Básico", "Preppy")
                "Boho" -> listOf("Básico", "Boho")
                else -> listOf(style)
            }

            // --- INICIO DE LA MODIFICACIÓN: Lógica de temporadas flexible ---
            // Creamos una lista de temporadas válidas según la selección del usuario.
            // Si se elige "Verano" o "Invierno", se incluyen también las prendas de "Entretiempo".
            val seasonsToInclude = when (season) {
                "Verano" -> listOf("Verano", "Entretiempo")
                "Invierno" -> listOf("Invierno", "Entretiempo")
                else -> listOf(season) // Para "Entretiempo" y otros casos, solo se usa la temporada seleccionada.
            }

            val filteredItems = allClothingItems.filter {
                it.season in seasonsToInclude && it.style in compatibleStyles
            }
            // --- FIN DE LA MODIFICACIÓN ---

            val tops = filteredItems.filter { it.category == "Superior" }
            val bottoms = filteredItems.filter { it.category == "Inferior" }
            val fullBody = filteredItems.filter { it.category == "Completo" }
            val shoes = allClothingItems.filter { it.features == "Zapatos" }
            val coats = allClothingItems.filter { it.category == "Exterior" }
            val finalOutfit = mutableListOf<ClothingItem>()
            if (fullBody.isNotEmpty() && (tops.isEmpty() || bottoms.isEmpty())) {
                fullBody.randomOrNull()?.let { finalOutfit.add(it) }
            } else {
                val neutralBottoms = bottoms.filter { it.color in DataSource.neutralColors }
                val chosenBottom = if (neutralBottoms.isNotEmpty()) neutralBottoms.random() else bottoms.randomOrNull()
                if (chosenBottom != null) {
                    finalOutfit.add(chosenBottom)
                    val chosenTop = if (chosenBottom.color in DataSource.neutralColors) {
                        tops.randomOrNull()
                    } else {
                        val neutralTops = tops.filter { it.color in DataSource.neutralColors }
                        if (neutralTops.isNotEmpty()) neutralTops.random() else tops.randomOrNull()
                    }
                    chosenTop?.let { finalOutfit.add(it) }
                } else {
                    tops.randomOrNull()?.let { finalOutfit.add(it) }
                }
            }
            val neutralShoes = shoes.filter { it.color in DataSource.neutralColors }
            if (neutralShoes.isNotEmpty()) {
                finalOutfit.add(neutralShoes.random())
            } else {
                shoes.randomOrNull()?.let { finalOutfit.add(it) }
            }
            if ((season == "Invierno" || season == "Entretiempo")) {
                val neutralCoats = coats.filter { it.color in DataSource.neutralColors }
                if (neutralCoats.isNotEmpty()) {
                    finalOutfit.add(neutralCoats.random())
                } else {
                    coats.randomOrNull()?.let { finalOutfit.add(it) }
                }
            }
            _generatedOutfit.value = finalOutfit
        }
    }
}
