package org.darchacheron.pantrypal.database.converters

import androidx.room.TypeConverter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UuidConverter {
    @TypeConverter
    fun fromUUID(uuid: Uuid): String = uuid.toString()

    @TypeConverter
    fun uuidFromString(string: String?): Uuid = Uuid.parse(string ?: "")
}