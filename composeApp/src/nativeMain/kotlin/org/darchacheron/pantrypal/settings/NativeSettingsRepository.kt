package org.darchacheron.pantrypal.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.darchacheron.pantrypal.settings.DataSynchronization
import platform.Foundation.NSDictionary
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dictionaryWithContentsOfFile
import platform.Foundation.writeToFile
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class NativeSettingsRepository : SettingsRepository {
    private val settingsFlow = MutableStateFlow(Settings())
    private val fileManager = NSFileManager.defaultManager
    private val settingsFile: String
        get() {
            val docs =
                NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true
                ).first() as String
            return "$docs/${SettingsKeys.FILE_NAME}.plist"
        }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        runCatching {
            if (!fileManager.fileExistsAtPath(settingsFile)) {
                return
            }

            (NSDictionary.dictionaryWithContentsOfFile(settingsFile))?.let { dict ->
                val settings =
                    Settings(
                        themeMode =
                            (dict.getValue(SettingsKeys.THEME_MODE) as? String)?.let {
                                runCatching { ThemeMode.valueOf(it) }.getOrNull()
                            } ?: ThemeMode.SYSTEM,
                        dataSynchronization =
                            (dict.getValue(SettingsKeys.DATA_SYNCHRONIZATION) as? String)?.let {
                                runCatching { DataSynchronization.valueOf(it) }.getOrNull()
                            } ?: DataSynchronization.NO_SYNCHRONIZATION,
                        serverUrl = (dict.getValue(SettingsKeys.SERVER_URL) as? String) ?: ""
                    )
                settingsFlow.value = settings
            }
        }.onFailure { it.printStackTrace() }
    }

    override suspend fun saveSettings(settings: Settings) {
        runCatching {
            val dict =
                mutableMapOf(
                    SettingsKeys.THEME_MODE to settings.themeMode.name,
                    SettingsKeys.DATA_SYNCHRONIZATION to settings.dataSynchronization.name,
                    SettingsKeys.SERVER_URL to settings.serverUrl
                )

            (dict as NSDictionary).writeToFile(settingsFile, true)
            settingsFlow.value = settings
        }.onFailure { it.printStackTrace() }
    }

    override fun getSettingsFlow(): Flow<Settings> = settingsFlow

    override suspend fun getSettings(): Settings = settingsFlow.value
}
