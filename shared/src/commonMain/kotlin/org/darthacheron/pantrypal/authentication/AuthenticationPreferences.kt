package org.darthacheron.pantrypal.authentication

data class AuthenticationPreferences(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val refreshExpiresIn: Int,
    val localProfileId: String = "",
    val isLoggedInRemotely: Boolean = false,
    val serverUrl: String = "",
    val stayLoggedIn: Boolean = false,
    val acquiredAt: Long = 0
)
