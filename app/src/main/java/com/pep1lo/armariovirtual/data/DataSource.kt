package com.pep1lo.armariovirtual.data

/**
 * Un objeto Singleton que actúa como fuente de datos estáticos para la app.
 */
object DataSource {

    // --- INICIO DE LA MODIFICACIÓN: Listas actualizadas para usar Enums ---
    val seasons = Season.values().toList()
    val styles = Style.values().toList()
    val categories = Category.values().toList()
    // --- FIN DE LA MODIFICACIÓN ---

    val neutralColors = listOf("Negro", "Blanco", "Gris", "Beige", "Azul Marino", "Marrón")
    val accentColors = listOf("Rojo", "Azul", "Verde", "Amarillo", "Naranja", "Rosa", "Morado")
    val allColors = neutralColors + accentColors

    // --- INICIO DE LA MODIFICACIÓN: Mapa actualizado para usar Enums ---
    val categoryItemMap = mapOf(
        Category.EXTERIOR to listOf("Abrigo", "Chaqueta", "Jersey", "Cardigan", "Americana", "Camisa"),
        Category.SUPERIOR to listOf("Top", "Camiseta", "Camisa"),
        Category.INFERIOR to listOf("Pantalones", "Falda", "Tejanos"),
        Category.COMPLETO to listOf("Vestido", "Mono")
    )
    // --- FIN DE LA MODIFICACIÓN ---
}
