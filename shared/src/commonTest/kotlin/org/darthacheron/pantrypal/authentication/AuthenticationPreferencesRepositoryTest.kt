package org.darthacheron.pantrypal.authentication

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AuthenticationPreferencesRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AuthenticationPreferencesRepositoryImpl
    private val preferencesFlow = MutableStateFlow<Preferences>(emptyPreferences())

    @BeforeTest
    fun setup() {
        dataStore = mock<DataStore<Preferences>> {
            every { data } returns preferencesFlow
        }
        repository = AuthenticationPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun testInitialState() = runTest {
        val prefs = emptyPreferences()
        preferencesFlow.value = prefs

        val result = repository.authenticationPreferencesFlow.first()
        assertEquals("", result.accessToken)
        assertFalse(result.isLoggedInRemotely)
    }

    @Test
    fun testAuthenticationPreferencesMapping() = runTest {
        val prefs = preferencesOf(
            AuthenticationPreferencesKeys.ACCESS_TOKEN to "token123",
            AuthenticationPreferencesKeys.REFRESH_TOKEN to "refresh123",
            AuthenticationPreferencesKeys.EXPIRES_IN to 3600,
            AuthenticationPreferencesKeys.REFRESH_EXPIRES_IN to 7200,
            AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY to true,
            AuthenticationPreferencesKeys.LOCAL_PROFILE_ID to "profile-uuid",
            AuthenticationPreferencesKeys.SERVER_URL to "http://localhost",
            AuthenticationPreferencesKeys.STAY_LOGGED_IN to true,
            AuthenticationPreferencesKeys.ACQUIRED_AT to 123456789L
        )
        preferencesFlow.value = prefs

        val result = repository.authenticationPreferencesFlow.first()
        assertEquals("token123", result.accessToken)
        assertEquals("refresh123", result.refreshToken)
        assertEquals(3600, result.expiresIn)
        assertEquals(7200, result.refreshExpiresIn)
        assertTrue(result.isLoggedInRemotely)
        assertEquals("profile-uuid", result.localProfileId)
        assertEquals("http://localhost", result.serverUrl)
        assertTrue(result.stayLoggedIn)
        assertEquals(123456789L, result.acquiredAt)
    }

    @Test
    fun testIOExceptionInFlow() = runTest {
        val failingDataStore = mock<DataStore<Preferences>> {
            every { data } returns flow {
                throw IOException("Disk full")
            }
        }
        val failingRepo = AuthenticationPreferencesRepositoryImpl(failingDataStore)
        
        val result = failingRepo.authenticationPreferencesFlow.first()
        assertEquals("", result.accessToken)
        assertFalse(result.isLoggedInRemotely)
    }

    @Test
    fun testNonIOExceptionInFlow() = runTest {
        val failingDataStore = mock<DataStore<Preferences>> {
            every { data } returns flow {
                throw Exception("Critical failure")
            }
        }
        val failingRepo = AuthenticationPreferencesRepositoryImpl(failingDataStore)
        
        assertFailsWith<Exception> {
            failingRepo.authenticationPreferencesFlow.first()
        }
    }
}
