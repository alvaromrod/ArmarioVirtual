package com.pep1lo.armariovirtual.data

import kotlinx.serialization.Serializable

// SE HAN ELIMINADO LAS DEFINICIONES DUPLICADAS DE ENUMS DE ESTE ARCHIVO
object DataSource {

    // Ahora usamos directamente los enums definidos en ClothingEnums.kt
    val categories = Category.values().toList()
    val styles = Style.values().toList()
    val seasons = Season.values().toList()

    val categoryItemMap = mapOf(
        Category.EXTERIOR to listOf("Chaleco", "Jersey"),
        Category.SUPERIOR to listOf("Top", "Blusa", "Camiseta", "Camisa"),
        Category.INFERIOR to listOf("Falda", "Pantalon", "Shorts"),
        Category.COMPLETO to listOf("Vestido")
    )

    @Serializable
    data class ColorInfo(val name: String, val isNeutral: Boolean)

    val allColors = listOf(
        ColorInfo("Negro", true),
        ColorInfo("Blanco", true),
        ColorInfo("Gris", true),
        ColorInfo("Beige", true),
        ColorInfo("Azul Marino", true),
        ColorInfo("Rojo", false),
        ColorInfo("Azul", false),
        ColorInfo("Verde", false),
        ColorInfo("Amarillo", false),
        ColorInfo("Rosa", false),
        ColorInfo("Naranja", false),
        ColorInfo("Morado", false)
    )
}

