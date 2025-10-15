// Improved WardrobeViewModel.kt

package com.pep1lo.armariovirtual.ui

import androidx.lifecycle.ViewModel
import com.pep1lo.armariovirtual.model.OutfitGenerator

class WardrobeViewModel : ViewModel() {
    private val outfitGenerator = OutfitGenerator()

    // New option to allow mix and match outfits
    var allowMixAndMatch: Boolean = false

    fun generateOutfit() {
        if (allowMixAndMatch) {
            // Logic for generating mixed outfits
            outfitGenerator.generateMixedOutfit()
        } else {
            // Default outfit generation
            outfitGenerator.generateOutfit()
        }
    }
}