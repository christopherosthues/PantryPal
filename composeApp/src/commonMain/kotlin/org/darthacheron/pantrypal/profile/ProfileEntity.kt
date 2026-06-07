package org.darthacheron.pantrypal.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.darthacheron.pantrypal.settings.DataSynchronization
import org.darthacheron.pantrypal.core.profile.ProfileDto
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "profile",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class ProfileEntity(
    @PrimaryKey val id: Uuid = Uuid.generateV7(),
    val username: String,
    val email: String,
    val passwordHash: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val lastSyncedAt: Instant? = null,
    val isLocalOnly: Boolean = false,
    val dataSynchronization: DataSynchronization = DataSynchronization.NO_SYNCHRONIZATION
) {
    fun toProfile(): Profile = Profile(
        id = id,
        username = username,
        email = email,
        passwordHash = passwordHash,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        lastSyncedAt = lastSyncedAt,
        isLocalOnly = isLocalOnly,
        dataSynchronization = dataSynchronization
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Profile.toProfileEntity(): ProfileEntity = ProfileEntity(
    id = id,
    username = username,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
    lastSyncedAt = lastSyncedAt,
    isLocalOnly = isLocalOnly,
    dataSynchronization = dataSynchronization
)

@OptIn(ExperimentalUuidApi::class)
fun ProfileDto.toProfileEntity(baseProfile: Profile): ProfileEntity = ProfileEntity(
    id = baseProfile.id,
    username = username,
    email = email,
    passwordHash = baseProfile.passwordHash,
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
    lastSyncedAt = lastSyncedAt,
    isLocalOnly = baseProfile.isLocalOnly,
    dataSynchronization = baseProfile.dataSynchronization
)
