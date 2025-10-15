package com.pep1lo.armariovirtual.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// SE HAN CORREGIDO LOS IMPORTS PARA USAR LOS ENUMS GLOBALES
import com.pep1lo.armariovirtual.data.Category
import com.pep1lo.armariovirtual.data.Season
import com.pep1lo.armariovirtual.data.Style

@Entity(tableName = "clothing_items")
@Serializable
data class ClothingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // AÑADIMOS VALORES POR DEFECTO A LAS PROPIEDADES REQUERIDAS
    val name: String = "",
    val category: Category = Category.SUPERIOR,
    val features: String = "",
    val color: String = "",
    val style: Style = Style.BASICO,
    val season: Season = Season.ENTRETIEMPO,
    val imageUri: String? = null,
    val isAvailable: Boolean = true,
    var wearCount: Int = 0,
    val secondaryCategory: Category? = null
)

