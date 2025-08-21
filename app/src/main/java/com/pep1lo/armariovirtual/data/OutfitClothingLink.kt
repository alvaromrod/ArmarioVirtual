package com.pep1lo.armariovirtual.data

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "outfit_clothing_link",
    primaryKeys = ["outfitId", "clothingItemId"],
    foreignKeys = [
        ForeignKey(
            entity = Outfit::class,
            parentColumns = ["id"],
            childColumns = ["outfitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClothingItem::class,
            parentColumns = ["id"],
            childColumns = ["clothingItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OutfitClothingLink(
    val outfitId: Int,
    val clothingItemId: Int
)
