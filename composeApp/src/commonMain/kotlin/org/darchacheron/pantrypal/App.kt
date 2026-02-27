package org.darchacheron.pantrypal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.settings.Settings
import org.darchacheron.pantrypal.settings.SettingsViewModel
import org.darchacheron.pantrypal.ui.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.dsl.koinConfiguration

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App(
    settingsViewModel: SettingsViewModel = koinInject(),
    navigator: Navigator = koinInject()
) {
    KoinApplication(configuration = koinConfiguration {  }) {
        val settingsUiState = settingsViewModel.settingsFlow.collectAsState()

        val currentSettings = settingsUiState.value
        val settings = if (currentSettings.hasData) {
            currentSettings.data!!
        } else {
            Settings()
        }

        AppTheme(themeMode = settings.themeMode) {
            navigator.Initialize()

            val backstack = navigator.backStack!!
            val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

            NavDisplay(
                backStack = backstack,
                onBack = { navigator.goBack() },
                sceneStrategy = listDetailStrategy,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = koinEntryProvider(),
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