package com.pep1lo.armariovirtual.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usageCount: Int = 0,
    val lastWornDate: Long? = null
)
