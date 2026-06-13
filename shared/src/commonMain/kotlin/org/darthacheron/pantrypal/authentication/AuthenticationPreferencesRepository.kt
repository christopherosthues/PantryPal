package org.darthacheron.pantrypal.authentication

import kotlinx.coroutines.flow.Flow

interface AuthenticationPreferencesRepository {
    val authenticationPreferencesFlow: Flow<AuthenticationPreferences>
    suspend fun loginLocally(localProfileId: String, serverUrl: String?, stayLoggedIn: Boolean): Result<Unit>
    suspend fun loginRemotely(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        refreshExpiresIn: Int,
        serverUrl: String,
        acquiredAt: Long
    ): Result<Unit>
    suspend fun updateAccessToken(
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        refreshExpiresIn: Int,
        acquiredAt: Long
    ): Result<Unit>
    suspend fun logoutRemotely(): Result<Unit>
    suspend fun logout(): Result<Unit>
}

