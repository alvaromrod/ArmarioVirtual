package com.pep1lo.armariovirtual.data

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val clothingItems: List<ClothingItem>,
    val outfits: List<Outfit>,
    val links: List<OutfitClothingLink>
)
