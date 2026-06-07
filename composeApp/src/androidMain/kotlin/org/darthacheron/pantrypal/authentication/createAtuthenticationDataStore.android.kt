package org.darthacheron.pantrypal.authentication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createAuthenticationDataStore(context: Context): DataStore<Preferences> {
    return createAuthenticationDataStore(
        producePath = {
            context.filesDir.resolve(authenticationDataStoreFileName).absolutePath
        }
    )
}