package com.pep1lo.armariovirtual.data

/**
 * Un objeto Singleton que actúa como fuente de datos estáticos para la app.
 */
object DataSource {

    val seasons = listOf("Invierno", "Verano", "Entretiempo")
    val styles = listOf("Básico", "Boho", "Preppy")

    // --- INICIO DE LA ACTUALIZACIÓN ---
    val neutralColors = listOf("Negro", "Blanco", "Gris", "Beige", "Azul Marino", "Marrón")
    val accentColors = listOf("Rojo", "Azul", "Verde", "Amarillo", "Naranja", "Rosa", "Morado")
    val allColors = neutralColors + accentColors
    // --- FIN DE LA ACTUALIZACIÓN ---

    val categories = listOf("Exterior", "Intermedio", "Superior", "Inferior", "Completo")

    val categoryItemMap = mapOf(
        "Exterior" to listOf("Abrigo", "Chaqueta"),
        "Intermedio" to listOf("Jersey", "Cardigan", "Americana", "Camisa"),
        "Superior" to listOf("Top", "Camiseta", "Camisa"),
        "Inferior" to listOf("Pantalones", "Falda", "Tejanos"),
        "Completo" to listOf("Vestido", "Mono")
    )
}
