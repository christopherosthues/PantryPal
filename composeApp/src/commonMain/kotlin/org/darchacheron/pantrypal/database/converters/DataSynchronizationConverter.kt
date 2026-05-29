package org.darchacheron.pantrypal.database.converters

import androidx.room.TypeConverter
import org.darchacheron.pantrypal.settings.DataSynchronization

class DataSynchronizationConverter {
    @TypeConverter
    fun fromDataSynchronization(value: DataSynchronization): String {
        return value.name
    }

    @TypeConverter
    fun toDataSynchronization(value: String): DataSynchronization {
        return DataSynchronization.valueOf(value)
    }
}
