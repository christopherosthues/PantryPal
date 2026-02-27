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
        val WEIGHT_UNIT = stringPreferencesKey(SettingsKeys.WEIGHT_UNIT)
        val THEME_MODE = stringPreferencesKey(SettingsKeys.THEME_MODE)
    }

    override suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEIGHT_UNIT] = settings.weightUnit.name
            preferences[PreferencesKeys.THEME_MODE] = settings.themeMode.name
        }
    }

    override fun getSettingsFlow(): Flow<Settings> =
        context.dataStore.data
            .catch { exception ->
                // Log the error and emit default settings
                exception.printStackTrace()
                emit(emptyPreferences())
            }.map { preferences ->
                Settings(
                    weightUnit =
                        preferences[PreferencesKeys.WEIGHT_UNIT]?.let {
                            MeasureUnit.valueOf(it)
                        } ?: MeasureUnit.KG,
                    themeMode =
                        preferences[PreferencesKeys.THEME_MODE]?.let {
                            ThemeMode.valueOf(it)
                        } ?: ThemeMode.SYSTEM,
                )
            }
}