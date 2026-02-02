package org.darchacheron.pantrypal.utils

import java.io.File
import kotlin.io.path.Path

/**
 * Utility object to handle desktop-specific application paths
 */
object DesktopPaths {
    private const val APP_NAME = "PantryPal"

    /**
     * Gets the OS-specific application data directory
     * Windows: %APPDATA%\PantryPal
     * macOS: ~/Library/Application Support/PantryPal
     * Linux: ~/.config/PantryPal
     */
    fun getAppDataDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")

        val appDir =
            when {
                os.contains("win") -> Path(System.getenv("APPDATA"), APP_NAME)
                os.contains("mac") -> Path(userHome, "Library/Application Support", APP_NAME)
                os.contains("nix") || os.contains("nux") || os.contains("aix") ->
                    Path(
                        userHome,
                        ".local/share/",
                        APP_NAME
                    )

                else -> Path(userHome, ".config", APP_NAME) // Linux and others
            }

        return File(appDir.toString()).apply { mkdirs() }
    }
}