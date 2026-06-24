package com.pep1lo.armariovirtual.data

import kotlinx.serialization.SerialName

enum class Category(val displayName: String) {
    @SerialName("Exterior")
    EXTERIOR("Exterior"),

    @SerialName("Superior")
    SUPERIOR("Superior"),

    @SerialName("Inferior")
    INFERIOR("Inferior"),

    @SerialName("Completo")
    COMPLETO("Completa")
}

enum class Style(val displayName: String) {
    @SerialName("Básico")
    BASICO("Básico"),

    @SerialName("Boho")
    BOHO("Boho"),

    @SerialName("Preppy")
    PREPPY("Preppy"),

    // --- VALORES AÑADIDOS ---
    @SerialName("Sport")
    SPORT("Sport"),

    @SerialName("Cute")
    CUTE("Cute"),

    @SerialName("Casual")
    CASUAL("Casual")
}

enum class Season(val displayName: String) {
    @SerialName("Invierno")
    INVIERNO("Invierno"),

    @SerialName("Verano")
    VERANO("Verano"),

    @SerialName("Entretiempo")
    ENTRETIEMPO("Entretiempo")
}

