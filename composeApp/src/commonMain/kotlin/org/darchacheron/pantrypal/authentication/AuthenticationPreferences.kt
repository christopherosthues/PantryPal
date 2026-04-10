package org.darchacheron.pantrypal.authentication

data class AuthenticationPreferences(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val refreshExpiresIn: Int,
    val localProfileId: String = "",
    val isLoggedInRemotely: Boolean = false
)
