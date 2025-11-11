package com.pep1lo.armariovirtual.data

/**
 * Datos calculados para la pantalla de estadísticas (no persistente).
 * Nuevos campos añadidos con valores por defecto para mantener compatibilidad.
 */
data class WardrobeStats(
    val top5MostWornItems: List<ClothingItem> = emptyList(),
    val top5LeastWornItems: List<ClothingItem> = emptyList(),
    val colorDistribution: Map<String, Int> = emptyMap(),
    val styleDistribution: Map<String, Int> = emptyMap(),
    // Lista completa ordenada por uso descendente (para expansión).
    val allItemsByUsageDesc: List<ClothingItem> = emptyList()
)