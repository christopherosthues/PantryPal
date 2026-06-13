package org.darthacheron.pantrypal.authentication

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AuthenticationPreferencesRepositoryImpl(private val dataStore: DataStore<Preferences>) :
    AuthenticationPreferencesRepository {
    override val authenticationPreferencesFlow: Flow<AuthenticationPreferences> = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }.map {
            val accessToken = it[AuthenticationPreferencesKeys.ACCESS_TOKEN] ?: ""
            val refreshToken = it[AuthenticationPreferencesKeys.REFRESH_TOKEN] ?: ""
            val expiresIn = it[AuthenticationPreferencesKeys.EXPIRES_IN] ?: 0
            val refreshExpiresIn = it[AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN] ?: 0
            val localProfileId = it[AuthenticationPreferencesKeys.LOCAL_PROFILE_ID] ?: ""
            val isLoggedInRemotely = it[AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY] ?: false
            val serverUrl = it[AuthenticationPreferencesKeys.SERVER_URL] ?: ""
            val stayLoggedIn = it[AuthenticationPreferencesKeys.STAY_LOGGED_IN] ?: false
            val acquiredAt = it[AuthenticationPreferencesKeys.ACQUIRED_AT] ?: 0
            AuthenticationPreferences(
                accessToken,
                refreshToken,
                expiresIn,
                refreshExpiresIn,
                localProfileId,
                isLoggedInRemotely,
                serverUrl,
                stayLoggedIn,
                acquiredAt
            )
        }

    override suspend fun loginLocally(localProfileId: String, serverUrl: String?, stayLoggedIn: Boolean): Result<Unit> =
        Result.runCatching {
            dataStore.edit {
                it[AuthenticationPreferencesKeys.LOCAL_PROFILE_ID] = localProfileId
                it[AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY] = false
                if (serverUrl != null) {
                    it[AuthenticationPreferencesKeys.SERVER_URL] = serverUrl
                }
                it[AuthenticationPreferencesKeys.STAY_LOGGED_IN] = stayLoggedIn
                // Clear remote tokens to ensure they don't leak between different local profile sessions
                it[AuthenticationPreferencesKeys.ACCESS_TOKEN] = ""
                it[AuthenticationPreferencesKeys.REFRESH_TOKEN] = ""
                it[AuthenticationPreferencesKeys.EXPIRES_IN] = 0
                it[AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN] = 0
                it[AuthenticationPreferencesKeys.ACQUIRED_AT] = 0
            }
        }

    override suspend fun loginRemotely(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        refreshExpiresIn: Int,
        serverUrl: String,
        acquiredAt: Long
    ): Result<Unit> = Result.runCatching {
        dataStore.edit {
            it[AuthenticationPreferencesKeys.ACCESS_TOKEN] = accessToken
            it[AuthenticationPreferencesKeys.REFRESH_TOKEN] = refreshToken
            it[AuthenticationPreferencesKeys.EXPIRES_IN] = expiresIn
            it[AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN] = refreshExpiresIn
            it[AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY] = true
            it[AuthenticationPreferencesKeys.SERVER_URL] = serverUrl
            it[AuthenticationPreferencesKeys.ACQUIRED_AT] = acquiredAt
        }
    }

    override suspend fun updateAccessToken(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        refreshExpiresIn: Int,
        acquiredAt: Long
    ): Result<Unit> = Result.runCatching {
        dataStore.edit {
            it[AuthenticationPreferencesKeys.ACCESS_TOKEN] = accessToken
            it[AuthenticationPreferencesKeys.REFRESH_TOKEN] = refreshToken
            it[AuthenticationPreferencesKeys.EXPIRES_IN] = expiresIn
            it[AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN] = refreshExpiresIn
            it[AuthenticationPreferencesKeys.ACQUIRED_AT] = acquiredAt
        }
    }

    override suspend fun logoutRemotely(): Result<Unit> = Result.runCatching {
        dataStore.edit {
            it[AuthenticationPreferencesKeys.ACCESS_TOKEN] = ""
            it[AuthenticationPreferencesKeys.REFRESH_TOKEN] = ""
            it[AuthenticationPreferencesKeys.EXPIRES_IN] = 0
            it[AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN] = 0
            it[AuthenticationPreferencesKeys.ACQUIRED_AT] = 0
            it[AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY] = false
        }
    }

    override suspend fun logout(): Result<Unit> = Result.runCatching {
        dataStore.edit {
            it[AuthenticationPreferencesKeys.ACCESS_TOKEN] = ""
            it[AuthenticationPreferencesKeys.REFRESH_TOKEN] = ""
            it[AuthenticationPreferencesKeys.EXPIRES_IN] = 0
            it[AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN] = 0
            it[AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY] = false
            it[AuthenticationPreferencesKeys.SERVER_URL] = ""
            it[AuthenticationPreferencesKeys.STAY_LOGGED_IN] = false
            it[AuthenticationPreferencesKeys.ACQUIRED_AT] = 0
            it.remove(AuthenticationPreferencesKeys.LOCAL_PROFILE_ID)
        }
    }
}