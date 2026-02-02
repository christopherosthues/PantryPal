package org.darchacheron.pantrypal.settings

import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.theme_dark
import pantrypal.composeapp.generated.resources.theme_light
import pantrypal.composeapp.generated.resources.theme_system

/**
 * Enum for theme selection.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    fun toStringResource(): StringResource =
        when (this) {
            LIGHT -> Res.string.theme_light
            DARK -> Res.string.theme_dark
            SYSTEM -> Res.string.theme_system
        }
}