package com.pep1lo.armariovirtual.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Clase que representa un Outfit completo junto con la lista de sus prendas.
 */
data class OutfitWithItems(
    @Embedded val outfit: Outfit,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = OutfitClothingLink::class,
            parentColumn = "outfitId",
            entityColumn = "clothingItemId"
        )
    )
    val items: List<ClothingItem>
)
