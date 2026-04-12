package org.darchacheron.pantrypal.profile

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile LIMIT 1")
    fun getProfile(): Flow<ProfileEntity?> // TODO: This is not the correct profile -> retrieve it from the settings

    @Query("SELECT * FROM profile WHERE id = :id")
    fun getProfileById(id: Uuid): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE serverId = :serverId")
    fun getProfileByServerId(serverId: Uuid): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE username = :username")
    fun getProfileByUsername(username: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE email = :email")
    fun getProfileByEmail(email: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE username = :identifier OR email = :identifier")
    fun getProfileByIdentifier(identifier: String): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun delete()
}
