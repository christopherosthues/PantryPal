package org.darthacheron.pantrypal.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val tokenResponse: TokenResponse,
    val user: UserResponse
)