package org.darchacheron.pantrypal.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SettingsKeys.FILE_NAME)

class AndroidSettingsRepository(
    private val context: Context
) : SettingsRepository {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey(SettingsKeys.THEME_MODE)
        val DATA_SYNCHRONIZATION = stringPreferencesKey(SettingsKeys.DATA_SYNCHRONIZATION)
    }

    override suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = settings.themeMode.name
            preferences[PreferencesKeys.DATA_SYNCHRONIZATION] = settings.dataSynchronization.name
        }
    }

    override fun getSettingsFlow(): Flow<Settings> =
        context.dataStore.data
            .catch { exception ->
                exception.printStackTrace()
                emit(emptyPreferences())
            }.map { preferences ->
                Settings(
                    themeMode =
                        preferences[PreferencesKeys.THEME_MODE]?.let {
                            runCatching { ThemeMode.valueOf(it) }.getOrNull()
                        } ?: ThemeMode.SYSTEM,
                    dataSynchronization =
                        preferences[PreferencesKeys.DATA_SYNCHRONIZATION]?.let {
                            runCatching { DataSynchronization.valueOf(it) }.getOrNull()
                        } ?: DataSynchronization.NO_SYNCHRONIZATION
                )
            }
}
