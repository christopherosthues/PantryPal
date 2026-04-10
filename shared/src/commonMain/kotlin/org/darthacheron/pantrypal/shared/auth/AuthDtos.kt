package org.darthacheron.pantrypal.shared.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(val username: String, val password: String)

@Serializable
data class RegistrationDto(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenDto(val refreshToken: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String
)

@Serializable
data class LoginResponse(
    val tokenResponse: TokenResponse,
    val user: UserResponse
)

@Serializable
data class RegistrationResponse(
    val tokenResponse: TokenResponse,
    val user: UserResponse
)

@Serializable
data class UpdateUserDto(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null
)

@Serializable
data class ProblemDetails(
    // TODO: use newer rfc 9467 standard
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val errors: Map<String, List<String>>? = null
)
