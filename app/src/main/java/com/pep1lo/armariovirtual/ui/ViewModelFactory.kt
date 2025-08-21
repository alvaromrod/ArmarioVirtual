package com.pep1lo.armariovirtual.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pep1lo.armariovirtual.data.ClothingItemDao
import com.pep1lo.armariovirtual.data.OutfitDao

class ViewModelFactory(
    private val clothingItemDao: ClothingItemDao,
    private val outfitDao: OutfitDao // Añadimos el nuevo DAO
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WardrobeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pasamos ambos DAOs al constructor del ViewModel
            return WardrobeViewModel(clothingItemDao, outfitDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
