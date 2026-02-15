package org.darchacheron.pantrypal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.darchacheron.pantrypal.settings.SettingsViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object Play : NavRoute
    @Serializable
    data object Settings : NavRoute
}

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NavRoute.Play::class, NavRoute.Play.serializer())
            subclass(NavRoute.Settings::class, NavRoute.Settings.serializer())
        }
    }
}

@Composable
@Preview
fun App(
    settingsViewModel: SettingsViewModel = koinInject()
) {
    KoinApplication(application = {}) {
        val settingsUiState = settingsViewModel.settingsFlow.collectAsState()

        val currentSettings = settingsUiState.value
        val settings = if (currentSettings is UiState.Success<*>) {
            currentSettings.data as Settings
        } else {
            Settings()
        }

        AppTheme(themeMode = settings.themeMode) {
            val backStack = rememberNavBackStack(org.darchacheron.gofirst.navConfig, org.darchacheron.gofirst.NavRoute.Play)

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { key ->
                    when (key) {
                        org.darchacheron.gofirst.NavRoute.Play -> NavEntry(key) {
                            PlayScreen(
                                onSettingsClick = { backStack.add(org.darchacheron.gofirst.NavRoute.Settings) }
                            )
                        }

                        org.darchacheron.gofirst.NavRoute.Settings -> NavEntry(key) {
                            SettingsView(
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeAt(backStack.size - 1)
                                    }
                                }
                            )
                        }

                        else -> NavEntry(key) { Text("Unknown route") }
                    }
                },
                transitionSpec = {
                    slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
                },
                popTransitionSpec = {
                    EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
                },
                predictivePopTransitionSpec = {
                    EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })
                }
            )
        }
    }
}