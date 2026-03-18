package org.darchacheron.pantrypal.authentication

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object AuthenticationPreferencesKeys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val EXPIRES_IN = intPreferencesKey("expires_in")
    val REFRESH_EXPIRES_IN = intPreferencesKey("refresh_expires_in")
}
