package com.pep1lo.armariovirtual.data

import kotlinx.serialization.SerialName

// Hemos añadido la anotación @SerialName para que la app sea compatible
// con los backups JSON de la versión anterior.
// Ahora, al leer un JSON, sabe que el texto "Exterior" corresponde al enum EXTERIOR.
enum class Category(val displayName: String) {
    @SerialName("Exterior")
    EXTERIOR("Exterior"),

    @SerialName("Superior")
    SUPERIOR("Superior"),

    @SerialName("Inferior")
    INFERIOR("Inferior"),

    @SerialName("Completo")
    COMPLETO("Completo")
}

enum class Style(val displayName: String) {
    @SerialName("Básico")
    BASICO("Básico"),

    @SerialName("Boho")
    BOHO("Boho"),

    @SerialName("Preppy")
    PREPPY("Preppy")
}

enum class Season(val displayName: String) {
    @SerialName("Invierno")
    INVIERNO("Invierno"),

    @SerialName("Verano")
    VERANO("Verano"),

    @SerialName("Entretiempo")
    ENTRETIEMPO("Entretiempo")
}

