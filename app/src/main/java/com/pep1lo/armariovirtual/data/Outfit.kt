package com.pep1lo.armariovirtual.data

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @SerialName("usageCount")
    val usageCount: Int = 0,
    val lastWornDate: Long? = null
)