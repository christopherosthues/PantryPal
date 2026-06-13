package org.darthacheron.pantrypal.authentication

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AuthenticationPreferencesRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AuthenticationPreferencesRepositoryImpl
    private val preferencesFlow = MutableStateFlow<Preferences>(mock())

    @BeforeTest
    fun setup() {
        dataStore = mock<DataStore<Preferences>> {
            every { data } returns preferencesFlow
        }
        repository = AuthenticationPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun testInitialState() = runTest {
        val prefs = mock<Preferences> {
            every { get(AuthenticationPreferencesKeys.ACCESS_TOKEN) } returns null
            every { get(AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY) } returns null
        }
        preferencesFlow.value = prefs

        val result = repository.authenticationPreferencesFlow.first()
        assertEquals("", result.accessToken)
        assertFalse(result.isLoggedInRemotely)
    }

    @Test
    fun testAuthenticationPreferencesMapping() = runTest {
        val prefs = mock<Preferences> {
            every { get(AuthenticationPreferencesKeys.ACCESS_TOKEN) } returns "token123"
            every { get(AuthenticationPreferencesKeys.IS_LOGGED_IN_REMOTELY) } returns true
            every { get(AuthenticationPreferencesKeys.LOCAL_PROFILE_ID) } returns "profile-uuid"
        }
        preferencesFlow.value = prefs

        val result = repository.authenticationPreferencesFlow.first()
        assertEquals("token123", result.accessToken)
        assertTrue(result.isLoggedInRemotely)
        assertEquals("profile-uuid", result.localProfileId)
    }

    // Testing edit() is harder with mocks because of the lambda. 
    // Usually better to use a real DataStore with a temporary file in an integration test.
}
