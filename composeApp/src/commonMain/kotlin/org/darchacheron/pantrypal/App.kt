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
import org.darchacheron.pantrypal.food.CameraView
import org.darchacheron.pantrypal.food.FoodDetailView
import org.darchacheron.pantrypal.food.FoodListView
import org.darchacheron.pantrypal.food.SimpleCameraView
import org.darchacheron.pantrypal.settings.Settings
import org.darchacheron.pantrypal.settings.SettingsView
import org.darchacheron.pantrypal.settings.SettingsViewModel
import org.darchacheron.pantrypal.ui.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object FoodList : NavRoute

    @Serializable
    data class FoodDetail(val foodId: String? = null) : NavRoute

    @Serializable
    data class Camera(val foodId: String) : NavRoute

    @Serializable
    data class SimpleCamera(val foodId: String) : NavRoute

    @Serializable
    data object Settings : NavRoute
}

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NavRoute.FoodList::class, NavRoute.FoodList.serializer())
            subclass(NavRoute.FoodDetail::class, NavRoute.FoodDetail.serializer())
            subclass(NavRoute.Camera::class, NavRoute.Camera.serializer())
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
        val settings = if (currentSettings.hasData) {
            currentSettings.data!!
        } else {
            Settings()
        }

        AppTheme(themeMode = settings.themeMode) {
            val backStack = rememberNavBackStack(navConfig, NavRoute.FoodList)

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { key ->
                    when (key) {
                        NavRoute.FoodList -> NavEntry(key) {
                            FoodListView(
                                onNavigateToDetail = { foodId ->
                                    backStack.add(NavRoute.FoodDetail(foodId))
                                },
                                onSettingsClick = {
                                    backStack.add(NavRoute.Settings)
                                }
                            )
                        }

                        is NavRoute.FoodDetail -> NavEntry(key) {
                            FoodDetailView(
                                key.foodId,
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeAt(backStack.size - 1)
                                    }
                                },
                                onOpenCamera = { foodId ->
                                    backStack.add(NavRoute.SimpleCamera(foodId))
                                }
                            )
                        }

                        is NavRoute.Camera -> NavEntry(key) {
                            CameraView(
                                foodId = key.foodId,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }

                        is NavRoute.SimpleCamera -> NavEntry(key) {
                            SimpleCameraView(
                                foodId = key.foodId,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }

                        NavRoute.Settings -> NavEntry(key) {
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