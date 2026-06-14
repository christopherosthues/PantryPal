package org.darthacheron.pantrypal.database.converters

import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.darthacheron.pantrypal.settings.DataSynchronization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ConverterTests {

    @Test
    fun testUuidConverter() {
        val converter = UuidConverter()
        val uuid = Uuid.random()
        val string = converter.fromUUID(uuid)
        assertEquals(uuid.toString(), string)
        assertEquals(uuid, converter.uuidFromString(string))
        
        assertNull(converter.fromUUID(null))
        assertNull(converter.uuidFromString(null))
    }

    @Test
    fun testInstantConverter() {
        val converter = InstantConverter()
        val now = Instant.fromEpochMilliseconds(123456789)
        val string = converter.instantToString(now)
        assertEquals(now.toString(), string)
        assertEquals(now, converter.fromString(string))

        assertNull(converter.instantToString(null))
        assertNull(converter.fromString(null))
    }

    @Test
    fun testLocalDateConverter() {
        val converter = LocalDateConverter()
        val date = LocalDate(2024, 1, 1)
        val string = converter.fromLocalDate(date)
        assertEquals("2024-01-01", string)
        assertEquals(date, converter.toLocalDate(string))
    }

    @Test
    fun testStringListConverter() {
        val converter = StringListConverter()
        val list = listOf("a", "b", "c")
        val string = converter.fromList(list)
        val decoded = converter.fromString(string)
        assertEquals(list, decoded)
        
        // Test empty/invalid
        assertEquals(emptyList(), converter.fromString(""))
    }

    @Test
    fun testDataSynchronizationConverter() {
        val converter = DataSynchronizationConverter()
        val sync = DataSynchronization.UPLOAD_AND_DOWNLOAD
        val string = converter.fromDataSynchronization(sync)
        assertEquals("UPLOAD_AND_DOWNLOAD", string)
        assertEquals(sync, converter.toDataSynchronization(string))
    }
}
