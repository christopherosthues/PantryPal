package org.darthacheron.pantrypal.database.converters

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

/**
 * Room type converter for kotlinx.datetime.LocalDate
 * Stores LocalDate as String in format YYYY-MM-DD
 */
@OptIn(ExperimentalTime::class)
class LocalDateConverter {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate = LocalDate.parse(dateString)
}
