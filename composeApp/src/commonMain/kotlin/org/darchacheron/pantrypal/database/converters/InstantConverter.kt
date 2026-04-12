package org.darchacheron.pantrypal.database.converters

import androidx.room.TypeConverter
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Converts [Instant] to and from a string using UTC ISO 8601.
 */
@OptIn(ExperimentalTime::class)
class InstantConverter {
    @TypeConverter
    fun fromString(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }

    @TypeConverter
    fun instantToString(instant: Instant?): String? {
        return instant?.toString()
    }
}