package com.pep1lo.armariovirtual.data

import androidx.room.TypeConverter

/**
 * Conversores para que Room sepa cómo guardar y leer nuestros Enums en la base de datos.
 * Los guardará como texto (String).
 */
class Converters {
    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(name: String): Category = Category.valueOf(name)

    @TypeConverter
    fun fromStyle(style: Style): String = style.name

    @TypeConverter
    fun toStyle(name: String): Style = Style.valueOf(name)

    @TypeConverter
    fun fromSeason(season: Season): String = season.name

    @TypeConverter
    fun toSeason(name: String): Season = Season.valueOf(name)
}

