package com.pep1lo.armariovirtual.data

import kotlinx.serialization.Serializable

// SE HAN ELIMINADO LAS DEFINICIONES DUPLICADAS DE ENUMS DE ESTE ARCHIVO
object DataSource {

    // Ahora usamos directamente los enums definidos en ClothingEnums.kt
    val categories = Category.values().toList()
    val styles = Style.values().toList()
    val seasons = Season.values().toList()

    val categoryItemMap = mapOf(
        Category.EXTERIOR to listOf("Abrigo", "Chaqueta", "Americana"),
        Category.SUPERIOR to listOf("Camisa", "Jersey", "Cardigan", "Top", "Camiseta"),
        Category.INFERIOR to listOf("Pantalones", "Falda", "Tejanos"),
        Category.COMPLETO to listOf("Vestido", "Mono")
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

