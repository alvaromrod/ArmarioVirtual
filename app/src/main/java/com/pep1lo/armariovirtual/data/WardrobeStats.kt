package com.pep1lo.armariovirtual.data

/**
 * Contiene los datos calculados para la pantalla de estadísticas.
 */
data class WardrobeStats(
    val top5MostWornItems: List<ClothingItem> = emptyList(),
    val colorDistribution: Map<String, Int> = emptyMap(),
    val styleDistribution: Map<String, Int> = emptyMap()
)
