package com.pep1lo.armariovirtual.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    // --- INICIO DE LA MODIFICACIÓN: Tipos cambiados a Enums ---
    val category: Category,
    val style: Style,
    val season: Season,
    // --- FIN DE LA MODIFICACIÓN ---

    val secondaryCategory: String? = null,

    val features: String,

    val color: String,

    val isAvailable: Boolean = true,

    val usageCount: Int = 0,

    val imageUri: String
)
