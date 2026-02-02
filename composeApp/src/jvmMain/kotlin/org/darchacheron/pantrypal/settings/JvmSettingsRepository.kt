package org.darchacheron.pantrypal.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.darchacheron.pantrypal.utils.DesktopPaths
import java.io.File
import java.util.Properties

class JvmSettingsRepository : SettingsRepository {
    private val settingsFlow = MutableStateFlow(Settings())
    private val settingsFile =
        File(
            DesktopPaths.getAppDataDir(),
            "${SettingsKeys.FILE_NAME}.properties"
        )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        runCatching {
            if (!settingsFile.exists()) {
                return
            }

            Properties().apply {
                settingsFile.inputStream().use { load(it) }

                val settings =
                    Settings(
                        weightUnit =
                            getProperty(SettingsKeys.WEIGHT_UNIT)?.let {
                                MeasureUnit.valueOf(it)
                            } ?: MeasureUnit.KG,
                        themeMode =
                            getProperty(SettingsKeys.THEME_MODE)?.let {
                                ThemeMode.valueOf(it)
                            } ?: ThemeMode.SYSTEM,
                    )
                settingsFlow.value = settings
            }
        }.onFailure { it.printStackTrace() }
    }

    override suspend fun saveSettings(settings: Settings) {
        withContext(Dispatchers.IO) {
            runCatching {
                Properties().apply {
                    setProperty(SettingsKeys.WEIGHT_UNIT, settings.weightUnit.name)
                    setProperty(SettingsKeys.THEME_MODE, settings.themeMode.name)
                    settingsFile.outputStream().use { store(it, null) }
                }
                settingsFlow.value = settings
            }.onFailure { it.printStackTrace() }
        }
    }

    override fun getSettingsFlow(): Flow<Settings> = settingsFlow

    override suspend fun getSettings(): Settings = settingsFlow.value
}