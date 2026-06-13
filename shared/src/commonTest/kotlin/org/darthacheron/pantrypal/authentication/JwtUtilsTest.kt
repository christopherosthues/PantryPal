package org.darthacheron.pantrypal.authentication

import kotlin.test.*

class JwtUtilsTest {

    @Test
    fun testGetUserIdFromToken_ValidSub() {
        // Payload: {"sub": "user123"}
        val token = "header.eyJzdWIiOiAidXNlcjEyMyJ9.signature"
        assertEquals("user123", JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun testGetUserIdFromToken_ValidId() {
        // Payload: {"id": "user456"}
        val token = "header.eyJpZCI6ICJ1c2VyNDU2In0.signature"
        assertEquals("user456", JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun testGetUserIdFromToken_InvalidTokenFormat() {
        val token = "invalidtoken"
        assertNull(JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun testGetUserIdFromToken_MissingUserId() {
        // Payload: {"name": "John Doe"}
        val token = "header.eyJuYW1lIjogIkpvaG4gRG9lIn0.signature"
        assertNull(JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun testIsAdmin_True() {
        // Payload: {"realm_access": {"roles": ["admin", "user"]}}
        val token = "header.eyJyZWFsbV9hY2Nlc3MiOiB7InJvbGVzIjogWyJhZG1pbiIsICJ1c2VyIl19fQ.signature"
        assertTrue(JwtUtils.isAdmin(token))
    }

    @Test
    fun testIsAdmin_False() {
        // Payload: {"realm_access": {"roles": ["user"]}}
        val token = "header.eyJyZWFsbV9hY2Nlc3MiOiB7InJvbGVzIjogWyJ1c2VyIl19fQ.signature"
        assertFalse(JwtUtils.isAdmin(token))
    }

    @Test
    fun testIsAdmin_MissingRealmAccess() {
        // Payload: {"sub": "user123"}
        val token = "header.eyJzdWIiOiAidXNlcjEyMyJ9.signature"
        assertFalse(JwtUtils.isAdmin(token))
    }
}
