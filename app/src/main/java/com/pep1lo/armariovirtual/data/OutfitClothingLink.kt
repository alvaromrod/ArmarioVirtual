package com.pep1lo.armariovirtual.data

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Entity(
    tableName = "outfit_clothing_link",
    primaryKeys = ["outfitId", "clothingItemId"],
    // CORRECCIÓN: Se añaden índices para mejorar el rendimiento y eliminar los warnings de compilación.
    indices = [
        Index(value = ["outfitId"]),
        Index(value = ["clothingItemId"])
    ],
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
