package com.pep1lo.armariovirtual.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String, // "Camisa", "Pantalón", etc.

    val category: String, // "Superior", "Inferior", etc.

    val secondaryCategory: String? = null, // Para prendas como vestidos ("Inferior")

    val features: String, // Descripción de texto libre

    val color: String,

    val style: String, // "Básico", "Boho", etc.

    val season: String, // "Invierno", "Verano", etc.

    val isAvailable: Boolean = true, // true si está disponible, false si está en la lavadora, etc.

    val usageCount: Int = 0, // Contador de veces que se ha usado

    val imageUri: String // URI de la imagen guardada en el teléfono
)
