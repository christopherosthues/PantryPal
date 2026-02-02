package org.darchacheron.pantrypal.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveSettings(settings: Settings)

    fun getSettingsFlow(): Flow<Settings>

    suspend fun getSettings(): Settings = getSettingsFlow().first()
}