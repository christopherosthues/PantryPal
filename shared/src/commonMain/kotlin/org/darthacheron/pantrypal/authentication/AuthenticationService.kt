package org.darthacheron.pantrypal.authentication

import org.darthacheron.pantrypal.core.auth.LoginResponse
import org.darthacheron.pantrypal.core.auth.RegistrationResponse
import kotlin.uuid.Uuid

class InvalidCredentialsException(message: String) : Exception(message)
class ProfileNotFoundException(message: String) : Exception(message)
class ServerUnreachableException(message: String, cause: Throwable? = null) : Exception(message, cause)
class UserAlreadyExistsException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)
class NotAuthenticatedException(message: String) : Exception(message)

interface AuthenticationService {
    suspend fun loginLocally(profileId: Uuid, stayLoggedIn: Boolean, serverUrl: String? = null)
    suspend fun loginRemotely(username: String, password: String, serverUrl: String): Result<LoginResponse?>
    suspend fun logoutRemotely(): Result<Boolean>
    suspend fun logout(): Result<Boolean>
    suspend fun refreshToken(serverUrl: String): Result<Boolean>
    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        serverUrl: String
    ): Result<RegistrationResponse?>
    suspend fun updateUser(
        serverUrl: String,
        username: String? = null,
        email: String? = null,
        password: String? = null,
        currentPassword: String? = null
    ): Result<Boolean>
}

