package org.darthacheron.pantrypal.authentication

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createAuthenticationDataStore(): DataStore<Preferences> {
    return createAuthenticationDataStore(
        producePath = {
            val osName = System.getProperty("os.name").lowercase()
            val appName = "PantryPal"
            val directory = when {
                osName.contains("win") -> "${System.getenv("APPDATA")}\\$appName\\"
                osName.contains("mac") -> "${System.getProperty("user.home")}/Library/Application Support/$appName/"
                osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> "${System.getProperty("user.home")}/.local/share/$appName/"
                else -> "${System.getProperty("user.home")}/"
            }
            directory + authenticationDataStoreFileName
        }
    )
}