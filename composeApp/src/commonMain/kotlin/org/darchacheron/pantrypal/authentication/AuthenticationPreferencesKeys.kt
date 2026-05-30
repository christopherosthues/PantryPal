package org.darchacheron.pantrypal.authentication

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object AuthenticationPreferencesKeys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val EXPIRES_IN = intPreferencesKey("expires_in")
    val REFRESH_EXPIRES_IN = intPreferencesKey("refresh_expires_in")
    val LOCAL_PROFILE_ID = stringPreferencesKey("local_profile_id")
    val IS_LOGGED_IN_REMOTELY = booleanPreferencesKey("is_logged_in_remotely")
    val SERVER_URL = stringPreferencesKey("server_url")
    val STAY_LOGGED_IN = booleanPreferencesKey("stay_logged_in")
    val ACQUIRED_AT = longPreferencesKey("acquired_at")
}
