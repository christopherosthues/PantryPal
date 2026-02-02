package org.darchacheron.pantrypal.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface SettingsRepository {
    suspend fun saveSettings(settings: Settings)

    fun getSettingsFlow(): Flow<Settings>

    suspend fun getSettings(): Settings = getSettingsFlow().first()
}