package com.pep1lo.armariovirtual.data

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BackupData(
    val clothingItems: List<ClothingItem>,
    val outfits: List<Outfit>,
    // Usamos @SerialName para que en el JSON la clave sea "outfitLinks",
    // pero en nuestro código podamos usar el nombre más corto "links".
    // Esto soluciona el problema de importación de forma definitiva.
    @SerialName("outfitLinks")
    val links: List<OutfitClothingLink>
)

