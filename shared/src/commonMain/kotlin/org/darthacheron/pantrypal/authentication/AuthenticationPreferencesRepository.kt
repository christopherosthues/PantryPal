package org.darthacheron.pantrypal.authentication

import kotlinx.coroutines.flow.Flow

interface AuthenticationPreferencesRepository {
    val authenticationPreferencesFlow: Flow<AuthenticationPreferences>
    suspend fun loginLocally(localProfileId: String, serverUrl: String?, stayLoggedIn: Boolean)
    suspend fun loginRemotely(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        refreshExpiresIn: Int,
        serverUrl: String,
        acquiredAt: Long
    )
    suspend fun updateAccessToken(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        refreshExpiresIn: Int,
        acquiredAt: Long
    )
    suspend fun logoutRemotely()
    suspend fun logout()
}

