package com.pep1lo.armariovirtual.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // AÑADIMOS @SerialName PARA COMPATIBILIDAD CON BACKUPS ANTIGUOS
    // El código usará 'wearCount', pero al leer JSON aceptará "usageCount"
    @SerialName("usageCount")
    val wearCount: Int = 0,
    val lastWornDate: Long? = null
)
