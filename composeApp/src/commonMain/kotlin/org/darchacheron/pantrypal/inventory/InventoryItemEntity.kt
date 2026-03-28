package org.darchacheron.pantrypal.inventory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.profile.ProfileEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "inventory_item",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = false) val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid?,
    val profileId: Uuid,
    val name: String,
    val kiloCalories: Int?,
    val kiloJoule: Int?,
    val fatInGrams: Float?,
    val saturatedFattyAcidsInGrams: Float?,
    val carbsInGrams: Float?,
    val sugarInGrams: Float?,
    val dietaryFiberInGrams: Float?,
    val proteinInGrams: Float?,
    val saltInGrams: Float?,
    val fillingQuantity: Float?,
    val isLiquid: Boolean = false,
    val createdAt: Instant,
    val lastModifiedAt: Instant
) {
    fun toInventoryItem(image: Image?, additionalImages: List<Image>): InventoryItem = InventoryItem(
        id = id,
        serverId = serverId,
        profileId = profileId,
        name = name,
        kiloCalories = kiloCalories,
        kiloJoule = kiloJoule,
        fatInGrams = fatInGrams,
        saturatedFattyAcidsInGrams = saturatedFattyAcidsInGrams,
        carbsInGrams = carbsInGrams,
        sugarInGrams = sugarInGrams,
        dietaryFiberInGrams = dietaryFiberInGrams,
        proteinInGrams = proteinInGrams,
        saltInGrams = saltInGrams,
        fillingQuantity = fillingQuantity,
        isLiquid = isLiquid,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        image = image,
        additionalImages = additionalImages
    )
}

@OptIn(ExperimentalUuidApi::class)
fun InventoryItem.toInventoryItemEntity(): InventoryItemEntity = InventoryItemEntity(
    id = id,
    serverId = serverId,
    profileId = profileId,
    name = name,
    kiloCalories = kiloCalories,
    kiloJoule = kiloJoule,
    fatInGrams = fatInGrams,
    saturatedFattyAcidsInGrams = saturatedFattyAcidsInGrams,
    carbsInGrams = carbsInGrams,
    sugarInGrams = sugarInGrams,
    dietaryFiberInGrams = dietaryFiberInGrams,
    proteinInGrams = proteinInGrams,
    saltInGrams = saltInGrams,
    fillingQuantity = fillingQuantity,
    isLiquid = isLiquid,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt
)
