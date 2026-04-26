package org.darthacheron.pantrypal.server.authentication

import org.darthacheron.pantrypal.server.keycloak.*
import org.darthacheron.pantrypal.server.profile.ProfileService
import org.darthacheron.pantrypal.shared.auth.*
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AuthenticationService(
    private val keycloakService: KeycloakService,
    private val profileService: ProfileService
) {
    private val logger = LoggerFactory.getLogger(AuthenticationService::class.java)

    suspend fun login(loginDto: LoginDto): Result<LoginResponse> {
        logger.debug("Attempting login for user: {}", loginDto.username)
        
        val tokenResponse = keycloakService.getAccessToken(loginDto.username, loginDto.password)
            .getOrElse { return Result.failure(it) }

        val profileResult = profileService.getProfileByUsernameOrEmail(loginDto.username)
        val profile = profileResult.getOrElse { return Result.failure(it) }
            ?: return Result.failure(ProfileNotFoundException("Credentials valid in Keycloak, but no associated profile found in server database."))

        if (profile.deletedAt != null) {
            return Result.failure(ProfileDeletedException("Credentials valid in Keycloak, but associated profile has been deleted in server database."))
        }

        val userResponse = UserResponse(
            id = profile.serverId.toString(),
            username = profile.username,
            email = profile.email
        )
        return Result.success(LoginResponse(tokenResponse, userResponse))
    }

    suspend fun register(registrationDto: RegistrationDto): Result<RegistrationResponse> {
        logger.info("Attempting registration for user: {}", registrationDto.username)
        
        // 1. Create User in Keycloak
        val keycloakUserId = keycloakService.createUser(registrationDto.username, registrationDto.email, registrationDto.password)
            .getOrElse { return Result.failure(it) }

        // 2. Login with new credentials to get tokens for the response
        val tokenResponse = keycloakService.getAccessToken(registrationDto.username, registrationDto.password)
            .getOrElse {
                logger.error("User created in Keycloak, but initial login failed for user: {}. Reverting Keycloak user creation.", registrationDto.username)
                keycloakService.deleteUser(keycloakUserId)
                return Result.failure(Exception("User created successfully, but initial login attempt failed. Reverting Keycloak user creation."))
            }

        // 3. Create local profile
        val profileResult = profileService.createProfile(
            ProfileDto(
                serverId = null,
                clientId = Uuid.random(),
                username = registrationDto.username,
                email = registrationDto.email,
                createdAt = Clock.System.now(),
                lastModifiedAt = Clock.System.now(),
                lastSyncedAt = Clock.System.now()
            )
        )
        
        val profile = profileResult.getOrElse { e ->
            logger.error("Failed to create local profile for user: {}. Reverting Keycloak user creation.", registrationDto.username, e)
            keycloakService.deleteUser(keycloakUserId)
            return Result.failure(ProfileAlreadyExistsException("A profile with this username or email already exists in the server database. Reverting Keycloak user creation.", e.message ?: "Conflict"))
        }

        val userResponse = UserResponse(
            id = profile.serverId.toString(),
            username = profile.username,
            email = profile.email
        )
        return Result.success(RegistrationResponse(tokenResponse, userResponse))
    }

    suspend fun refresh(refreshDto: RefreshTokenDto): Result<TokenResponse> {
        logger.debug("Attempting token refresh")
        return keycloakService.refreshAccessToken(refreshDto.refreshToken)
    }
}

class ProfileNotFoundException(message: String) : Exception(message)
class ProfileDeletedException(message: String) : Exception(message)
class ProfileAlreadyExistsException(message: String, val detail: String) : Exception(message)
