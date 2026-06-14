package org.darthacheron.pantrypal.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileTest {

    @Test
    fun testToDto() {
        val id = Uuid.random()
        val now = Clock.System.now()
        val profile = Profile(
            id = id,
            username = "user",
            email = "user@test.com",
            createdAt = now,
            lastModifiedAt = now
        )

        val serverId = Uuid.random()
        val dto = profile.toDto(serverId)

        assertEquals(serverId, dto.serverId)
        assertEquals(id, dto.clientId)
        assertEquals("user", dto.username)
        assertEquals("user@test.com", dto.email)
    }

    @Test
    fun testToDtoOverrides() {
        val profile = Profile(
            username = "local",
            email = "local@test.com",
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now()
        )

        val dto = profile.toDto(null, usernameOverride = "remote", emailOverride = "remote@test.com")

        assertEquals("remote", dto.username)
        assertEquals("remote@test.com", dto.email)
    }
}
