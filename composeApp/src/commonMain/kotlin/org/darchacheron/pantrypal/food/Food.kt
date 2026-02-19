package org.darchacheron.pantrypal.food

import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Food(
    val id: Uuid = Uuid.generateV7(),
    val name: String,
    val calories: Int?,
    val carbs: Int?,
    val fat: Int?,
    val protein: Int?,
    val weightInGrams: Int?,
    val bestBeforeDate: LocalDate?,
    val useByDate: LocalDate?,
    val openedAt: LocalDate?,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val imagePath: String? = null,
)
